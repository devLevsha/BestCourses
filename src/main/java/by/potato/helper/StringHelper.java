package by.potato.helper;

import by.potato.Enum.Info;
import by.potato.Enum.TypeOfCurrency;
import by.potato.holder.Department;
import by.potato.Pairs.MinMax;
import com.vdurmont.emoji.EmojiParser;
import org.telegram.telegrambots.api.objects.Message;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.function.Predicate;

public class StringHelper {

    private static final String templateNearDist = "Город %s\nРасстояние %.3f км\nБанк %s\nАдрес %s\nRUB покупка %s\nRUB продажа %s\nUSD покупка %s\nUSD продажа %s\nEURO покупка %s\nEUR продажа %s";
    private static final String templateNear = "Город %s\nБанк %s\nАдрес %s\nRUB покупка %s\nRUB продажа %s\nUSD покупка %s\nUSD продажа %s\nEURO покупка %s\nEUR продажа %s";
    private static final String templateBold = "<b>%s :thumbsup:</b>";
    private static final String templateDepartment ="Адрес %s\nТелефон %s\nГрафик работы %s\nRUB покупка %s\nRUB продажа %s\nUSD покупка %s\nUSD продажа %s\nEURO покупка %s\nEUR продажа %s";

    public static List<String> getPrintNearDepartment(List<Department> list) {
        Predicate<String> trueAlways = x -> true;

        return courses(list,trueAlways, Info.DIST);
    }

    public static List<String> getBestCoursesByCity(List<Department> list) {
        Predicate<String> containBestCourse = x -> x.contains("<b>");


        return courses(list,containBestCourse , Info.NONE);
    }

    public static List<String> getDepartment(List<Department> list) {
        Predicate<String> trueAlways = x -> true;

        return courses(list,trueAlways, Info.DIST_DOP);
    }

    private static List<String> courses(List<Department> list, Predicate<String> print, Info info) {
        List<String> strings = new ArrayList<>();

        Map<TypeOfCurrency, MinMax> minMax = findMaxCourses(list);

        for (Department dep : list) {
            Message mess = new Message();

            String eurSell = "";
            if (minMax.get(TypeOfCurrency.EUR).getMinSell().compareTo(dep.getEur().getValueSell()) == 0) {
                eurSell = String.format(templateBold, dep.getEur().getValueSell().toString());
            } else {
                eurSell = dep.getEur().getValueSell().toString();
            }

            String euroBuy = "";
            if (minMax.get(TypeOfCurrency.EUR).getMaxBuy().compareTo(dep.getEur().getValueBuy()) == 0) {
                euroBuy = String.format(templateBold, dep.getEur().getValueBuy().toString());
            } else {
                euroBuy = dep.getEur().getValueBuy().toString();
            }

            String usdSell = "";
            if (minMax.get(TypeOfCurrency.USD).getMinSell().compareTo(dep.getUsd().getValueSell()) == 0) {
                usdSell = String.format(templateBold, dep.getUsd().getValueSell().toString());
            } else {
                usdSell = dep.getUsd().getValueSell().toString();
            }

            String usdBuy = "";
            if (minMax.get(TypeOfCurrency.USD).getMaxBuy().compareTo(dep.getUsd().getValueBuy()) == 0) {
                usdBuy = String.format(templateBold, dep.getUsd().getValueBuy().toString());
            } else {
                usdBuy = dep.getUsd().getValueBuy().toString();
            }

            String rubSell = "";
            if (minMax.get(TypeOfCurrency.RUB).getMinSell().compareTo(dep.getRub().getValueSell()) == 0) {
                rubSell = String.format(templateBold, dep.getRub().getValueSell().toString());
            } else {
                rubSell = dep.getRub().getValueSell().toString();
            }

            String rubBuy = "";
            if (minMax.get(TypeOfCurrency.RUB).getMaxBuy().compareTo(dep.getRub().getValueBuy()) == 0) {
                rubBuy = String.format(templateBold, dep.getRub().getValueBuy().toString());
            } else {
                rubBuy = dep.getRub().getValueBuy().toString();
            }

            String workTime = "";
            String tel = "";
            if(info == Info.DIST_DOP) {
                workTime = dep.getWorksTimeStr();
            }   tel = dep.getTel();

            try {
                String str = "";

                switch (info) {
                    case DIST:
                        str = String.format(templateNearDist, dep.getCityName(), dep.getDist(), dep.getBankName(), dep.getAddress(), rubBuy, rubSell, usdBuy, usdSell, euroBuy, eurSell);
                        break;
                    case NONE:
                        str = String.format(templateNear, dep.getCityName(), dep.getBankName(), dep.getAddress(), rubBuy, rubSell, usdBuy, usdSell, euroBuy, eurSell);
                        break;
                    case DIST_DOP:
                        str = String.format(templateDepartment, dep.getAddress(), tel, workTime, rubBuy, rubSell, usdBuy, usdSell, euroBuy, eurSell);
                        break;
                }

                if(print.test(str)){
                    strings.add(EmojiParser.parseToUnicode(str));
                }

            } catch (Exception e) {
                e.printStackTrace();
            }


        }

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


    public static String getStringWithFirstUpperCase(String word) {

        String str = word.trim().toLowerCase();
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
