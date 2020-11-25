package no.nav.nom.apps.dvh;

import no.nav.nom.libs.utils.common.kafka.KafkaUtils;
import no.nav.nom.libs.utils.common.properties.PropertyUtils;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@ConfigurationPropertiesScan
public class DvhStarter {
    public static void main(String[] args) {
        PropertyUtils.initDefaultProperties("nom-dvh");
        KafkaUtils.assignRandomTransactionIdPrefix();
        SpringApplication.run(DvhStarter.class, args);
    }
}
