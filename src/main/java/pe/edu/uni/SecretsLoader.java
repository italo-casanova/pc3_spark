package pe.edu.uni;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Properties;

public class SecretsLoader {
    public static Properties loadSecrets(String filename) {
        Properties properties = new Properties();

        try (InputStream input = Files.exists(Paths.get(filename))
                ? Files.newInputStream(Paths.get(filename))
                : SecretsLoader.class.getClassLoader().getResourceAsStream(filename)) {

            if (input == null) {
                throw new IOException("Unable to find " + filename);
            }
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load secrets from " + filename, e);
        }
        return properties;
    }
}
