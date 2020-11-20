package no.nav.nom.apps.api;

import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ApiStarter {
    public static void main(String[] args) {
        System.setProperty("app.name", "nom-api");
        KafkaUtils.assignRandomTransactionIdPrefix();
        SpringApplication.run(ApiStarter.class, args);
    }
}