package no.nav.nom.skjermede.personer.tps.config;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@NoArgsConstructor
@ConfigurationProperties(prefix = "tps.mq")
public class JmsConfigProperties {

    private String managerUrl;
    private String channelName;
    private String queueName;
    private String username;
}
