package by.potato.helper;

import by.potato.Enum.Items;
import by.potato.db.DataBaseHelper;
import by.potato.holder.Department;
import by.potato.holder.KeyboardMarkUp;
import by.potato.holder.StatusUser;
import com.google.maps.model.LatLng;
import com.vdurmont.emoji.EmojiParser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Location;
import org.telegram.telegrambots.api.objects.Update;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static by.potato.BestCoursesBot.outStringMessage;
import static by.potato.BestCoursesBot.updateMessages;
import static by.potato.Enum.Items.*;


public class BotHelper implements Runnable {

    private static final Logger logger = LogManager.getLogger(BotHelper.class.getSimpleName());

    private static ConcurrentMap<Long, StatusUser> history = new ConcurrentHashMap<>();

//    private static ConcurrentMap<Long, Deque<Items>> historyOfActions = new ConcurrentHashMap<>();
//    private static ConcurrentMap<Long, Optional<LatLng>> historyOfLocation = new ConcurrentHashMap<>();
//    private static ConcurrentMap<Long, Double> historyOfDistance = new ConcurrentHashMap<>();
//    private static ConcurrentMap<Long, String> historyOfCity = new ConcurrentHashMap<>();
    private Long chatId;
    private String messageInp;
    private Items action;
    private Optional<LatLng> location = Optional.empty();

    @Override
    public void run() {

        while (true) {

            Update update = updateMessages.poll();
            if (update == null) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            if (update.hasCallbackQuery()) {
                workWithCallBack(update);
                continue;
            }


            if (update.getMessage().hasText()) {
                this.messageInp = update.getMessage().getText();
                this.messageInp = EmojiParser.removeAllEmojis(this.messageInp).trim();
            } else {
                this.messageInp = "";
            }

            this.chatId = update.getMessage().getChatId();
            this.action = Items.parse(this.messageInp);


            Optional<Location> locationTemp = Optional.ofNullable(update.getMessage().getLocation());
            if (locationTemp.isPresent()) {
                LatLng latLng = new LatLng();
                latLng.lng = locationTemp.get().getLongitude();
                latLng.lat = locationTemp.get().getLatitude();

                this.location = Optional.of(latLng);

                switch (history.get(this.chatId).actions.getLast()) {
                    case NEAR:
                        this.action = LOCATION_NEAR;
                        break;
                    case DISTANCE:
                        this.action = LOCATION_DIST_STEP_ONE;
                        break;
                }

            } else {
                this.location = Optional.empty();
            }


            System.out.println(messageInp);


            SendMessage message = new SendMessage();
            message.setChatId(this.chatId);
            String messageOut;


            while (true) {//for action BACK

                switch (this.action) {
                    case START:
                    case START_BOT:
                        messageOut = "<i>Привет!\n</i>Я помогу тебе поменять деньги по выгодному курсу!";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getStartMenu());
                        inithPosition();
                        sendMessage(message);
                        break;

                    case COURSES_CITY:
                        messageOut = "Введите название города";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getBackKeyboard());
                        forwardPosition();
                        sendMessage(message);
                        break;

                    case TYPE_OF_INFO:
                        messageOut = "Лучшие курсы в городе за текущий день:thumbsup:\nСписок банков представленных в городе";
                        message.setText(EmojiParser.parseToUnicode(messageOut));
                        message.setReplyMarkup(KeyboardMarkUp.getTypeOfInfo());
                        forwardPosition();
                        sendMessage(message);
                        break;

                    case BEST_COURSES:
                        List<Department> departments = DataBaseHelper.getInstance().getCoursesByCity(history.get(this.chatId).city);
                        List<String> messagesDep = StringHelper.getBestCoursesByCity(departments, null);

                        for (String str : messagesDep) {
                            SendMessage mess = new SendMessage();
                            mess.setText(str);
                            mess.setChatId(this.chatId);
                            sendMessage(mess);
                        }

                        messageOut = "Список лучший курсов";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getBackKeyboard());
                        forwardPosition();
                        sendMessage(message);
                        break;

                    case BANKS:
                        List<String> banks = DataBaseHelper.getInstance().getBanksByCity(history.get(this.chatId).city);

                        SendMessage mes = new SendMessage();
                        messageOut = "Список Банков";
                        mes.setChatId(this.chatId);
                        mes.setText(messageOut);
                        mes.setReplyMarkup(KeyboardMarkUp.getBanks(banks));
                        forwardPosition();
                        sendMessage(mes);

                        messageOut = "Выберите интересущий банк и нажмите на него";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getBackKeyboard());
                        forwardPosition();
                        sendMessage(message);

