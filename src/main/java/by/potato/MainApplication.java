package by.potato;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;


class MainApplication {

    private static void initFunction() {

        ScheduledExecutorService scheduledExecutorService =
                Executors.newScheduledThreadPool(1);

//        scheduledExecutorService.scheduleAtFixedRate( new UpdateCourses(),
//                0,4,
//                TimeUnit.HOURS);
    }

    public static void main(String[] args) {


        LocalDateTime timeMinus = LocalDateTime.now().minus(10, ChronoUnit.MINUTES);

        LocalDateTime timeNOow = LocalDateTime.now().minus(222, ChronoUnit.MINUTES);

        if (timeMinus.compareTo(timeNOow) > 0) {
            System.out.println("sdfsdfsdfsdf");
        }

//
//        LatLng latLng = Geocoding.getCoordFromAddressYandex("г. Гродно, ул. Большая Троицкая, д.17, пом.3").get();
//
//
//        System.out.println(MainApplication.class.getSimpleName());
//
//        PropCheck propCheck = new PropCheck();
//
//        initFunction();

//        List<Day> test = new ArrayList<>();
//
//        List<String> mass = new ArrayList<>();
//        mass.add("Пн-Чт: 09:30-18:00 Перерыв: 14:15-15:00, 12:00-12:15");
//        mass.add("Пт: 09:30-17:00 Перерыв: 14:15-15:00,");
//        mass.add("Сб-Вс: Выходной");
//
//
//        for(String s :mass) {
//            WorkOfWeek.parse(s,test);
//        }
//
//        System.out.println(test);

    }

}

//Пн-Чт: 09:30-18:00 Перерыв: 14:15-15:00, 12:00-12:15
//        Пт: 09:30-17:00 Перерыв: 14:15-15:00, 12:00-12:15
//        Сб-Вс: Выходной
//	Пн-Пт: 09:00-18:00 Перерыв: 14:00-15:00,
//          Сб-Вс: Выходной Перерыв: 13:30-14:30

//Пн-Вс: 09:00-19:15 Перерыв: 13:00-14:00, 17:00-17:10