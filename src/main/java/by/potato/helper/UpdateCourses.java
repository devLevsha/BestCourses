package by.potato.helper;

import by.potato.Enum.TypeOfCurrency;
import by.potato.db.DataBaseHelper;
import by.potato.holder.City;
import by.potato.holder.Currency;
import by.potato.holder.Department;
import com.google.maps.model.LatLng;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateCourses implements Job {
    private static final Logger logger = LogManager.getLogger(UpdateCourses.class.getSimpleName());

    private static final String targetUrlforCity = "https://myfin.by/currency/vitebsk";
    private static final String templateCity = "https://myfin.by/currency/";
    private static final String templateAccrdional = "?accordion=accordion-";
    private Instant lastUpdate;


    private List<City> cities = new ArrayList<>();

    public UpdateCourses() {
        this.getNameOfCities();

        //for debug
        //cities.add(new City("Несвиж","nesvizh"));
    }

    //get name of city and update data in DB
    private void getNameOfCities() {

        try {
            Document doc = Jsoup.connect(targetUrlforCity).get();
            Elements city = doc.getElementsByClass("set_city");

            city.forEach(n -> cities.add(new City(n.text(), n.attr("data-slug"))));

            DataBaseHelper.getInstance().updateCities(cities);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //получить список отделений банка для определённого города
    private void getInfoCityBanks(City city) {

        this.lastUpdate = Instant.now();

        logger.info("getInfoCityBanks for city " + city.getRusName());
        Map<String, List<Department>> listBank = new HashMap<>();

        List<String> currentAddress = DataBaseHelper.getInstance().getAddressFromCity(city.getRusName());

        try {

            Document doc = Jsoup.connect(templateCity + city.getEngName()).timeout(180000).get();

            Element body = doc.getElementsByTag("body").first();
            Elements nameOfBanks = body.getElementsByAttributeValueContaining("class", "tr-tb acc-link_");

            nameOfBanks.forEach(m -> {

                Element infoBank = m.getElementsByTag("a").first();

                String nameOfBank = infoBank.text();

                listBank.put(nameOfBank, new ArrayList<>());


                try {
                    //линк на запрос доп. данных под конкретному банку
                    String link = templateCity
                            + city.getEngName()
                            + templateAccrdional
                            + m.attr("data-bank_id");

                    Document docWithDepartment = Jsoup.connect(link).timeout(180000).get();

                    String filterForDeparment = "acc_" + m.attr("data-bank_id") + " acc-body";

                    Element bank = docWithDepartment.getElementsByAttributeValueContaining("class", filterForDeparment).first();

                    Elements depList = bank.getElementsByClass("currency_row_1");

                    depList.forEach(j -> listBank.get(nameOfBank).add(getDepartment(j, city.getRusName(), currentAddress)));

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }

        DataBaseHelper.getInstance().updateDepartments(listBank, city.getRusName(), this.lastUpdate);
    }

    //получить данные о курсе валют в определённом отделини
    private Department getDepartment(Element depaptment, String nameOfCity, List<String> currentAddress) {

        Elements listCurrency = depaptment.getElementsByTag("td");

        Map<TypeOfCurrency, Currency> infoCurrency = new ConcurrentHashMap<>();

        //<tr class=" currency_row_1">
        // <td>
        //  <div class="ttl">
        //   <a href="/bank/absolutbank/department/565-minsk-pr-nezavisimosti-95">Головное отделение ЗАО "Абсолютбанк"</a>
        //  </div>
        //  <div class="tel">
        //                +375-17-237-07-02
        //                </div>
        //  <div class="address">
        //                г. Минск, пр. Независимости, 95
        //                </div>
        //  <div class="date">
        //                22:38
        //                </div> </td>
        // <td>1.957<i class="conv-btn buy" data-c="usd" data-cf="1"></i></td>
        // <td>1.963<i class="conv-btn sell" data-c="usd" data-cf="1"></i></td>
        // <td>2.423<i class="conv-btn buy" data-c="eur" data-cf="1"></i></td>
        // <td class="best">2.435<i class="conv-btn sell" data-c="eur" data-cf="1"></i></td>
        // <td>3.455<i class="conv-btn buy" data-c="rub" data-cf="100"></i></td>
        // <td>3.48<i class="conv-btn sell" data-c="rub" data-cf="100"></i></td>
        // <td>1.233<i class="conv-btn buy" data-c="eurusd" data-cf="1"></i></td>
        // <td>1.244<i class="conv-btn sell" data-c="eurusd" data-cf="1"></i></td>
        //</tr>
        for (Element k : listCurrency) {

            try {


                if (k.getElementsByTag("i").size() == 0) {
                    continue;
                }


                String value = k.text();

                Element elem = k.getElementsByTag("i").first();

                if (elem == null) {
                    continue;
                }

                String typeOper = elem.attr("class");
                String typeCurr = elem.attr("data-c");
                String multiplier = elem.attr("data-cf");

                TypeOfCurrency type;
                try {
                    //на вход ожидаем только rub/euro/usb конверсия и т.д. не обрабатываем
                    type = TypeOfCurrency.valueOf(typeCurr.toUpperCase());

                } catch (IllegalArgumentException e) {
                    continue;
                }

                Currency cur;

                if (infoCurrency.containsKey(type)) {
                    cur = infoCurrency.get(type);
                } else {
                    cur = new Currency(type, Integer.valueOf(multiplier));
                }

                if (typeOper.endsWith("buy")) {
                    cur.setValueBuy(Double.valueOf(value));
                }

                if (typeOper.endsWith("sell")) {
                    cur.setValueSell(Double.valueOf(value));
                }

                infoCurrency.put(type, cur);

            } catch (Exception e) {
                System.out.println(nameOfCity);
                e.printStackTrace();
            }
        }

        Department dep = new Department();

        Element nameDepWithLink = depaptment.getElementsByClass("ttl").first();
        String nameDepartment = nameDepWithLink.text();

        String linkForMoreInformation = nameDepWithLink.getElementsByTag("a").first().attr("href");

        String address = depaptment.getElementsByClass("address").first().text();
        dep.setAddress(improveAddress(address, nameOfCity));

        dep.setNameOfDepartment(nameDepartment);
        dep.setTel(depaptment.getElementsByClass("tel").first().text());
        dep.setCurrencies(new ArrayList<>(infoCurrency.values()));

        dep.setLinkToTimes(linkForMoreInformation);

        if (!currentAddress.contains(dep.getAddress())) {

            //attempt maps.yandex.api
            LatLng latLng = Geocoding.getCoordFromAddressYandex(dep.getAddress()).get();

            //attempt maps.google.api
            if (latLng.lng == 0 && latLng.lat == 0) {
                latLng = Geocoding.getCoordFromAddressGoogle(dep.getAddress()).get();
            }

            dep.setLocation(latLng);
        } else {
            dep.setLocation(new LatLng());
        }

        return dep;
    }

    private String improveAddress(String address, String nameOfCity) {

        int positionNameOfCity = address.indexOf(nameOfCity);

        if (positionNameOfCity == -1) {//в адресе нет названия города

            return String.format("РБ г. %s %s", nameOfCity, address);
        } else {

            int positionBreacket = address.indexOf("(");

            if ((positionBreacket < positionNameOfCity) && (positionBreacket != -1)) {
                return String.format("РБ г. %s %s", nameOfCity, address);
            } else {
                return "РБ " + address;
            }

        }

    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        //for test count thread 1
        ExecutorService es = Executors.newFixedThreadPool(9);
        //ExecutorService es = Executors.newFixedThreadPool(1);

        for (City city : cities) {
            es.submit(() -> getInfoCityBanks(city));
        }

        es.shutdown();
    }
}
