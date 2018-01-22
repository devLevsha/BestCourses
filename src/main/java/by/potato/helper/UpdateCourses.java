package by.potato.helper;

import by.potato.Enum.TypeOfCurrency;
import by.potato.db.DataBaseHelper;
import by.potato.holder.City;
import by.potato.holder.Currency;
import by.potato.holder.Day;
import by.potato.holder.Department;
import com.google.maps.model.LatLng;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class UpdateCourses implements Runnable {
    private static final Logger loggerLostLatLng = LogManager.getLogger(UpdateCourses.class.getSimpleName());

    private final String targetUrlforCity = "https://myfin.by/currency/vitebsk";
    private final String templateCity = "https://myfin.by/currency/";
    private final String templateSite = "https://myfin.by";

    private List<City> cities = new ArrayList<>();

    public UpdateCourses() {
        this.getNameOfCities();

        //for debug
        //cities.add(new City("Несвиж","nesvizh"));
    }

    //get name of city and update data in DB
    private void getNameOfCities() {

        try {
            Document doc = Jsoup.connect(this.targetUrlforCity).get();
            Elements city = doc.getElementsByClass("set_city");

            city.forEach(n -> {
                cities.add(new City(n.text(), n.attr("data-slug")));
            });

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

            Document doc = Jsoup.connect(this.templateCity + city.getEngName()).timeout(180000).get();

            Element body = doc.getElementsByTag("body").first();
            Elements nameOfBanks = body.getElementsByAttributeValueContaining("class", "table-acc_link acc-link_");

            nameOfBanks.forEach(m -> {

                Element infoBank = m.getElementsByTag("a").first();

                String nameOfBank = infoBank.text();

                listBank.put(nameOfBank, new ArrayList<>());

                String filterForDeparment = "table-acc acc_" + m.attr("data-key");

                Element bank = body.getElementsByAttributeValueContaining("class", filterForDeparment).first();
                Elements depList = bank.getElementsByClass("currency_row_1");


                depList.forEach(j -> {
                    listBank.get(nameOfBank).add(getDepartment(j, city.getRusName(), currentAddress));
                });
            });

        } catch (IOException e) {
            e.printStackTrace();
        }

        DataBaseHelper.getInstance().updateDepartments(listBank, city.getRusName());
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
            dep.setLatlng(Geocoding.getCoordFromAddress(dep.getAddress()).get());
        } else {
            dep.setLatlng(new LatLng());
        }

        return dep;
    }

    //Преобразовать часы работы из строк в объекты и обновить в БД
    private void updateWorkTime() {

        Map<String, List<Day>> result = new HashMap<>();

        List<String> linkToWorkTime = DataBaseHelper.getInstance().getLinkWorkTime();

        for(String link: linkToWorkTime) {

            //получить список строк с временем работы
            List<String> workTimes = getWorkingTime(link);


            List<Day> worksTime = new ArrayList<>();


            for(String elem: workTimes) {
                WorkOfWeek.parse(elem,worksTime);
            }

            if(worksTime.size() != 0) {//защита от того что сайт недоступен и т.д. чтобы не перетереть время
                result.put(link, worksTime);
            }

            result.put(link,worksTime);
        }

        DataBaseHelper.getInstance().updateWorkTime(result);
    }

    //Преобразовать часы работы из строк в объекты и обновить в БД
    private void test() {

        Map<String, List<Day>> result = new HashMap<>();

        List<String> linkToWorkTime = DataBaseHelper.getInstance().getLinkWorkTime();



        ExecutorService es = Executors.newFixedThreadPool(10);

        for (String link : linkToWorkTime) {
            es.submit(() -> {

                //получить список строк с временем работы
                List<String> workTimes = getWorkingTime(link);


                List<Day> worksTime = new ArrayList<>();

                for (String elem : workTimes) {
                    WorkOfWeek.parse(elem, worksTime);
                }

                if(worksTime.size() != 0) {
                    result.put(link, worksTime);
                }
            });
        }

        int before;
        int after;
        while (linkToWorkTime.size() != result.size() )
        try {
            before = result.size();
            Thread.sleep(10000);
            System.err.println( (result.size() * 100) / linkToWorkTime.size() + "%");
            //System.err.println(result.size());
            after = result.size();

            if(before == after) {
                break;
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }



        DataBaseHelper.getInstance().updateWorkTime(result);
    }

    //получить часы работы отделения как набор строк
    private List<String> getWorkingTime(String link) {

        List<String> result = new ArrayList<>();

        Document doc;
        try {

            doc = Jsoup.connect(this.templateSite + link).get();
            Element aboutDepart = doc.getElementsByClass("content_i department").first();
            Element table = aboutDepart.getElementsByTag("tbody").first();
            Elements workTimes = aboutDepart.getElementsByAttributeValue("itemprop", "openingHoursSpecification");

            for (Element elem : workTimes) {
                result.add(elem.getElementsByAttributeValue("itemprop", "name").first().text());
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("BAD link" + link);
            e.printStackTrace();
        }

        return result;
    }

    @Override
    public void run() {

        ExecutorService es = Executors.newFixedThreadPool(10);

//        es.submit( () -> {
//            this.test();
//        });

        es.submit( () -> {
            this.updateWorkTime();
        });


        for (City city : cities) {
            es.submit(() -> {
                getInfoCityBanks(city);
            });
        }

        es.shutdown();
    }
}
