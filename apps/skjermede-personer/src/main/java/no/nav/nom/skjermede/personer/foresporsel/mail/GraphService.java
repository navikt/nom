package no.nav.nom.skjermede.personer.foresporsel.mail;

import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.extensions.EmailAddress;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemBody;
import com.microsoft.graph.models.extensions.Message;
import com.microsoft.graph.models.extensions.Recipient;
import com.microsoft.graph.models.generated.BodyType;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class GraphService {

    private static final String TITTEL = "Forespørsel om skjerming";
    private static final String AVSENDER = "noreply-skjerming@nav.no";

    private final TokenService tokenService;

    public void sendMail(final String content, final String recipient) throws Exception {
        final Message message = composeMessage(content, recipient);
        final String accessToken = tokenService.getAccessToken();
        final IGraphServiceClient graphService = GraphServiceClient.builder()
                    .authenticationProvider(httpRequest -> {
                        httpRequest.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken));
                    }).buildClient();

            graphService.getLogger().setLoggingLevel(LoggerLevel.ERROR);

            graphService.me()
                    .sendMail(message, null)
                    .buildRequest()
                    .post();
    }

    private Message composeMessage(final String content, final String recipient) {
        final ItemBody body = new ItemBody();
        body.contentType = BodyType.HTML;
        body.content = content;

        final Message message = new Message();
        message.subject = TITTEL;
        message.body = body;
        message.toRecipients = Collections.singletonList(makeRecipient(recipient));
        message.from = makeRecipient(AVSENDER);

        return message;
    }

    private Recipient makeRecipient(final String recipient) {
        final EmailAddress address = new EmailAddress();
        address.address = recipient;

        final Recipient rcpt = new Recipient();
        rcpt.emailAddress = address;

        return rcpt;
    }
}
