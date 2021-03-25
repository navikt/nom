package no.nav.nom.skjermede.personer.foresporsel;

import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.BodyType;
import com.microsoft.graph.models.EmailAddress;
import com.microsoft.graph.models.ItemBody;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.User;
import com.microsoft.graph.models.UserSendMailParameterSet;
import com.microsoft.graph.requests.GraphServiceClient;
import lombok.RequiredArgsConstructor;
import lombok.val;
import okhttp3.Request;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class GraphService {

    private static final String TITTEL = "Forespørsel om skjerming";
    private static final String AVSENDER = "noreply-skjerming@nav.no";

    public String getNavident(final String accessToken) {
        User user = graphServiceClient(accessToken)
                .me()
                .buildRequest()
                .select("onPremisesSamAccountName")
                .get();
        return Objects.requireNonNull(user).onPremisesSamAccountName;
    }

    public void sendMail(final String content, final String recipient, final String accessToken) {
        val message = composeMessage(content, recipient);
        val graphServiceClient = graphServiceClient(accessToken);

        graphServiceClient.getLogger().setLoggingLevel(LoggerLevel.ERROR);
        graphServiceClient.me()
                .sendMail(UserSendMailParameterSet.newBuilder()
                        .withMessage(message)
                        .withSaveToSentItems(false)
                        .build())
                .buildRequest()
                .post();
    }

    private GraphServiceClient<Request> graphServiceClient(final String accessToken) {
        return GraphServiceClient.builder()
                .authenticationProvider(url -> CompletableFuture.completedFuture(accessToken))
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
