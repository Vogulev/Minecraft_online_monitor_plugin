package com.vogulev.online_monitor.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.logging.Logger;

/**
 * Static manager for plugin localization
 * Supports English (en) and Russian (ru) languages
 */
public class LocalizationManager {
    private static final Logger logger = Logger.getLogger("OnlineMonitor");
    private static ResourceBundle messages;
    private static String language;

    private LocalizationManager() {
    }

    /**
     * Initializes localization with specified language
     * @param lang Language code (en or ru)
     */
    public static void initialize(String lang) {
        language = lang != null ? lang : "en";
        loadMessages();
    }

    /**
     * Loads messages from properties file
     */
    private static void loadMessages() {
        String fileName = "messages_" + language + ".properties";

        try (InputStream stream = LocalizationManager.class.getClassLoader().getResourceAsStream(fileName)) {
            if (stream == null) {
                logger.warning("Localization file not found: " + fileName + ", using English");
                try (InputStream enStream = LocalizationManager.class.getClassLoader().getResourceAsStream("messages_en.properties")) {
                    if (enStream != null) {
                        messages = new PropertyResourceBundle(new InputStreamReader(enStream, StandardCharsets.UTF_8));
                    } else {
                        logger.severe("English localization file not found!");
                    }
                }
            } else {
                messages = new PropertyResourceBundle(new InputStreamReader(stream, StandardCharsets.UTF_8));
                logger.info("Loaded localization: " + language);
            }
        } catch (IOException e) {
            logger.severe("Failed to load localization: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Gets a localized message by key
     * @param key Message key
     * @return Localized message or key if not found
     */
    public static String getMessage(String key) {
        if (messages == null) {
            return key;
        }

        try {
            return messages.getString(key);
        } catch (Exception e) {
            logger.warning("Missing translation key: " + key);
            return key;
        }
    }

    /**
     * Gets a localized message with parameters
     * @param key Message key
     * @param params Parameters to insert into message
     * @return Formatted localized message
     */
    public static String getMessage(String key, Object... params) {
        String message = getMessage(key);
        try {
            return MessageFormat.format(message, params);
        } catch (Exception e) {
            logger.warning("Failed to format message: " + key + " with params");
            return message;
        }
    }

    /**
     * Gets the current language code
     * @return Language code (en or ru)
     */
    public static String getLanguage() {
        return language;
    }
}
