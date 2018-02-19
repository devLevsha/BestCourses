package by.potato;

import by.potato.helper.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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

    private static final Logger logger = LogManager.getLogger(BestCoursesBot.class.getSimpleName());
    private static final int COUNT_BOT_HELPERS = 5;
    public static Queue<Update> updateMessages = new ConcurrentLinkedQueue<>();
    public static Queue<SendMessage> outStringMessage = new ConcurrentLinkedQueue<>();

    private BestCoursesBot() {

        this.executorSendMessageToUser();
        this.executorInitBotHelper();

        SchedulerFactory sf = new StdSchedulerFactory();

        this.scheduleUpdateCourses(sf);
        this.scheduleUpdateUnusedDepartment(sf);
        this.scheduleUpdateWorkTime(sf);
    }

    public static void main(String[] args) {

        PropCheck propCheck = new PropCheck();

        ApiContextInitializer.init();

        TelegramBotsApi botsApi = new TelegramBotsApi();

        try {
            botsApi.registerBot(new BestCoursesBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void executorInitBotHelper() {
        ExecutorService ex = Executors.newFixedThreadPool(COUNT_BOT_HELPERS);
        for (int i = 0; i < COUNT_BOT_HELPERS; i++) {
            ex.submit(new BotHelper());
        }
    }

    private void executorSendMessageToUser() {

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
    }

    private void scheduleUpdateCourses(SchedulerFactory sf) {

        try {
            Scheduler scheduler = sf.getScheduler();
            scheduler.start();

            JobDetail job = newJob(UpdateCourses.class)
                    .withIdentity("updateCourses", "groupBot1")
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger1", "group1")
                    .withSchedule(cronSchedule("0 33 0,1,2,4,6,8,9,10,12,14,16,18,20,22,23 * * ?"))
                    .build();

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.error("Error scheduleUpdateCourses " + e.getMessage() + e.getCause());
        }
    }

    private void scheduleUpdateUnusedDepartment(SchedulerFactory sf) {

        try {
            Scheduler scheduler = sf.getScheduler();
            scheduler.start();

            JobDetail job = newJob(UpdateUnusedDepartment.class)
                    .withIdentity("unusedDepartment", "groupBot2")
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger2", "group2")
                    .withSchedule(cronSchedule("0 15 0 * * ?"))
                    .build();

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.error("Error scheduleUpdateUnusedDepartment " + e.getMessage() + e.getCause());
        }
    }

    private void scheduleUpdateWorkTime(SchedulerFactory sf) {
        try {
            Scheduler scheduler = sf.getScheduler();
            scheduler.start();

            JobDetail job = newJob(UpdateWorkTime.class)
                    .withIdentity("workTime", "groupBot3")
                    .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity("trigger3", "group3")
                    .withSchedule(cronSchedule("0 22 0,1,2,4,6,8,9,10,12,14,16,18,20,22,23 * * ?"))
                    //.withSchedule(cronSchedule("0 0 7 * * ?"))
                    .build();

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            logger.error("Error scheduleUpdateWorkTime " + e.getMessage() + e.getCause());
        }
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
