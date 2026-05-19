package no.nav.nom.apps.api.clients.slack;

import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.stereotype.Service;

@Service
public class SlackService {

    private final SlackClient slackClient;
    private final AppProperties.Slack slackProperties;

    public SlackService(SlackClient slackClient, AppProperties appProperties) {
        this.slackClient = slackClient;
        this.slackProperties = appProperties.clients().slack();
    }

    public void sendOrgbotErrorOmadaUttrekk(String message) {
        slackClient.postMessage(slackProperties.channelIdPrimary(), message);
        slackClient.postMessage(slackProperties.channelIdSecondary(), message);
    }
}
