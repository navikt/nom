package no.nav.nom.apps.proxy.common;

import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class CommonConfig {
    // Marker for use with logger to ensure that sensitive logs are filtered into the correct log
    public static final Marker TEAM_LOGS = MarkerFactory.getMarker("TEAM_LOGS");

}
