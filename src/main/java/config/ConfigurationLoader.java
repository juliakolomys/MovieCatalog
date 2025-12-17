package config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigurationLoader {

    private final Properties properties;
    public ConfigurationLoader() throws IOException {
        this.properties = new Properties();

        try (InputStream input = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new IOException("Unable to find application.properties in classpath");
            }
            properties.load(input);
        }
    }

    public String getString(String key) {
        String value = properties.getProperty(key);
        if (value == null) {
            throw new RuntimeException("Configuration key not found: " + key);
        }
        return value;
    }

    public int getInt(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Configuration key is not a valid integer: " + key, e);
        }
    }
}