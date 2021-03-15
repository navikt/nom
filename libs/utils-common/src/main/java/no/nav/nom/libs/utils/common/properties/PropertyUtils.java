package no.nav.nom.libs.utils.common.properties;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

@Slf4j
public class PropertyUtils {

    public static void initDefaultProperties(String appName) {
        // Får ikke satt disse i common-app, mest sannsyneligvis pga rekkefølge de blir lastet.
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.main.cloud-platform", "kubernetes");

        // Vente på at alle requests skal bli ferdig
        System.setProperty("server.shutdown", "graceful");
        System.setProperty("app.name", appName);
        System.setProperty("spring.application.name", appName);
    }

    public static void readVaultSrvProps() {
        listSecretFiles();
        fileToProp("/var/run/secrets/nais.io/srv/username", "app.username");
        fileToProp("/var/run/secrets/nais.io/srv/password", "app.password");
    }

    private static void listSecretFiles() {
        var base = Paths.get("/var/run/secrets/nais.io");
        try (Stream<Path> paths = Files.walk(base)) {
            paths.forEach(p -> log.info("Vault file: {}", p.toAbsolutePath()));
        } catch (IOException e) {
            log.error("couldnt list vault files", e);
        }
    }

    private static void fileToProp(String file, String prop) {
        Path path = Paths.get(file);
        try {
            if (Files.exists(path)) {
                log.info("Reading property={} from={}", prop, file);
                String content = Files.readString(path);
                System.setProperty(prop, content);
            }
        } catch (Exception e) {
            throw new RuntimeException("Couldn't read file " + file);
        }
    }
}
