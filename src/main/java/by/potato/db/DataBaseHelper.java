package by.potato.db;

import by.potato.Enum.TypeOfCurrency;
import by.potato.holder.City;
import by.potato.holder.Currency;
import by.potato.holder.Day;
import by.potato.holder.Department;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.maps.model.LatLng;
import oracle.ucp.UniversalConnectionPoolAdapter;
import oracle.ucp.UniversalConnectionPoolException;
import oracle.ucp.admin.UniversalConnectionPoolManager;
import oracle.ucp.admin.UniversalConnectionPoolManagerImpl;
import oracle.ucp.jdbc.PoolDataSource;
import oracle.ucp.jdbc.PoolDataSourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.*;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.*;

import static by.potato.helper.PropCheck.ORA_DB_LOGIN;
import static by.potato.helper.PropCheck.ORA_DB_PASS;

public class DataBaseHelper {

    private String poolName;
    private PoolDataSource pds;
    private ScheduledExecutorService sc;
    private UniversalConnectionPoolManager mgr;

    private Map<String, Integer> citiesID;
    private Set<String> cities;

    private final String ORA_DB_URL = "jdbc:mysql://localhost/BestCourses?characterEncoding=UTF-8";

    private static final ObjectMapper mapper = new ObjectMapper();

    private static Logger logger = LogManager.getLogger("DataBaseHelper");

    public DataBaseHelper() {

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.WRITE_ENUMS_USING_INDEX, true);


        this.pds = PoolDataSourceFactory.getPoolDataSource();
        this.sc = Executors.newScheduledThreadPool(1);
        this.poolName = new String("MariaDB");

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

