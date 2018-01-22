package by.potato.helper;

import by.potato.Enum.Info;
import by.potato.Enum.TypeOfCurrency;
import by.potato.Pairs.MinMax;
import by.potato.holder.Department;
import com.vdurmont.emoji.EmojiParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.objects.Message;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringHelper {

    private static final Logger logger = LogManager.getLogger(StringHelper.class.getSimpleName());

    private static final String template_min_to_hour = "%d:%02d";

    private static final String templateNearDist = "Город %s\nРасстояние %.3f км\nБанк %s\nАдрес %s\nВремя %s\nRUB покупка %s\nRUB продажа %s\nUSD покупка %s\nUSD продажа %s\nEURO покупка %s\nEUR продажа %s";
    private static final String templateDepartment = "Адрес %s\nТелефон %s\nГрафик работы %s\nRUB покупка %s\nRUB продажа %s\nUSD покупка %s\nUSD продажа %s\nEURO покупка %s\nEUR продажа %s";

    private static final String templateBold = "<b>%s :thumbsup:</b>";

    private static final int STEP_FOR_INFO = 10;
    private static final int STEP_INIT_POSITION = 0;

    public static List<String> getPrintNearDepartment(List<Department> list, LocalDateTime localDateTime) {

        List<Department> result;
        try {
            result = list.subList(STEP_INIT_POSITION, STEP_FOR_INFO);
        } catch (IndexOutOfBoundsException e) {
            //если элементов меньше 10
            result = list.subList(STEP_INIT_POSITION, list.size() - 1);
        }

        return courses(result, Info.NEAR, localDateTime, false);
    }

    public static List<String> getBestCoursesByCity(List<Department> list, LocalDateTime localDateTime) {
        return courses(list, Info.INFO, localDateTime, true);
    }

    public static List<String> getPrintNearDistDepartment(List<Department> list, LocalDateTime localDateTime) {
        return courses(list, Info.NEAR, localDateTime, false);
    }


    private static List<String> courses(List<Department> list, Info info, LocalDateTime localDateTime, Boolean onlyBestCourses) {


        List<String> strings = new ArrayList<>();

        Map<TypeOfCurrency, MinMax> minMax = findMaxCourses(list);

        for (Department dep : list) {
            Message mess = new Message();

            String eurSell = bestCourses(minMax.get(TypeOfCurrency.EUR).getMinSell(), dep.getEur().getValueSell());
            String euroBuy = bestCourses(minMax.get(TypeOfCurrency.EUR).getMaxBuy(), dep.getEur().getValueBuy());
            String usdSell = bestCourses(minMax.get(TypeOfCurrency.USD).getMinSell(), dep.getUsd().getValueSell());
            String usdBuy = bestCourses(minMax.get(TypeOfCurrency.USD).getMaxBuy(), dep.getUsd().getValueBuy());
            String rubSell = bestCourses(minMax.get(TypeOfCurrency.RUB).getMinSell(), dep.getRub().getValueSell());
            String rubBuy = bestCourses(minMax.get(TypeOfCurrency.RUB).getMaxBuy(), dep.getRub().getValueBuy());

            try {
                String str = "";

                switch (info) {
                    case NEAR:
                        str = String.format(templateNearDist, dep.getCityName(), dep.getDist(), dep.getBankName(), dep.getAddress(), dep.getWorTime(localDateTime), rubBuy, rubSell, usdBuy, usdSell, euroBuy, eurSell);
                        break;
                    case INFO:
                        str = String.format(templateDepartment, dep.getAddress(), dep.getTel(), dep.getWorkTimeOriginal(), rubBuy, rubSell, usdBuy, usdSell, euroBuy, eurSell);
                        break;
                }

                if (onlyBestCourses) {//только лушчие курсы
                    if (str.contains("<b>")) {
                        strings.add(EmojiParser.parseToUnicode(str));
                    }
                } else {
                    strings.add(EmojiParser.parseToUnicode(str));
                }


            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //see method List.subList();
        list.clear();

        if (strings.size() == 0) { //нет результатов удовлетворяющих условия
            String str = "По вашему запроси ничего не найдено :confused:";
            strings.add(EmojiParser.parseToUnicode(str));
        }

        return strings;
    }

    private static Map<TypeOfCurrency, MinMax> findMaxCourses(List<Department> list) {
        Map<TypeOfCurrency, MinMax> map = new HashMap<>();

        Double euroMinSell = list.stream().filter(d -> d.getEur().getValueSell() > 0.0d).mapToDouble(d -> d.getEur().getValueSell()).min().orElse(0.0d);
        Double usdMinSell = list.stream().filter(d -> d.getUsd().getValueSell() > 0.0d).mapToDouble(d -> d.getUsd().getValueSell()).min().orElse(0.0d);
        Double rubMinSell = list.stream().filter(d -> d.getRub().getValueSell() > 0.0d).mapToDouble(d -> d.getRub().getValueSell()).min().orElse(0.0d);
        Double euroMaxBuy = list.stream().filter(d -> d.getEur().getValueBuy() > 0.0d).mapToDouble(d -> d.getEur().getValueBuy()).max().orElse(0.0d);
        Double usdMaxBuy = list.stream().filter(d -> d.getUsd().getValueBuy() > 0.0d).mapToDouble(d -> d.getUsd().getValueBuy()).max().orElse(0.0d);
        Double rubMaxBuy = list.stream().filter(d -> d.getRub().getValueBuy() > 0.0d).mapToDouble(d -> d.getRub().getValueBuy()).max().orElse(0.0d);

        map.put(TypeOfCurrency.EUR, new MinMax(euroMinSell, euroMaxBuy));
        map.put(TypeOfCurrency.USD, new MinMax(usdMinSell, usdMaxBuy));
        map.put(TypeOfCurrency.RUB, new MinMax(rubMinSell, rubMaxBuy));

        return map;

    }

    private static String bestCourses(Double max, Double current) {

        if (max.compareTo(current) == 0) {
            return String.format(templateBold, max);
        } else {
            return current.toString();
        }
    }

    public static String getStringWithFirstUpperCase(String word) {

        String str = word.trim().toLowerCase();
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    public static String humanReadableFormatFromDuration(LocalTime localTime, LocalTime endTime) {
        Long min = Duration.between(localTime, endTime).toMinutes();
        return String.format(template_min_to_hour, (min / 60), (min % 60));
    }
}
