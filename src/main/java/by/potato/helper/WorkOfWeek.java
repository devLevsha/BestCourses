package by.potato.helper;

import by.potato.Pairs.Breaks;
import by.potato.holder.Day;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;


public class WorkOfWeek {

    private static final Logger logger = LogManager.getLogger(WorkOfWeek.class.getSimpleName());

    private static final int COUNT_DAY_OF_WEEK = 7;

    private static final String []allDays = {"пн","вт","ср","чт","пт","сб","вс"};

    public static void parse(String str, List<Day> result) {

        try {
            //Пн-Пт: 09:00-19:00,
            String workStr = str.toLowerCase();
            //пн-пт: 09:00-19:00,


            if (workStr.endsWith(",")) {
                workStr = workStr.substring(0, workStr.length() - 1);
                //пн-пт: 09:00-19:00,
            }

            //сб: 10:00-17:00 перерыв: 14:00-15:00, 14:00-15:00
            String[] elementsWork = workStr.split("перерыв:");
            //сб: 10:00-17:00 , 14:00-15:00, 14:00-15:00

            List<Breaks> breaks = null;
            if (elementsWork.length == 2) { //отделение работает с перерывами
                //список всех перерывов для текущего дня
                try {
                    breaks = parseBreaks(elementsWork[1]);
                } catch (Exception e) {
                    logger.error(String.format("Error mess %s, cause %s, string %s", e.getMessage(), e.getCause().toString(), str));
                }
            }


            //сб: 10:00-17:00 , 14:00-15:00, 14:00-15:00
            String[] workTime = elementsWork[0].trim().split(" ");
            //сб:,10:00-17:00

            //пн-пт:
            String days = workTime[0].trim().replace(":", "");
            //пн-пт

            ////пн-пт
            String[] day = days.split("-");
            //пн,пт

            //10:00-17:00
            String[] time = workTime[1].trim().split("-");
            //10:00,17:00

            for (int i = 0; i < COUNT_DAY_OF_WEEK; i++) {

                //пропуск лишних дней до дня который парсится
                if (allDays[i].compareTo(day[0]) != 0) {
                    continue;
                }

                for (int j = i; j < COUNT_DAY_OF_WEEK; j++) {

                    DayOfWeek dayOfWeek = getDayOfWeek(allDays[j]);
                    LocalTime begin;
                    LocalTime end;

                    switch (time[0]) {
                        case "выходной":
                            begin = LocalTime.parse("00:00");
                            end = LocalTime.parse("00:00");
                            break;
                        case "круглосуточно":
                            begin = LocalTime.parse("00:00");
                            end = LocalTime.parse("23:59:59");
                            break;
                        default:
                            try {
                                begin = LocalTime.parse(time[0]);
                                end = LocalTime.parse(time[1]);
                            } catch (DateTimeParseException e) {
                                begin = LocalTime.parse("00:00");
                                end = LocalTime.parse("00:00");

                                logger.error(String.format("Parse time of work. Error mess %s, cause %s, string %s", e.getMessage(), e.getCause().toString(), str));

                            }
                    }

                    result.add(new Day(dayOfWeek, begin, end, breaks));

                    //break first FOR
                    if (allDays[j].compareTo(day[day.length - 1]) == 0) {
                        i = COUNT_DAY_OF_WEEK;
                        break;
                    }
                }

            }

        }catch (Exception e) {
            System.err.println("BAD string " + str);
            e.printStackTrace();
        }

    }

    private static DayOfWeek getDayOfWeek(String str) {
        switch (str) {
            case "пн":
                return DayOfWeek.MONDAY;
            case "вт":
                return DayOfWeek.TUESDAY;
            case "ср":
                return DayOfWeek.WEDNESDAY;
            case "чт":
                return DayOfWeek.THURSDAY;
            case "пт":
                return DayOfWeek.FRIDAY;
            case "сб":
                return DayOfWeek.SATURDAY;
            case "вс":
                return DayOfWeek.SUNDAY;
        }
        return null;
    }

    private static List<Breaks> parseBreaks(String str) throws Exception {

        List<Breaks> result = new ArrayList<>(3);

        String[] breaks = str.trim().split(",");

        for(String s : breaks) {
            String [] time = s.split("-");

            try {
                result.add(new Breaks(LocalTime.parse(time[0].trim()), LocalTime.parse(time[1].trim())));
            } catch (DateTimeParseException e) {
                throw new Exception("Parsing breaks" , e.getCause());
            }
        }

        return result;
    }
}