    //обновить список городов
    public void updateCities(List<City> elements) {

        String sql = "INSERT INTO Cities VALUES (null,?)";

        Set<String> currentCity = this.citiesID.keySet();

        Set<String> uniqueCity = new HashSet<>();

        for (City city : elements) {
            uniqueCity.add(city.getRusName());
        }

        uniqueCity.removeAll(currentCity);

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {

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

    //список всех адресов отделений в определённом городе
    public List<String> getAddressFromCity(String city) {
        List<String> elements = new ArrayList<>();

        String sql = "select d.address from Departments as d, Cities as c where c.id = d.id_cities and c.name = ?";

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {

            preparedStatement.setString(1, city);


            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                elements.add(resultSet.getString(1));
            }

            logger.info(String.format("Read address departments %d for city %s", elements.size(), city));

            preparedStatement.close();
            resultSet.close();

        } catch (SQLException e) {
            logger.error("getAddressFromCity error: " + e.getMessage() + e.getCause());
        }

        return elements;
    }

    //обновить время работы отделений
    public void updateWorkTime(Map<String, List<Day>> elements) {

        String sql = "UPDATE Departments SET workTimes = ? WHERE link_work_time = ?";

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {


            for(Map.Entry<String, List<Day>>  elem : elements.entrySet()) {


                String jsonInString = this.mapper.writeValueAsString(elem.getValue());

                preparedStatement.setString(1, jsonInString);
                preparedStatement.setString(2, elem.getKey());

                preparedStatement.addBatch();
            }

            preparedStatement.executeBatch();
            preparedStatement.close();

            logger.info("Update time of work succesfully");
        } catch (SQLException e) {
            logger.error("UpdateDepartments error: " + e.getMessage() + e.getCause());
        } catch (JsonProcessingException e) {
            logger.error("POJO to JSON fail " + e.getMessage() + e.getCause());
        }

    }

    //обновить или добавить новое отделение
    public void updateDepartments(Map<String, List<Department>> listBank, String nameOfCity) {

        String sql = "INSERT INTO Departments " +
                "(address,bank_name,id_cities,doll_buy,doll_sell,doll_multiplier,euro_buy,euro_sell,euro_multiplier,rub_buy,rub_sell,rub_multiplier,lat,lng,workTimes,link_work_time,phone)" +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)  " +
                "ON DUPLICATE KEY UPDATE " +
                "doll_buy = ?," +
                "doll_sell = ?," +
                "doll_multiplier = ?," +
                "euro_buy = ?," +
                "euro_sell = ?," +
                "euro_multiplier = ?," +
                "rub_buy = ?," +
                "rub_sell = ?," +
                "rub_multiplier = ?";



        int idCity = this.citiesID.get(nameOfCity);

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {

            for (Map.Entry<String, List<Department>> entry : listBank.entrySet()) {

                for (Department department : entry.getValue()) {

                    String jsonInString = this.mapper.writeValueAsString(department);

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
                    preparedStatement.setDouble(13, department.getLatlng().lat);
                    preparedStatement.setDouble(14, department.getLatlng().lng);
                    preparedStatement.setString(15, jsonInString);
                    preparedStatement.setString(16, department.getLinkToTimes());
                    preparedStatement.setString(17, department.getTel());

                    //section ON DUPLICATE
                    preparedStatement.setDouble(18, doll.getValueBuy());
                    preparedStatement.setDouble(19, doll.getValueSell());
                    preparedStatement.setDouble(20, doll.getMultiplier());
                    preparedStatement.setDouble(21, euro.getValueBuy());
                    preparedStatement.setDouble(22, euro.getValueSell());
                    preparedStatement.setDouble(23, euro.getMultiplier());
                    preparedStatement.setDouble(24, rub.getValueBuy());
                    preparedStatement.setDouble(25, rub.getValueSell());
                    preparedStatement.setDouble(26, rub.getMultiplier());
//                    try {
//
//                        String url = "jdbc:mysql://localhost/test";
//                        try {
//                            Class.forName ("com.mysql.jdbc.Driver").newInstance ();
//                        } catch (InstantiationException e) {
//                            e.printStackTrace();
//                        } catch (IllegalAccessException e) {
//                            e.printStackTrace();
//                        } catch (ClassNotFoundException e) {
//                            e.printStackTrace();
//                        }
//                        Connection tt = DriverManager.getConnection ("jdbc:mysql://localhost/BestCourses?characterEncoding=UTF-8", "root", "lipwUmR");
//
//                        //Connection tt = DriverManager.getConnection("jdbc:mysql://127.0.0.1/BestCourses?characterEncoding=UTF-8?user=root&password=lipwUmR");
//
//                        String test = "INSERT INTO TEST " +
//                                "(address,bank_name,id_cities,doll_buy,doll_sell,doll_multiplier,euro_buy,euro_sell,euro_multiplier,rub_buy,rub_sell,rub_multiplier,lat,lng,workTimes)" +
//                                "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)  " +
//                                "ON DUPLICATE KEY UPDATE " +
//                                "doll_buy = ?," +
//                                "doll_sell = ?," +
//                                "doll_multiplier = ?," +
//                                "euro_buy = ?," +
//                                "euro_sell = ?," +
//                                "euro_multiplier = ?," +
//                                "rub_buy = ?," +
//                                "rub_sell = ?," +
//                                "rub_multiplier = ?," +
//                                "workTimes = ?";
//
//                        PreparedStatement prep = tt.prepareStatement(test);
//
//
//                        prep.setString(1, department.getAddress());
//                        prep.setString(2, entry.getKey());
//                        prep.setInt(3, idCity);
//                        prep.setDouble(4, doll.getValueBuy());
//                        prep.setDouble(5, doll.getValueSell());
//                        prep.setDouble(6, doll.getMultiplier());
//                        prep.setDouble(7, euro.getValueBuy());
//                        prep.setDouble(8, euro.getValueSell());
//                        prep.setDouble(9, euro.getMultiplier());
//                        prep.setDouble(10, rub.getValueBuy());
//                        prep.setDouble(11, rub.getValueSell());
//                        prep.setDouble(12, rub.getMultiplier());
//                        prep.setDouble(13, department.getLatlng().lat);
//                        prep.setDouble(14, department.getLatlng().lng);
//                        prep.setString(15, jsonInString);
//                        //section ON DUPLICATE
//                        prep.setDouble(16, doll.getValueBuy());
//                        prep.setDouble(17, doll.getValueSell());
//                        prep.setDouble(18, doll.getMultiplier());
//                        prep.setDouble(19, euro.getValueBuy());
//                        prep.setDouble(20, euro.getValueSell());
//                        prep.setDouble(21, euro.getMultiplier());
//                        prep.setDouble(22, rub.getValueBuy());
//                        prep.setDouble(23, rub.getValueSell());
//                        prep.setDouble(24, rub.getMultiplier());
//                        prep.setString(25, jsonInString);
//
//                        System.out.println(prep);
//
//
//
//                    } catch (SQLException e) {
//                        e.printStackTrace();
//                    }
                    preparedStatement.addBatch();
                }
            }

            preparedStatement.executeBatch();
            preparedStatement.close();

            logger.info("Update courses succesfully for " + nameOfCity);
        } catch (SQLException e) {
            logger.error("UpdateDepartments error: " + e.getMessage() + e.getCause());
        } catch (JsonProcessingException e) {
            logger.error("POJO to JSON fail " + e.getMessage() + e.getCause());
        }
    }

