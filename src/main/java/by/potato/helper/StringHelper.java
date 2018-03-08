package by.potato.helper;

import by.potato.Enum.Info;
import by.potato.Enum.TypeOfCurrency;
import by.potato.Pairs.MinMax;
import by.potato.holder.Department;
import by.potato.holder.StatusUser;
import by.potato.holder.UserSettings;
import com.google.maps.model.LatLng;
import com.vdurmont.emoji.EmojiParser;
import org.apache.commons.lang3.tuple.Pair;
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
    private static final String templateBold = "<b>%s :thumbsup:</b>";
    private static final int STEP_FOR_INFO = 5;
    private static final int STEP_INIT_POSITION = 0;


    public static Pair<List<String>, List<LatLng>> getPrintDepartment(StatusUser statusUser) {
        List<Department> result = getFiveElements(statusUser.departments);

        return courses(result, statusUser.info, statusUser.localDateTime, false, statusUser.userSettings);
    }

    public static Pair<List<String>, List<LatLng>> getBestCoursesByCity(List<Department> list, LocalDateTime localDateTime, UserSettings userSettings) {
        return courses(list, Info.INFO, localDateTime, true, userSettings);
    }

    public static Pair<List<String>, List<LatLng>> getBestCoursesByCityNext(Pair<List<String>, List<LatLng>> list) {

        List<String> elements = getFiveElements(list.getLeft());
        List<LatLng> locations = getFiveElements(list.getRight());

        List<String> result = new ArrayList<>(elements);
        List<LatLng> resultLocation = new ArrayList<>(locations);

        elements.clear();
        locations.clear();

        return Pair.of(result, resultLocation);
    }

    private static <T> List<T> getFiveElements(List<T> list) {
        List<T> result;
        try {
            result = list.subList(STEP_INIT_POSITION, STEP_FOR_INFO);
        } catch (IndexOutOfBoundsException e) {
            //если элементов меньше 5
            result = list.subList(STEP_INIT_POSITION, list.size());
        }
        return result;
    }


    private static Pair<List<String>, List<LatLng>> courses(List<Department> list, Info info, LocalDateTime localDateTime, Boolean onlyBestCourses, UserSettings userSettings) {

        List<String> strings = new ArrayList<>();
        List<LatLng> locations = new ArrayList<>();

        Map<TypeOfCurrency, MinMax> minMax = findMaxCourses(list);

        for (Department dep : list) {
            Message mess = new Message();

            String eurSell = bestCourses(minMax.get(TypeOfCurrency.EUR).getMinSell(), dep.getEur().getValueSell());
            String eurBuy = bestCourses(minMax.get(TypeOfCurrency.EUR).getMaxBuy(), dep.getEur().getValueBuy());
            String usdSell = bestCourses(minMax.get(TypeOfCurrency.USD).getMinSell(), dep.getUsd().getValueSell());
            String usdBuy = bestCourses(minMax.get(TypeOfCurrency.USD).getMaxBuy(), dep.getUsd().getValueBuy());
            String rubSell = bestCourses(minMax.get(TypeOfCurrency.RUB).getMinSell(), dep.getRub().getValueSell());
            String rubBuy = bestCourses(minMax.get(TypeOfCurrency.RUB).getMaxBuy(), dep.getRub().getValueBuy());

            try {
                String str = "";

                String currencies =
                        (userSettings.getRubSell() ? "\nRUB продажа " + rubSell : "")
                                + (userSettings.getRubBuy() ? "\nRUB покупка " + rubBuy : "")
                                + (userSettings.getUsdSell() ? "\nUSD продажа " + usdSell : "")
                                + (userSettings.getUsdBuy() ? "\nUSD покупка " + usdBuy : "")
                                + (userSettings.getEurSell() ? "\nEUR продажа " + eurSell : "")
                                + (userSettings.getEurBuy() ? "\nEUR покупка " + eurBuy : "");

                switch (info) {
                    case NEAR:
                        str = dep.getAddress()
                                + String.format("\nРасстояние %.3f км", dep.getDist())
                                + "\nВремя " + dep.getWorTime(localDateTime)
                                + currencies;
                        break;
                    case INFO:

                        str = "Адрес " + dep.getAddress()
                                + (userSettings.getPhone() ? "\nТелефон " + dep.getTel() : "")
                                + (userSettings.getWorkTime() ? "\nГрафик работы " + dep.getWorkTimeOriginal() : "")
                                + currencies;
                        break;
                }

                if (onlyBestCourses) {//только лушчие курсы
                    if (str.contains("<b>")) {
                        strings.add(EmojiParser.parseToUnicode(str));
                        locations.add(dep.getLocation());
                    }
                } else {
                    strings.add(EmojiParser.parseToUnicode(str));
                    locations.add(dep.getLocation());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        //see method List.subList();
        list.clear();

        return Pair.of(strings, locations);
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