                        break;

                    case NEAR_EXCHANGE:
                        messageOut = "Ближайшие - список ближайших работающих обменных пункта\nРасстояние - список работающих обменных пунктов в пределах Х км";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getNearKeyboard());
                        forwardPosition();
                        sendMessage(message);
                        break;


                    case DISTANCE:
                    case NEAR:
                        messageOut = "Введите свой адрес или поделитесь координами";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getDistNearKeyboard());
                        forwardPosition();
                        sendMessage(message);
                        break;


                    case LOCATION_NEAR:
                        history.get(this.chatId).localDateTime = LocalDateTime.now();
                        history.get(this.chatId).departments = DataBaseHelper.getInstance().geoDepartment(this.location, history.get(this.chatId).localDateTime);

                    case NEXT:
                        List<String> messagesNear = StringHelper.getPrintNearDepartment(history.get(this.chatId).departments,  history.get(this.chatId).localDateTime);

                        for (String str : messagesNear) {
                            SendMessage mess = new SendMessage();
                            mess.setText(str);
                            mess.setChatId(this.chatId);
                            sendMessage(mess);
                        }

                        messageOut = "Введите свой адрес или поделитесь координами";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getDistNearNextKeyboard());
                        sendMessage(message);
                        break;



                    case LOCATION_DIST_STEP_ONE:
                        messageOut = "Введите радиус поиска в км";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getBackKeyboard());
                        sendMessage(message);
                        forwardPosition();
                        if (this.location.isPresent()) {
                            history.get(this.chatId).location = this.location;
                        }

                        break;

                    case LOCATION_DIST_STEP_TWO:
                        history.get(this.chatId).localDateTime = LocalDateTime.now();
                        List<Department> departmentsDist = DataBaseHelper.getInstance().geoDepartmentDist(history.get(this.chatId).location, history.get(this.chatId).distance);
                        List<String> messagesDist = StringHelper.getPrintNearDepartment(departmentsDist,  history.get(this.chatId).localDateTime);


                        for (String str : messagesDist) {
                            SendMessage mess = new SendMessage();
                            mess.setText(str);
                            mess.setChatId(this.chatId);
                            sendMessage(mess);
                        }

                        messageOut = "Введите свой адрес или поделитесь координами";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getDistNearKeyboard());
                        forwardPosition();
                        sendMessage(message);
                        break;


                    case INFO:
                        messageOut = "Данные любезно предоставлены <a href=\"http://www.myfin.by/\">MyFin</a>";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getBackKeyboard());
                        forwardPosition();
                        sendMessage(message);
                        break;

                    case QUESTION:
                        messageOut = "Напишите Ваш вопрос или предложение";
                        message.setText(messageOut);
                        message.setReplyMarkup(KeyboardMarkUp.getQuestion());
                        forwardPosition();
                        sendMessage(message);
                        break;

                    case BACK:
                        this.action = backPosition();
                        continue;


                    default:

                        switch (history.get(chatId).actions.getLast()) { //последнее действие пользователя
                            case NEAR: //пользователь ввёл свои координаты через стороку
                                this.action = LOCATION_NEAR;
                                this.location = Geocoding.getCoordFromAddress(messageInp);

                                if (!this.location.isPresent()) { //не удалось получить координаты из адреса
                                    SendMessage mess = new SendMessage();
                                    String str = "К сожалению введённый адрес не корректен :confused:\nПовторите ввод";
                                    mess.setText(EmojiParser.parseToUnicode(str));
                                    mess.setChatId(this.chatId);
                                    sendMessage(mess);
                                    this.action = NEAR;
                                }
                                continue;

                            case LOCATION_DIST_STEP_TWO:
                            case DISTANCE: //пользователь ввёл свои координаты через стороку
                                this.action = LOCATION_DIST_STEP_ONE;
                                this.location = Geocoding.getCoordFromAddress(messageInp);

                                if (!this.location.isPresent()) { //не удалось получить координаты из адреса
                                    SendMessage mess = new SendMessage();
                                    String str = "К сожалению был введённый некорректный адрес :confused:\nПовторите ввод";
                                    mess.setText(EmojiParser.parseToUnicode(str));
                                    mess.setChatId(this.chatId);
                                    sendMessage(mess);
                                    this.action = DISTANCE;
                                }

                                continue;

                            case LOCATION_DIST_STEP_ONE://пользователь ввёл растояние в км

                                try {

                                    history.get(this.chatId).distance = new Double(messageInp);
                                    this.action = Items.LOCATION_DIST_STEP_TWO;
                                } catch (NumberFormatException e) {

                                    SendMessage mess = new SendMessage();
                                    String str = "К сожалению вы ввели не число :confused:\nПовторите ввод";
                                    mess.setText(EmojiParser.parseToUnicode(str));
                                    mess.setChatId(this.chatId);
                                    mess.setReplyMarkup(KeyboardMarkUp.getStartMenu());
                                    sendMessage(mess);
                                    this.action = LOCATION_DIST_STEP_ONE;

                                }

                                continue;

                            case COURSES_CITY:
                                String cityName = StringHelper.getStringWithFirstUpperCase(messageInp);

                                //город есть в списках )
                                if (DataBaseHelper.getInstance().getCities().contains(cityName)) {
                                    history.get(this.chatId).city = cityName;
                                    this.action = TYPE_OF_INFO;
                                } else {
                                    SendMessage mess = new SendMessage();
                                    String str = "К сожалению Ваш город не найдет :confused:\nПовторите ввод";
                                    mess.setText(EmojiParser.parseToUnicode(str));
                                    mess.setChatId(this.chatId);
                                    sendMessage(mess);
                                    this.action = COURSES_CITY;
                                }

                                continue;


                            case QUESTION:
                                DataBaseHelper.getInstance().insertQuestion(this.chatId,messageInp);

                                SendMessage messOut = new SendMessage();
                                String text = "Отправлено :outbox_tray:";
                                messOut.setText(EmojiParser.parseToUnicode(text));
                                messOut.setChatId(this.chatId);
                                sendMessage(messOut);
                                inithPosition();
                                this.action = START;
                                continue;


                            default:
                                SendMessage mess = new SendMessage();
                                String str = "Команда не распознана ";
                                mess.setText(str);
                                mess.setChatId(this.chatId);
                                sendMessage(mess);
                                inithPosition();
                                this.action = START;
                                continue;
                        }
                }

                break;
            }
        }
    }

    private void workWithCallBack(Update update) {

        String[] outputStr = update.getCallbackQuery().getData().split("_");
        this.action = Items.parse(outputStr[0]);
        this.messageInp = outputStr[1];
        this.chatId = update.getCallbackQuery().getMessage().getChatId();

        switch (this.action) {
            case BANK:
                List<Department> departments = DataBaseHelper.getInstance().getDepartmentByBankAndCity(history.get(this.chatId).city, messageInp);
                List<String> prints = StringHelper.getBestCoursesByCity(departments,null);

                for (String str : prints) {
                    SendMessage mess = new SendMessage();
                    mess.setText(str);
                    mess.setChatId(this.chatId);
                    sendMessage(mess);
                }

                SendMessage mess = new SendMessage();
                String str = "Cписок всех отделений :arrow_up_small:";
                mess.setText(EmojiParser.parseToUnicode(str));
                mess.setChatId(this.chatId);
                mess.setReplyMarkup(KeyboardMarkUp.getBackKeyboard());
                sendMessage(mess);
                break;
        }


    }

    private void inithPosition() {

        StatusUser statusUser = new StatusUser();
        statusUser.actions.add(Items.START);
        history.put(this.chatId,statusUser);
    }

    private void forwardPosition() {
        history.get(this.chatId).actions.add(this.action);

    }

    private Items backPosition() {
        try {
            Items position = history.get(this.chatId).actions.getLast();

            while (position == history.get(this.chatId).actions.getLast()) {
                history.get(this.chatId).actions.removeLast();
            }

        } catch (NoSuchElementException | NullPointerException e) {
            inithPosition();
        }

        return history.get(this.chatId).actions.getLast();
    }

    private void sendMessage(SendMessage message) {
        message.setParseMode("HTML");

        outStringMessage.add(message);
    }


}
