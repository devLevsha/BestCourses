package by.potato.Enum;

public enum Items {

    START("/start"),
    START_BOT("Главное меню"),
    COURSES_CITY("Курсы по городам"),
    TYPE_OF_INFO("Виды информации о городских обменниках"),
    NEAR_EXCHANGE("Обменные пункты"),
    INFO("Информация"),
    NEAR("Ближайшие"),
    DISTANCE("По расстоянию"),
    UNKNOW("Проблема сэр"),
    LOCATION_NEAR("Координаты"),
    NEXT("Следующие"),
    NEXT_DEP("Следующие отделения"),
    NEXT_DEP_INLINE("Отделение след"),
    LOCATION_DIST_STEP_ONE("Координаты"),
    LOCATION_DIST_STEP_TWO("Радиус"),
    BEST_COURSES("Лучшие курсы"),
    BANKS("Список банков"),
    BANK("Отделение"),
    QUESTION("Идеи"),
    SETTINGS("Настройки"),
    BACK("назад"),
    RUB_SELL("RUB продажа"),
    RUB_BUY("RUB покупка"),
    USD_SELL("USD продажа"),
    USD_BUY("USD покупка"),
    EUR_SELL("EUR продажа"),
    EUR_BUY("EUR покупка"),
    PHONE("Телефоны"),
    SCHEDULE("Рассписание"),
    SAVE("Сохранить");



    private final String text;

    Items(String text) {
        this.text = text;
    }

    public static Items parse(String str) {
        for (Items e : Items.values()) {
            if (e.getText().equals(str)) {
                return e;
            }
        }
        return Items.UNKNOW;
    }

    private String getText() {
        return text;
    }
}
