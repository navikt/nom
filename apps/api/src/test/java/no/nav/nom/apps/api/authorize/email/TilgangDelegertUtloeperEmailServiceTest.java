package no.nav.nom.apps.api.authorize.email;

import freemarker.template.Configuration;
import freemarker.template.Template;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.msgraph.EmailMessage;
import no.nav.nom.apps.api.clients.msgraph.MicrosoftGraphClient;
import no.nav.nom.apps.api.dvh.DvhStartupLoadMonitor;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.state.LederTilgangState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.authorize.DelegertLederStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;


@ActiveProfiles("test")
@ExtendWith({SpringExtension.class})
@ContextConfiguration(classes = {Configuration.class})
public class TilgangDelegertUtloeperEmailServiceTest {

    @Spy
    private final Configuration freemarkerConfig;

    @InjectMocks
    private TilgangDelegertUtloeperEmailService tilgangDelegertUtloeperEmailService;

    @Mock
    private DvhStartupLoadMonitor dvhStartupLoadMonitor;

    @Mock
    private LeaderElection leaderElection;
    @Mock
    private DelegertLederStateRepository delegertLederStateRepository;
    @Mock
    private AdClient adClient;
    @Mock
    private OrgEnhetStateRepository orgEnhetStateRepository;



    @Mock
    private MicrosoftGraphClient microsoftGraphClient;

    public TilgangDelegertUtloeperEmailServiceTest(@Autowired Configuration freemarkerConfig) throws IOException {
        this.freemarkerConfig = freemarkerConfig;
        this.freemarkerConfig.setClassForTemplateLoading(this.getClass(), "/templates/");
        Template template = freemarkerConfig.getTemplate("epostvarsel-utlopsdato.ftlh");    // <-- load the template

    }


    @Test
    public void triggeredCorrectly() {


        var allDelegations = getManyDaysLederTilgangState(LocalDate.now(), 7*8);

        var delegationPair5Weeks = LederDelegertLederPair.fromDelegeringMatchingTime(allDelegations, LocalDate.now().plusWeeks(5));
        var delegationPair2Weeks = LederDelegertLederPair.fromDelegeringMatchingTime(allDelegations, LocalDate.now().plusWeeks(2));


        when(leaderElection.isLeader()).thenReturn(true);

        var mailArgCapture = ArgumentCaptor.forClass(EmailMessage.class);
        when(microsoftGraphClient.sendEmail(mailArgCapture.capture())).thenReturn(Mono.when());


        when(delegertLederStateRepository.getAll()).thenReturn(allDelegations);
        when(adClient.fetchInAdRessursInBatches(argThat(delegationPair5Weeks::matchesContents))).thenReturn(delegationPair5Weeks.toAdRessursMap());
        when(adClient.fetchInAdRessursInBatches(argThat(delegationPair2Weeks::matchesContents))).thenReturn(delegationPair2Weeks.toAdRessursMap());
        when(orgEnhetStateRepository.findById(any())).thenAnswer(invocation -> Optional.of(OrgEnhetState.builder().navn("orgenhet-" + invocation.getArgument(0)).build()));

        tilgangDelegertUtloeperEmailService.sendEmailIfNecessary();

        verify(microsoftGraphClient, times(2)).sendEmail(any());
        verify(microsoftGraphClient, times(2)).sendEmail(any());

        var mailsSent = mailArgCapture.getAllValues();
        assertThat(mailsSent).hasSize(2);

        assertCorrectPairingInEmail(delegationPair2Weeks, mailsSent);
        assertCorrectPairingInEmail(delegationPair5Weeks, mailsSent);

    }

    private void assertCorrectPairingInEmail(LederDelegertLederPair delegationPair, List<EmailMessage> mailsSent) {
        var msgLederTmp = mailsSent.stream()
            .filter(it -> {
                var r = it.message().toRecipients().getFirst();
                var adr = r.emailAddress().address();
                return adr.contains(delegationPair.lederNavident());
            })
            .findFirst();
        assertThat(msgLederTmp).isPresent();
        assertThat( Pattern.compile(delegationPair.delegertLederNavident()).matcher(msgLederTmp.get().message().body().content()).results().count()).isEqualTo(4);
    }

    private List<LederTilgangState> getManyDaysLederTilgangState(LocalDate startDate, int dayAmount) {
        var out = new ArrayList<LederTilgangState>(dayAmount);
        for (int i = 0; i < dayAmount; i += 1) {
            out.add(new LederTilgangState(
                "fid" + i,
                "til" + i,
                "av" + i,
                "o" + i,
                startDate.minusWeeks(1),
                startDate.plusDays(i)
            ));
        }
        return out;
    }

    private record LederDelegertLederPair(
        String lederNavident,
        String delegertLederNavident
    ) {

        static LederDelegertLederPair fromDelegeringMatchingTime(List<LederTilgangState> list, LocalDate target) {
            var delegering = list.stream().filter(it -> it.gyldigTom().equals(target)).findFirst().orElseThrow();
            return new LederDelegertLederPair(delegering.av(), delegering.til());
        }

        private boolean matchesContents(Collection<String> otherNavidents) {
            return otherNavidents != null && otherNavidents.size() == 2 &&
                otherNavidents.contains(this.lederNavident) &&
                otherNavidents.contains(this.delegertLederNavident);
        }

        private Map<String, AdRessurs> toAdRessursMap() {
            return Stream.of(this.lederNavident, this.delegertLederNavident)
                .collect(Collectors.toMap(navident -> navident, this::createMockAdRessurs));
        }

        private AdRessurs createMockAdRessurs(String navident) {
            return AdRessurs.builder().navIdent(navident).email(navident + "@email.com").displayName("Navn-" + navident).build();
        }
    }

}
