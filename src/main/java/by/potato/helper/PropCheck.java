package by.potato.helper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

public class PropCheck {

    public static String BotName;
    public static String BotApiKey;
    public static String ORA_DB_LOGIN;
    public static String ORA_DB_PASS;
    public static String ORA_DB_URL;

    private File configFile;
    private final String propertiesFname;

    private Properties  properties;

    private static final Logger logger = LogManager.getLogger(Properties.class.getSimpleName());

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

    }

    private void readProps() {
        FileReader fr;
        try {
            fr = new FileReader(this.configFile);
            this.properties.clear();
            this.properties.load(fr);

        } catch (FileNotFoundException e) {
            logger.error(e.getMessage(),e.getCause());
        } catch (IOException e) {
            logger.error(e.getMessage(),e.getCause());
        }
    }
}
