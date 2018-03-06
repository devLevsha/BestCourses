package by.potato.db;

import by.potato.Enum.TypeOfCurrency;
import by.potato.holder.*;
import by.potato.holder.Currency;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.maps.model.LatLng;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

import static by.potato.helper.PropCheck.*;

public class DataBaseHelper {

    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Logger logger = LogManager.getLogger(DataBaseHelper.class.getSimpleName());
    private String poolName;
    private PoolDataSource pds;
    private ScheduledExecutorService sc;
    private UniversalConnectionPoolManager mgr;
    private Map<String, Integer> citiesID;
    private Set<String> cities;

    private DataBaseHelper() {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);


        this.pds = PoolDataSourceFactory.getPoolDataSource();
        this.sc = Executors.newScheduledThreadPool(1);
        this.poolName = "MariaDB";

        try {
            this.mgr = UniversalConnectionPoolManagerImpl.getUniversalConnectionPoolManager();
            logger.info("Manager created successfully!");
        } catch (UniversalConnectionPoolException e1) {
            logger.error("Can't create manager! Error: %s", e1.getMessage(), e1);
        }

        try {
            this.pds.setConnectionPoolName(this.poolName);
            this.pds.setConnectionFactoryClassName("com.mysql.jdbc.Driver");
            this.pds.setURL(ORA_DB_URL);
            this.pds.setUser(ORA_DB_LOGIN);
            this.pds.setPassword(ORA_DB_PASS);
            this.pds.setInitialPoolSize(5);
            this.pds.setMaxConnectionReuseCount(150);
            this.pds.setInactiveConnectionTimeout(60);

            Properties connProps = new Properties();
            connProps.put("characterEncoding", "UTF-8");

            this.pds.setConnectionProperties(connProps);

            try {
                this.mgr.createConnectionPool((UniversalConnectionPoolAdapter) this.pds);
                this.mgr.startConnectionPool(this.poolName);
            } catch (UniversalConnectionPoolException e) {
                logger.error("Can't add connection pool to manager! Error: %s", e.getMessage(), e);
            }

            logger.info("Connection pool created successfully!");
            this.sc.scheduleAtFixedRate(new HealthCheckerPool(this.pds.getStatistics(), this.poolName), 0, 30,
                    TimeUnit.SECONDS);
        } catch (SQLException e) {
            logger.error("Can't init connection pool! Error: %s", e.getMessage(), e);
        }

