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

    private Instant lastUpdate;


    private List<City> cities = new ArrayList<>();

    public UpdateCourses() {
        this.getNameOfCities();
        this.lastUpdate = Instant.now();

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

    //get Name bank with list departments
    private void getInfoCityBanks(City city) {

        Map<String, List<Department>> listBank = new HashMap<>();

        List<String> currentAddress = DataBaseHelper.getInstance().getAddressFromCity(city.getRusName());

        try {

            //     System.out.println("link " + this.templateCity + city.getEngName());

            Document doc = Jsoup.connect(templateCity + city.getEngName()).timeout(180000).get();

            Element body = doc.getElementsByTag("body").first();
            Elements nameOfBanks = body.getElementsByAttributeValueContaining("class", "table-acc_link acc-link_");

            nameOfBanks.forEach(m -> {

                Element infoBank = m.getElementsByTag("a").first();

                String nameOfBank = infoBank.text();

                listBank.put(nameOfBank, new ArrayList<>());

                String filterForDeparment = "table-acc acc_" + m.attr("data-key");

                Element bank = body.getElementsByAttributeValueContaining("class", filterForDeparment).first();
                Elements depList = bank.getElementsByClass("currency_row_1");


                depList.forEach(j -> listBank.get(nameOfBank).add(getDepartment(j, city.getRusName(), currentAddress)));
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

        for (Element k : listCurrency) {

            try {

                Element element = k.getElementsByTag("span").first();
                if (element == null) {
                    continue;
                }

                String value = element.text();

                Element elem = k.getElementsByTag("i").first();

                if (elem == null) {
                    continue;
                }

                String typeOper = elem.attr("class");
                String typeCurr = elem.attr("data-c");
                String multiplier = elem.attr("data-cf");

                TypeOfCurrency type = TypeOfCurrency.valueOf(typeCurr.toUpperCase());

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

        dep.setAddress(depaptment.getElementsByClass("address").first().text());
        dep.setNameOfDepartment(nameDepartment);
        dep.setTel(depaptment.getElementsByClass("tel").first().text());
        dep.setCurrencies(new ArrayList<>(infoCurrency.values()));

        dep.setLinkToTimes(linkForMoreInformation);

        if (!currentAddress.contains(dep.getAddress())) {

            String address = dep.getAddress();

            if (dep.getAddress().contains(nameOfCity)) {
                dep.setLatlng(Geocoding.getCoordFromAddress(dep.getAddress()).get());
            } else {
                dep.setLatlng(Geocoding.getCoordFromAddress(String.format("г. %s %s", nameOfCity, address)).get());
            }

            dep.setLatlng(Geocoding.getCoordFromAddress(dep.getAddress()).get());
        } else {
            dep.setLatlng(new LatLng());
        }

        return dep;
    }

    //Преобразовать часы работы из строк в объекты и обновить в БД
//   private void test() {
//
//        List<Triple<String, List<Day>, String>> result = new ArrayList<>();
//
//        List<String> linkToWorkTime = DataBaseHelper.getInstance().getLinkWorkTime();
//
//
//
//        ExecutorService es = Executors.newFixedThreadPool(10);
//
//        for (String link : linkToWorkTime) {
//            es.submit(() -> {
//
//                //получить список строк с временем работы
//                List<String> workTimes = getWorkingTime(link);
//
//
//                List<Day> worksTime = new ArrayList<>();
//
//                for (String elem : workTimes) {
//                    WorkOfWeek.parse(elem, worksTime);
//                }
//
//                if(worksTime.size() != 0) {
//                    result.add(Triple.of(link,worksTime, String.join("\n", workTimes)));
//                }
//            });
//        }
//
//        int before;
//        int after;
//        while (linkToWorkTime.size() != result.size() )
//        try {
//            before = result.size();
//            Thread.sleep(10000);
//            System.err.println( (result.size() * 100) / linkToWorkTime.size() + "%");
//            //System.err.println(result.size());
//            after = result.size();
//
//            if(before == after) {
//                break;
//            }
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
//
//
//
//        DataBaseHelper.getInstance().updateWorkTime(result);
//    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        ExecutorService es = Executors.newFixedThreadPool(9);

        for (City city : cities) {
            es.submit(() -> getInfoCityBanks(city));
        }

        es.shutdown();
    }
}
