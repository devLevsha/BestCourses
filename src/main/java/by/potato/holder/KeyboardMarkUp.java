package by.potato.holder;

import com.google.maps.model.LatLng;
import com.vdurmont.emoji.EmojiParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class KeyboardMarkUp {

    private static final Logger logger = LogManager.getLogger(KeyboardMarkUp.class.getSimpleName());
    private static final String templateLocation = "http://www.maps.yandex.ru/?text=%f,%f";

    public static ReplyKeyboardMarkup getStartMenu() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);
        String textOnButton;

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow rowOne = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Курсы по городам :house:");
        rowOne.add(textOnButton);

        KeyboardRow rowTwo = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Обменные пункты :us::euro::ru:");
        rowTwo.add(textOnButton);

        KeyboardRow rowSix = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Настройки ⚙");
        rowSix.add(textOnButton);

        KeyboardRow rowThree = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Информация :information_source:");
        rowThree.add(textOnButton);

        KeyboardRow rowFour = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Идеи :question:");
        rowThree.add(textOnButton);


        keyboard.add(rowOne);
        keyboard.add(rowTwo);
        keyboard.add(rowSix);
        keyboard.add(rowThree);

        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getTypeOfInfo() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        String textOnButton;
        List<KeyboardRow> keyboard = new ArrayList<>();


        KeyboardRow rowOne = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Лучшие курсы :currency_exchange:");
        rowOne.add(textOnButton);

        KeyboardRow rowTwo = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Список банков :bookmark_tabs:");
        rowTwo.add(textOnButton);

        keyboard.add(rowOne);
        keyboard.add(rowTwo);
        keyboard.add(getBackButton());

        keyboardMarkup.setKeyboard(keyboard);


        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getQuestion() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        String textOnButton;
        List<KeyboardRow> keyboard = new ArrayList<>();


        keyboard.add(getBackButton());

        keyboardMarkup.setKeyboard(keyboard);


        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getNearKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        String textOnButton;
        List<KeyboardRow> keyboard = new ArrayList<>();


        KeyboardRow rowOne = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Ближайшие :currency_exchange:");
        rowOne.add(textOnButton);

        KeyboardRow rowTwo = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("По расстоянию :currency_exchange:");
        rowTwo.add(textOnButton);

        keyboard.add(rowOne);
        keyboard.add(rowTwo);
        keyboard.add(getBackButton());

        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getDistNearNextKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        String textOnButton;
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow rowOne = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Следующие :arrow_right:");
        KeyboardButton next = new KeyboardButton();
        next.setText(textOnButton);
        rowOne.add(next);

        KeyboardRow rowTwo = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Координаты :round_pushpin:");
        KeyboardButton location = new KeyboardButton();
        location.setRequestLocation(true);
        location.setText(textOnButton);
        rowTwo.add(location);

        keyboard.add(rowOne);
        keyboard.add(rowTwo);
        keyboard.add(getBackButton());

        keyboardMarkup.setKeyboard(keyboard);


        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getDistNearKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        String textOnButton;
        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow rowOne = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Координаты :round_pushpin:");
        KeyboardButton location = new KeyboardButton();
        location.setRequestLocation(true);
        location.setText(textOnButton);
        rowOne.add(location);

        keyboard.add(rowOne);
        keyboard.add(getBackButton());

        keyboardMarkup.setKeyboard(keyboard);


        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getSearchCities() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);


        KeyboardRow rowOne = new KeyboardRow();
        rowOne.add("Минск");
        rowOne.add("Гродно");
        rowOne.add("Витебск");

        KeyboardRow rowTwo = new KeyboardRow();
        rowTwo.add("Брест");
        rowTwo.add("Гомель");
        rowTwo.add("Могилев");

        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(rowOne);
        keyboard.add(rowTwo);
        keyboard.add(getBackButton());

        keyboardMarkup.setKeyboard(keyboard);


        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getDepNext() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        String textOnButton;
        KeyboardRow rowOne = new KeyboardRow();
        textOnButton = EmojiParser.parseToUnicode("Следующие отделения :arrow_right:");
        KeyboardButton next = new KeyboardButton();
        next.setText(textOnButton);
        rowOne.add(next);


        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(rowOne);
        keyboard.add(getBackButton());


        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getBackKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setResizeKeyboard(true);

        List<KeyboardRow> keyboard = new ArrayList<>();

        keyboard.add(getBackButton());

        keyboardMarkup.setKeyboard(keyboard);

        return keyboardMarkup;
    }

    public static ReplyKeyboardMarkup getHideKeyboard() {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setOneTimeKeyboard(true);

        return keyboardMarkup;
    }


    public static InlineKeyboardMarkup getDepNextInline() {

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(new InlineKeyboardButton().setText("Следующие отделения").setCallbackData("Отделение след"));

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);

        return markupInline;
    }

    public static InlineKeyboardMarkup getLocationButton(LatLng location) {

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();

        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(new InlineKeyboardButton().setText("На карте").setUrl(String.format(templateLocation, location.lat, location.lng)));

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);

        return markupInline;
    }

    public static InlineKeyboardMarkup getBanks(List<String> banks) {

        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        for (String bank : banks) {

            List<InlineKeyboardButton> rowInline = new ArrayList<>();
            rowInline.add(new InlineKeyboardButton().setText(bank).setCallbackData("Отделение_" + bank));

            rowsInline.add(rowInline);

        }

        markupInline.setKeyboard(rowsInline);

        return markupInline;
    }

    private static KeyboardRow getBackButton() {

        KeyboardRow rowOne = new KeyboardRow();

        rowOne.add(EmojiParser.parseToUnicode("Главное меню :arrows_counterclockwise:"));
        rowOne.add(EmojiParser.parseToUnicode("назад :back:"));

        return rowOne;
    }

    private static List<InlineKeyboardButton> getBackButtonInline() {

        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        rowInline.add(new InlineKeyboardButton().
                setText(EmojiParser.parseToUnicode("назад :back:")).
                setCallbackData("back"));

        return rowInline;
    }
}