        getIDCities();

    }

    public static DataBaseHelper getInstance() {
        return LazyDataBaseHelper.helper;
    }

    //обновить список городов
    public void updateCities(List<City> elements) {
        Set<String> currentCity = this.citiesID.keySet();
        Set<String> uniqueCity = new HashSet<>();

        for (City city : elements) {
            uniqueCity.add(city.getRusName());
        }

        uniqueCity.removeAll(currentCity);

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.UpdateCities)) {

            for (String elem : uniqueCity) {

                preparedStatement.setString(1, elem);
                preparedStatement.addBatch();
            }

            int[] affectedRecords = preparedStatement.executeBatch();

            logger.info("New cities in table " + Arrays.stream(affectedRecords).sum());

        } catch (SQLException e) {
            logger.error("updateCities error: " + e.getMessage() + e.getCause());
            e.printStackTrace();
        }
    }

    public void destroyPool() {

        try {
            this.sc.shutdownNow();
            logger.info("Health checker of connection pool closed successfully!");
            this.mgr.destroyConnectionPool(this.poolName);
            this.mgr = null;
            logger.info("Connection pool destroyed successfully!");
        } catch (UniversalConnectionPoolException e) {
            logger.error("Can't destroyed connection pool. Error: %s", e.getMessage(), e);
        }

    }

    //список всех адресов отделений в определённом городе
    public List<String> getAddressFromCity(String city) {
        List<String> elements = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.GetAddressFromCity)) {

            preparedStatement.setString(1, city);


            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                elements.add(resultSet.getString(1));
            }

            //      logger.info(String.format("Read address departments %d for city %s", elements.size(), city));

            preparedStatement.close();
            resultSet.close();


        } catch (SQLException e) {
            logger.error("getAddressFromCity error: " + e.getMessage() + e.getCause());
        }

        return elements;
    }

    //обновить время работы отделений
    public void updateWorkTime(List<Triple<String, List<Day>, String>> elements) {
        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.UpdateWorkTime)) {

            for (Triple<String, List<Day>, String> elem : elements) {
                String jsonInString = mapper.writeValueAsString(elem.getMiddle());

                preparedStatement.setString(1, jsonInString);
                preparedStatement.setString(2, elem.getRight());
                preparedStatement.setString(3, elem.getLeft());
                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
            preparedStatement.close();

            logger.info("Update time of work succesfully");
        } catch (SQLException e) {
            logger.error("updateWorkTime error: " + e.getMessage() + e.getCause());
        } catch (JsonProcessingException e) {
            logger.error("POJO to JSON fail " + e.getMessage() + e.getCause());
        }

    }

    //обновить или добавить новое отделение
    public void updateDepartments(Map<String, List<Department>> listBank, String nameOfCity, Instant instans) {
        int idCity = this.citiesID.get(nameOfCity);

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.UpdateDepartments)) {

            for (Map.Entry<String, List<Department>> entry : listBank.entrySet()) {

                for (Department department : entry.getValue()) {

                    String jsonInString = mapper.writeValueAsString(department);

                    Currency euro = new Currency();
                    Currency doll = new Currency();
                    Currency rub = new Currency();

                    for (Currency n : department.getCurrencies()) {
                        switch (n.getType()) {
                            case EUR:
                                euro = n;
                                break;
                            case RUB:
                                rub = n;
                                break;
                            case USD:
                                doll = n;
                                break;
                        }
                    }

                    //section INSERT INTO
                    preparedStatement.setString(1, department.getAddress());
                    preparedStatement.setString(2, entry.getKey());
                    preparedStatement.setInt(3, idCity);
                    preparedStatement.setDouble(4, doll.getValueBuy());
                    preparedStatement.setDouble(5, doll.getValueSell());
                    preparedStatement.setDouble(6, doll.getMultiplier());
                    preparedStatement.setDouble(7, euro.getValueBuy());
                    preparedStatement.setDouble(8, euro.getValueSell());
                    preparedStatement.setDouble(9, euro.getMultiplier());
                    preparedStatement.setDouble(10, rub.getValueBuy());
                    preparedStatement.setDouble(11, rub.getValueSell());
                    preparedStatement.setDouble(12, rub.getMultiplier());
                    preparedStatement.setDouble(13, department.getLocation().lat);
                    preparedStatement.setDouble(14, department.getLocation().lng);
                    preparedStatement.setString(15, jsonInString);
                    preparedStatement.setString(16, department.getLinkToTimes());
                    preparedStatement.setString(17, department.getTel());
                    preparedStatement.setLong(18, instans.getEpochSecond());
                    //section ON DUPLICATE
                    preparedStatement.setDouble(19, doll.getValueBuy());
                    preparedStatement.setDouble(20, doll.getValueSell());
                    preparedStatement.setDouble(21, doll.getMultiplier());
                    preparedStatement.setDouble(22, euro.getValueBuy());
                    preparedStatement.setDouble(23, euro.getValueSell());
                    preparedStatement.setDouble(24, euro.getMultiplier());
                    preparedStatement.setDouble(25, rub.getValueBuy());
                    preparedStatement.setDouble(26, rub.getValueSell());
                    preparedStatement.setDouble(27, rub.getMultiplier());
                    preparedStatement.setLong(28, instans.getEpochSecond());

                    preparedStatement.addBatch();
                }
            }

            preparedStatement.executeBatch();
            preparedStatement.close();

            logger.info("Update courses succesfully for " + nameOfCity);
        } catch (SQLException e) {
            logger.error("updateDepartments error: " + e.getMessage() + e.getCause());
        } catch (JsonProcessingException e) {
            logger.error("POJO to JSON fail " + e.getMessage() + e.getCause());
        } catch (Exception e) {
            logger.error("ERROR " + e.getMessage() + e.getCause() + e.getStackTrace().toString());
        }
    }

    public void deleteUnusedDepartment() {
        Long currentMinusTwoDays = Instant.now().minus(2, ChronoUnit.DAYS).getEpochSecond();

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.DeleteUnusedDepartment)) {

            preparedStatement.setLong(1, currentMinusTwoDays);

            preparedStatement.execute();

        } catch (SQLException e) {
            logger.error("Error deleteUnusedDepartment " + e.getMessage() + e.getCause());
        }
    }

    //Список обменных пунктов
    public List<Department> getCoursesByCity(String city) {
        List<Department> result = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.GetCoursesByCity)) {

            preparedStatement.setString(1, city);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                String bankName = resultSet.getString(1);
                String address = resultSet.getString(2);

                Currency euro = new Currency(TypeOfCurrency.EUR);
                euro.setValueBuy(resultSet.getDouble(3));
                euro.setValueSell(resultSet.getDouble(4));
                euro.setMultiplier(resultSet.getInt(5));

                Currency rub = new Currency(TypeOfCurrency.RUB);
                rub.setValueBuy(resultSet.getDouble(6));
                rub.setValueSell(resultSet.getDouble(7));
                rub.setMultiplier(resultSet.getInt(8));

                Currency usd = new Currency(TypeOfCurrency.USD);
                usd.setValueBuy(resultSet.getDouble(9));
                usd.setValueSell(resultSet.getDouble(10));
                usd.setMultiplier(resultSet.getInt(11));

                String tel = resultSet.getString(12);
                String workTimeOriginal = resultSet.getString(13);

                LatLng latLng = new LatLng();
                latLng.lat = resultSet.getDouble(14);
                latLng.lng = resultSet.getDouble(15);


                Department department =
                        new Department.Builder()
                                .setBankName(bankName)
                                .setAddress(address)
                                .setEur(euro)
                                .setRub(rub)
                                .setUsd(usd)
                                .setCityName(city)
                                .setTel(tel)
                                .setWorkTimeOriginal(workTimeOriginal)
                                .setLocation(latLng)
                                .build();

                result.add(department);
            }

            logger.info(String.format("Read courses for city %s", city));

            preparedStatement.close();
            resultSet.close();

        } catch (SQLException e) {
            logger.error("getCoursesByCity error: " + e.getMessage() + e.getCause());
        }

        return result;
    }

    public List<Department> geoDepartment(LatLng location, LocalDateTime localDateTime) {

        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
        LocalTime localTime = localDateTime.toLocalTime();

        List<Department> result = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); CallableStatement callableStatement = conn.prepareCall("{call geoDepartment(?,?)}")) {

            callableStatement.setDouble(1, location.lat);
            callableStatement.setDouble(2, location.lng);

            ResultSet resultSet = callableStatement.executeQuery();

            while (resultSet.next()) {

                String bankName = resultSet.getString(1);
                String address = resultSet.getString(2);

                Currency euro = new Currency(TypeOfCurrency.EUR);
                euro.setValueBuy(resultSet.getDouble(3));
                euro.setValueSell(resultSet.getDouble(4));
                euro.setMultiplier(resultSet.getInt(5));

                Currency rub = new Currency(TypeOfCurrency.RUB);
                rub.setValueBuy(resultSet.getDouble(6));
                rub.setValueSell(resultSet.getDouble(7));
                rub.setMultiplier(resultSet.getInt(8));

                Currency usd = new Currency(TypeOfCurrency.USD);
                usd.setValueBuy(resultSet.getDouble(9));
                usd.setValueSell(resultSet.getDouble(10));
                usd.setMultiplier(resultSet.getInt(11));

                String worksTimeStr = resultSet.getString(12);

                LatLng latLng = new LatLng();
                latLng.lat = resultSet.getDouble(13);
                latLng.lng = resultSet.getDouble(14);

                String cityName = resultSet.getString(15);
                Double dist = resultSet.getDouble(16);

                List<Day> workTime = new ArrayList<>();
                if (worksTimeStr != null) {
                    workTime = mapper.readValue(worksTimeStr,
                            new TypeReference<List<Day>>() {
                            });
                }

                Department department =
                        new Department.Builder()
                                .setBankName(bankName)
                                .setAddress(address)
                                .setEur(euro)
                                .setRub(rub)
                                .setUsd(usd)
                                .setCityName(cityName)
                                .setDist(dist)
                                .setWorksTime(workTime)
                                .setLocation(latLng)
                                .build();

                if (department.isWork(dayOfWeek, localTime)) {
                    result.add(department);
                }


            }

            logger.info("Read result succesfully = " + result.size());

        } catch (Exception e) {
            logger.error("Read result error: " + e.getMessage() + e.getCause());
        }

        return result;
    }

    //dist in distantion precision is km
    public List<Department> geoDepartmentDist(LatLng location, double distantion) {

        LocalDateTime localDateTime = LocalDateTime.now();
        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
        LocalTime localTime = localDateTime.toLocalTime();

        List<Department> result = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); CallableStatement callableStatement = conn.prepareCall("{call geoDepartmentDist(?,?,?)}")) {

            callableStatement.setDouble(1, location.lat);
            callableStatement.setDouble(2, location.lng);
            callableStatement.setDouble(3, distantion);

            ResultSet resultSet = callableStatement.executeQuery();

            while (resultSet.next()) {

                String bankName = resultSet.getString(1);
                String address = resultSet.getString(2);

                Currency euro = new Currency(TypeOfCurrency.EUR);
                euro.setValueBuy(resultSet.getDouble(3));
                euro.setValueSell(resultSet.getDouble(4));
                euro.setMultiplier(resultSet.getInt(5));

                Currency rub = new Currency(TypeOfCurrency.RUB);
                rub.setValueBuy(resultSet.getDouble(6));
                rub.setValueSell(resultSet.getDouble(7));
                rub.setMultiplier(resultSet.getInt(8));

                Currency usd = new Currency(TypeOfCurrency.USD);
                usd.setValueBuy(resultSet.getDouble(9));
                usd.setValueSell(resultSet.getDouble(10));
                usd.setMultiplier(resultSet.getInt(11));

                String worksTimeStr = resultSet.getString(12);

                List<Day> workTime = mapper.readValue(worksTimeStr,
                        new TypeReference<List<Day>>() {
                        });

                LatLng latLng = new LatLng();
                latLng.lat = resultSet.getDouble(13);
                latLng.lng = resultSet.getDouble(14);

                String cityName = resultSet.getString(15);
                Double dist = resultSet.getDouble(16);

                Department department = new Department.Builder()
                        .setBankName(bankName)
                        .setAddress(address)
                        .setEur(euro)
                        .setRub(rub)
                        .setUsd(usd)
                        .setCityName(cityName)
                        .setDist(dist)
                        .setWorksTime(workTime)
                        .setLocation(latLng)
                        .build();

                if (department.isWork(dayOfWeek, localTime)) {
                    result.add(department);
                }
            }

            logger.info("Read result geoDepartmentDist succesfully = " + result.size());

        } catch (Exception e) {
            logger.error("Read result geoDepartmentDist error: " + e.getMessage() + e.getCause());
        }

        return result;
    }

    //список банков в городе
    public List<String> getBanksByCity(String city) {
        List<String> result = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.GetBanksByCity)) {

            preparedStatement.setString(1, city);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                result.add(resultSet.getString(1));
            }

            logger.info(String.format("Read banks for city %s", city));

            preparedStatement.close();
            resultSet.close();

        } catch (SQLException e) {
            logger.error("getCoursesByCity error: " + e.getMessage() + e.getCause());
        }

        return result;
    }

    //список отделений в определённом городе
    public List<Department> getDepartmentByBankAndCity(String city, String bankName) {
        List<Department> result = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.GetDepartmentByBankAndCity)) {

            preparedStatement.setString(1, city);
            preparedStatement.setString(2, bankName);

            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {

                String address = resultSet.getString(1);

                Currency euro = new Currency(TypeOfCurrency.EUR);
                euro.setValueBuy(resultSet.getDouble(2));
                euro.setValueSell(resultSet.getDouble(3));
                euro.setMultiplier(resultSet.getInt(4));

                Currency rub = new Currency(TypeOfCurrency.RUB);
                rub.setValueBuy(resultSet.getDouble(5));
                rub.setValueSell(resultSet.getDouble(6));
                rub.setMultiplier(resultSet.getInt(7));

                Currency usd = new Currency(TypeOfCurrency.USD);
                usd.setValueBuy(resultSet.getDouble(8));
                usd.setValueSell(resultSet.getDouble(9));
                usd.setMultiplier(resultSet.getInt(10));

                String tel = resultSet.getString(11);
                String workTimeOriginal = resultSet.getString(12);

                LatLng latLng = new LatLng();
                latLng.lat = resultSet.getDouble(13);
                latLng.lng = resultSet.getDouble(14);

                Department department =
                        new Department.Builder()
                                .setBankName(bankName)
                                .setAddress(address)
                                .setEur(euro)
                                .setRub(rub)
                                .setUsd(usd)
                                .setCityName(city)
                                .setTel(tel)
                                .setWorkTimeOriginal(workTimeOriginal)
                                .setLocation(latLng)
                                .build();

                result.add(department);
            }

            logger.info(String.format("Read department for city %s", city));

            preparedStatement.close();
            resultSet.close();

        } catch (SQLException e) {
            logger.error("getDepartmentByBankAndCity error: " + e.getMessage() + e.getCause());
        }

        return result;

    }

    public void insertQuestion(Long chatId, String message) {
        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.InsertQuestion)) {

            preparedStatement.setLong(1, chatId);
            preparedStatement.setString(2, message);

            preparedStatement.addBatch();

            preparedStatement.executeBatch();
            preparedStatement.close();

            logger.info("Insert question succesfully");
        } catch (SQLException e) {
            logger.error("insertQuestion error: " + e.getMessage() + e.getCause());
        }

    }

    //получить список городов
    private void getIDCities() {
        this.citiesID = new ConcurrentHashMap<>();

        try (Connection conn = this.pds.getConnection(); Statement statement = conn.createStatement()) {

            ResultSet resultSet = statement.executeQuery(SQL.GetIDCities);

            while (resultSet.next()) {
                this.citiesID.put(resultSet.getString("name"), resultSet.getInt("id"));
            }

            logger.info("Read cities succesfully = " + this.citiesID.size());

            statement.close();
            resultSet.close();

            this.cities = new CopyOnWriteArraySet<>(this.citiesID.keySet());

        } catch (SQLException e) {
            logger.error("getIDCities error: " + e.getMessage() + e.getCause());
        }
    }

    public UserSettings getUserSettings(Long chatId) {
        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.GetUserSettings)) {

            preparedStatement.setLong(1, chatId);

            ResultSet resultSet = preparedStatement.executeQuery();

            UserSettings userSettings = new UserSettings();

            if (resultSet.next()) {

                Boolean rubSell = resultSet.getBoolean(1);
                Boolean rubBuy = resultSet.getBoolean(2);
                Boolean usdSell = resultSet.getBoolean(3);
                Boolean usdBuy = resultSet.getBoolean(4);
                Boolean eurSell = resultSet.getBoolean(5);
                Boolean eurBuy = resultSet.getBoolean(6);
                Boolean phone = resultSet.getBoolean(7);
                Boolean workTime = resultSet.getBoolean(8);

                userSettings = new UserSettings.Builder()
                        .setRubBuy(rubBuy)
                        .setRubSell(rubSell)
                        .setUsdBuy(usdBuy)
                        .setUsdSell(usdSell)
                        .setEurBuy(eurBuy)
                        .setEurSell(eurSell)
                        .setPhone(phone)
                        .setWorkTime(phone)
                        .build();

                resultSet.close();
            }

            logger.info("getUserSetting get succesfully");
            return userSettings;
        } catch (SQLException e) {
            logger.error("getUserSetting error: " + e.getMessage() + e.getCause());
            return new UserSettings();
        }
    }

    public void updateUserSettings(UserSettings userSettings, Long chatId) {
        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(SQL.UpdateUserSettings)) {

            preparedStatement.setLong(1, chatId);
            preparedStatement.setBoolean(2, userSettings.getRubSell());
            preparedStatement.setBoolean(3, userSettings.getRubBuy());
            preparedStatement.setBoolean(4, userSettings.getUsdSell());
            preparedStatement.setBoolean(5, userSettings.getUsdBuy());
            preparedStatement.setBoolean(6, userSettings.getEurSell());
            preparedStatement.setBoolean(7, userSettings.getEurBuy());
            preparedStatement.setBoolean(8, userSettings.getPhone());
            preparedStatement.setBoolean(9, userSettings.getWorkTime());

            //section INSERT INTO
            preparedStatement.setBoolean(10, userSettings.getRubSell());
            preparedStatement.setBoolean(11, userSettings.getRubBuy());
            preparedStatement.setBoolean(12, userSettings.getUsdSell());
            preparedStatement.setBoolean(13, userSettings.getUsdBuy());
            preparedStatement.setBoolean(14, userSettings.getEurSell());
            preparedStatement.setBoolean(15, userSettings.getEurBuy());
            preparedStatement.setBoolean(16, userSettings.getPhone());
            preparedStatement.setBoolean(17, userSettings.getWorkTime());


            preparedStatement.executeUpdate();
            preparedStatement.close();

            logger.info("updateUserSettings for user succesfully");
        } catch (SQLException e) {
            logger.error("updateUserSettings error: " + e.getMessage() + e.getCause());
        }
    }

    //получить ссылки о подробной информации об отделении
    public List<String> getLinkWorkTime() {
        List<String> list = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); Statement statement = conn.createStatement()) {

            ResultSet resultSet = statement.executeQuery(SQL.GetLinkWorkTime);

            while (resultSet.next()) {
                list.add(resultSet.getString("link_work_time"));
            }

            logger.info("Read link_work_time succesfully = " + list.size());

            statement.close();
            resultSet.close();

        } catch (SQLException e) {
            logger.error("getLinkWorkTime error: " + e.getMessage() + e.getCause());
        }

        return list;

    }

    public Set<String> getCities() {
        return this.cities;
    }

    private static class LazyDataBaseHelper {
        static DataBaseHelper helper = new DataBaseHelper();
    }
}










































