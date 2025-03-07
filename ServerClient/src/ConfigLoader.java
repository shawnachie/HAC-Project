package ServerClient.src;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class ConfigLoader {
    private Properties properties;

    public ConfigLoader(String filename) {
        properties = new Properties();

        try (FileInputStream input = new FileInputStream(filename)) {
            properties.load(input);
        } catch (IOException e) {
            System.err.println("Error loading configuration file: " + e.getMessage());
        }
    }

    public String getString(String key) {
        return properties.getProperty(key);
    }

    public int getInt(String key) {
        String value = properties.getProperty(key, "0");
        return Integer.parseInt(value);
    }

    public List<String> getList(String key) {
        String value = properties.getProperty(key, "");
        return Arrays.asList(value.split(","));
    }

    public static void main(String[] args) {
        ConfigLoader config = new ConfigLoader("General/config.properties");
        System.out.println("Node ID: " + config.getString("node.id"));
        System.out.println("Node Mode: " + config.getString("node.mode"));
        System.out.println("Port: " + config.getInt("udp.port"));
        System.out.println("Node IPs: " + config.getList("node.ips"));
    }
}