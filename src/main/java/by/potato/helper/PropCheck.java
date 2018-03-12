package by.potato.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class PropCheck {

    private static final Logger logger = LogManager.getLogger(Properties.class.getSimpleName());

    public static String BotName;
    public static String BotApiKey;
    public static String ORA_DB_LOGIN;
    public static String ORA_DB_PASS;
    public static String ORA_DB_URL;
    public static String UPDATE_COURSES_SCHEDULE;
    public static String UPDATE_UNUSED_DEPARTMENT;
    public static String UPDATE_WORK_TIME;
    public static Integer COUNT_BOT_HELPER;

    private String propertiesFname;
    private File configFile;
    private Properties properties;

    public PropCheck() {

        this.propertiesFname = System.getProperty("user.dir") + "/conf/bot.properties";
        this.configFile = new File(this.propertiesFname);
        this.properties = new Properties();
        readProps();


        BotName = this.properties.getProperty("bot_name");
        BotApiKey = this.properties.getProperty("bot_api_key");
        ORA_DB_LOGIN = this.properties.getProperty("db_login");
        ORA_DB_PASS = this.properties.getProperty("db_pass");
        ORA_DB_URL = this.properties.getProperty("db_url");
        UPDATE_COURSES_SCHEDULE = this.properties.getProperty("update_courses");
        UPDATE_UNUSED_DEPARTMENT = this.properties.getProperty("update_unused_department");
        UPDATE_WORK_TIME = this.properties.getProperty("update_work_time");
        COUNT_BOT_HELPER = Integer.parseInt(this.properties.getProperty("count_bot_helper"));
    }

    private void readProps() {
        FileReader fr;
        try {
            fr = new FileReader(this.configFile);
            this.properties.clear();
            this.properties.load(fr);

        } catch (IOException e) {
            logger.error("Error PropCheck " + e.getMessage() + e.getCause());
        }
    }
}
