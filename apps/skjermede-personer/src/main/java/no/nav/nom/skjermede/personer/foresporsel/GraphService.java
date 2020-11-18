package no.nav.nom.skjermede.personer.foresporsel;

import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.extensions.EmailAddress;
import com.microsoft.graph.models.extensions.IGraphServiceClient;
import com.microsoft.graph.models.extensions.ItemBody;
import com.microsoft.graph.models.extensions.Message;
import com.microsoft.graph.models.extensions.Recipient;
import com.microsoft.graph.models.generated.BodyType;
import com.microsoft.graph.requests.extensions.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class GraphService {

    private static final String TITTEL = "Forespørsel om skjerming";
    private static final String AVSENDER = "noreply-skjerming@nav.no";

    public String getNavident(final String accessToken) {
        return graphServiceClient(accessToken)
                .me()
                .buildRequest()
                .select("onPremisesSamAccountName")
                .get().onPremisesSamAccountName;
    }

    public void sendMail(final String content, final String recipient, final String accessToken) {
        val message = composeMessage(content, recipient);
        val graphServiceClient = graphServiceClient(accessToken);

        graphServiceClient.getLogger().setLoggingLevel(LoggerLevel.ERROR);
        graphServiceClient.me()
                .sendMail(message, null)
                .buildRequest()
                .post();
    }

    private IGraphServiceClient graphServiceClient(final String accessToken) {
        return GraphServiceClient.builder()
                .authenticationProvider(httpRequest ->
                    httpRequest.addHeader(HttpHeaders.AUTHORIZATION, String.format("Bearer %s", accessToken)))
                .buildClient();
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
