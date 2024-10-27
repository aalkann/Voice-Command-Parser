package com.local.dev.config;

import java.util.Properties;
import java.io.IOException;
import java.io.InputStream;

public class ConfigReader {
    private static final Properties properties = new Properties();
    public static final String VOICE_COMMANDS_TOPIC = "voice-commands";
    public static final String RECOGNIZED_COMMAND_TOPIC = "recognized-commands";
    public static final String UNRECOGNIZED_COMMAND_TOPIC = "unrecognized-commands";

    static {
        try (InputStream input = ConfigReader.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IllegalArgumentException("Unable to find config.properties");
            }
            properties.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public static String getProperty(String key){
        return properties.getProperty(key);
    }

    public static Properties getProperties(){
        return properties;
    }
}
