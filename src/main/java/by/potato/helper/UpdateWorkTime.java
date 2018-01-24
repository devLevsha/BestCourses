package by.potato.helper;

import by.potato.db.DataBaseHelper;
import by.potato.holder.Day;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.ArrayList;
import java.util.List;

public class UpdateWorkTime implements Job {

    private static final Logger logger = LogManager.getLogger(UpdateWorkTime.class.getSimpleName());

    private final String templateSite = "https://myfin.by";

    //Преобразовать часы работы из строк в объекты и обновить в БД
    @Override
    public void execute(JobExecutionContext jobExecutionContext) {

        List<Triple<String, List<Day>, String>> result = new ArrayList<>();

        List<String> linkToWorkTime = DataBaseHelper.getInstance().getLinkWorkTime();

        for (String link : linkToWorkTime) {
            //получить список строк с временем работы
            List<String> workTimes = getWorkingTime(link);


            List<Day> worksTime = new ArrayList<>();


            for (String elem : workTimes) {
                WorkOfWeek.parse(elem, worksTime);
            }

            if (worksTime.size() != 0) {//защита от того что сайт недоступен и т.д. чтобы не перетереть время
                result.add(Triple.of(link, worksTime, String.join("\n", workTimes)));
            }
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

        } catch (Exception e) {
            logger.error(String.format("Bad link %s, error message = %s, cause = %s", link, e.getMessage(), e.getCause()));
            e.printStackTrace();
        }

        return result;
    }
}