    //TODO isWork
    public List<Department> geoDepartment(Optional<LatLng> place, int count) {


        LocalDateTime localDateTime = LocalDateTime.now();
        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
        LocalTime localTime = localDateTime.toLocalTime();

        List<Department> result = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); CallableStatement callableStatement = conn.prepareCall("{call geoDepartment(?,?)}");) {

            callableStatement.setDouble(1, place.get().lat);
            callableStatement.setDouble(2, place.get().lng);

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

                String cityName = resultSet.getString(13);
                Double dist = resultSet.getDouble(14);

                Map<DayOfWeek,Day> workTime = mapper.readValue(worksTimeStr,
                        new TypeReference<Map<DayOfWeek,Day>>() {
                        });

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
                                .build();


                //TODO
                if (department.isWork(dayOfWeek, localTime).isWork()) {
                    result.add(department);

                    if (result.size() == count) {
                        break;
                    }
                }

            }

            logger.info("Read result succesfully = " + result.size());

        } catch (SQLException e) {
            logger.error("Read result error: " + e.getMessage() + e.getCause());
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    //dist in distantion precision is km
    //TODO isWork
    public List<Department> geoDepartmentDist(Optional<LatLng> place, double distantion) {

        LocalDateTime localDateTime = LocalDateTime.now();
        DayOfWeek dayOfWeek = localDateTime.getDayOfWeek();
        LocalTime localTime = localDateTime.toLocalTime();

        List<Department> result = new ArrayList<>();

        try (Connection conn = this.pds.getConnection(); CallableStatement callableStatement = conn.prepareCall("{call geoDepartmentDist(?,?,?)}");) {

            callableStatement.setDouble(1, place.get().lat);
            callableStatement.setDouble(2, place.get().lng);
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

                Map<DayOfWeek,Day> workTime = mapper.readValue(worksTimeStr,
                        new TypeReference<Map<DayOfWeek,Day>>() {
                        });

                String cityName = resultSet.getString(13);
                Double dist = resultSet.getDouble(14);

                Department department = new Department.Builder()
                        .setBankName(bankName)
                        .setAddress(address)
                        .setEur(euro)
                        .setRub(rub)
                        .setUsd(usd)
                        .setCityName(cityName)
                        .setDist(dist)
                        .setWorksTime(workTime)
                        .build();

                if (department.isWork(dayOfWeek, localTime).isWork()) {//TODO
                    result.add(department);
                }
            }

            logger.info("Read result succesfully = " + result.size());

        } catch (SQLException e) {
            logger.error("Read result error: " + e.getMessage() + e.getCause());
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (JsonMappingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    //Список обменных пунктов
    public List<Department> getCoursesByCity(String city) {


        List<Department> result = new ArrayList<>();

        String sql = "SELECT dep.bank_name, " +
                "dep.address," +
                "dep.euro_buy," +
                "dep.euro_sell," +
                "dep.euro_multiplier," +
                "dep.rub_buy," +
                "dep.rub_sell," +
                "dep.rub_multiplier," +
                "dep.doll_buy," +
                "dep.doll_sell," +
                "dep.doll_multiplier" +
                " FROM Departments as dep, Cities as c  WHERE c.name = ? and c.id = dep.id_cities";

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {

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


                Department department =
                        new Department.Builder()
                                .setBankName(bankName)
                                .setAddress(address)
                                .setEur(euro)
                                .setRub(rub)
                                .setUsd(usd)
                                .setCityName(city)
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

    //список банков в городе
    public List<String> getBanksByCity(String city) {

        List<String> result = new ArrayList<>();

        String sql = "SELECT DISTINCT bank_name " +
                "FROM Departments as dep, Cities as c  " +
                "WHERE c.name = ? and c.id = dep.id_cities " +
                "ORDER BY bank_name ASC";

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {

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
    public List<Department> getDepartmentByBankAndCity(String city,String bankName) {
        List<Department> result = new ArrayList<>();

        String sql = "SELECT " +
                "dep.address," +
                "dep.euro_buy," +
                "dep.euro_sell," +
                "dep.euro_multiplier," +
                "dep.rub_buy," +
                "dep.rub_sell," +
                "dep.rub_multiplier," +
                "dep.doll_buy," +
                "dep.doll_sell," +
                "dep.doll_multiplier" +
                " FROM Departments as dep, Cities as c  WHERE c.name = ? and c.id = dep.id_cities and bank_name = ?";

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {

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

                Department department =
                        new Department.Builder()
                                .setBankName(bankName)
                                .setAddress(address)
                                .setEur(euro)
                                .setRub(rub)
                                .setUsd(usd)
                                .setCityName(city)
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

    public void insertQuestion(Long charId, String message) {
        String sql = "INSERT INTO Questions (id, chart_id, message) VALUES (NULL,?,?)";

        try (Connection conn = this.pds.getConnection(); PreparedStatement preparedStatement = conn.prepareStatement(sql);) {

            preparedStatement.setLong(1, charId);
            preparedStatement.setString(2, message);

            preparedStatement.addBatch();

            preparedStatement.executeBatch();
            preparedStatement.close();

            logger.info("Insert question succesfully");
        } catch (SQLException e) {
            logger.error("UpdateDepartments error: " + e.getMessage() + e.getCause());
        }

    }

    //получить список городов
    private void getIDCities() {

        this.citiesID = new ConcurrentHashMap<>();

        String sql = "select * from Cities";

        try (Connection conn = this.pds.getConnection(); Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

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

    public Set<String> getCities() {
        return this.cities;
    }

    //получить ссылки о подробной информации об отделении
    public List<String> getLinkWorkTime() {
        List<String> list = new ArrayList<>();

        String sql = "select link_work_time from Departments";

        try (Connection conn = this.pds.getConnection(); Statement statement = conn.createStatement();) {

            ResultSet resultSet = statement.executeQuery(sql);

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

    private static class LazyDataBaseHelper {
        public static DataBaseHelper helper = new DataBaseHelper();
    }

    public static DataBaseHelper getInstance() {
        return LazyDataBaseHelper.helper;
    }
}










































