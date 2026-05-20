package no.nav.nom.apps.api.clients.slack;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Arrays;

@Slf4j
@Service
public class SlackService {

    private final SlackClient slackClient;
    private final AppProperties.Slack slackProperties;
    private final Environment environment;

    public SlackService(SlackClient slackClient, AppProperties appProperties, Environment environment) {
        this.slackClient = slackClient;
        this.slackProperties = appProperties.clients().slack();
        this.environment = environment;
    }

    public void sendOrgbotErrorOmadaUttrekk(String message) {
        if (isDev()) {
            log.info("Skipper Slack-melding i dev: {}", message);
            return;
        }
        slackClient.postMessage(slackProperties.channelIdPrimary(), message);
        slackClient.postMessage(slackProperties.channelIdSecondary(), message);
    }

    private boolean isDev() {
        var activeProfiles = Arrays.asList(environment.getActiveProfiles());
        return activeProfiles.contains("dev");
    }
}
