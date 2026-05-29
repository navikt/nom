package no.nav.nom.apps.api.clients.slack;

import com.slack.api.Slack;
import no.nav.nom.apps.api.common.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SlackClient {

    private static final Logger log = LoggerFactory.getLogger(SlackClient.class);
    private static final int MAX_ATTEMPTS = 3;

    private final AppProperties.Slack appProperties;
    private final Slack slack = Slack.getInstance();

    public SlackClient(AppProperties appProperties) {
        this.appProperties = appProperties.clients().slack();
    }

    public void postMessage(String channelId, String text) {
        int attempt = 0;
        while (attempt < MAX_ATTEMPTS) {
            attempt++;
            try {
                var response = slack.methods(appProperties.token())
                    .chatPostMessage(req -> req.channel(channelId).text(text));
                if (response.isOk()) {
                    return;
                }
                log.warn("Slack postMessage attempt {}/{} failed: {}", attempt, MAX_ATTEMPTS, response.getError());
            } catch (Exception e) {
                log.warn("Slack postMessage attempt {}/{} threw exception", attempt, MAX_ATTEMPTS, e);
            }
            if (attempt < MAX_ATTEMPTS) {
                sleepExponential(attempt);
            }
        }
        log.error("Slack postMessage failed after {} attempts to channel {}", MAX_ATTEMPTS, channelId);
    }

    private void sleepExponential(int attempt) {
        try {
            long millis = (long) Math.pow(2, attempt - 1) * 1000L; // 1s, 2s, 4s
            Thread.sleep(millis);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}
