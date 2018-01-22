package by.potato;

import by.potato.helper.BotHelper;
import by.potato.helper.PropCheck;
import by.potato.helper.UpdateCourses;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.TelegramBotsApi;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.exceptions.TelegramApiException;

import java.util.Queue;
import java.util.concurrent.*;

import static by.potato.helper.PropCheck.BotApiKey;
import static by.potato.helper.PropCheck.BotName;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;


public class BestCoursesBot extends TelegramLongPollingBot {

    public static Queue<Update> updateMessages = new ConcurrentLinkedQueue<>();
    public static Queue<SendMessage> outStringMessage = new ConcurrentLinkedQueue<>();

    private static final int COUNT_BOT_HELPER = 5;

    public static void main(String[] args) throws SchedulerException {

        PropCheck propCheck = new PropCheck();

        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(new BestCoursesBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public BestCoursesBot() throws SchedulerException {

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        executorService.scheduleWithFixedDelay(() -> {

            while (true) {

                SendMessage sendMessage = outStringMessage.poll();

                if (sendMessage == null) {
                    break;
                }

                try {
                    execute(sendMessage);
                } catch (TelegramApiException e) {
                    outStringMessage.add(sendMessage);
                    e.printStackTrace();
                }
            }
        }, 0, 1, TimeUnit.SECONDS);


        ExecutorService ex = Executors.newFixedThreadPool(COUNT_BOT_HELPER);
        for (int i = 0; i < COUNT_BOT_HELPER; i++) {
            ex.submit(new BotHelper());
        }


//        ScheduledExecutorService scheduledExecutorService =
//                Executors.newScheduledThreadPool(1);
//
//        scheduledExecutorService.scheduleAtFixedRate(new UpdateCourses(),
//                0, 2, TimeUnit.HOURS);

        Scheduler scheduler = new StdSchedulerFactory().getScheduler();
        scheduler.start();

        JobDetail job = newJob(UpdateCourses.class)
                .withIdentity("updateCourses", "groupBot")
                .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger1", "group1")
                .withSchedule(cronSchedule("0 53 0,2,4,6,8,10,12,14,16,18,20,22 * * ?"))
                .build();

        scheduler.scheduleJob(job, trigger);

    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() || update.hasCallbackQuery()) {
            updateMessages.add(update);
        }
    }

    @Override
    public String getBotUsername() {
        return BotName;
    }

    @Override
    public String getBotToken() {
        return BotApiKey;
    }
}
