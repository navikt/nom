package no.nav.nom.apps.api.ressurs.email;

import freemarker.template.Configuration;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.clients.ad.AdClient;
import no.nav.nom.apps.api.clients.email.EmailClient;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.LederResolver;
import no.nav.nom.contract.ad.AdRessurs;
import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RessursUtlopersnartEmailService {

    private static final String SLUTTDATO_UTLOPER_VARSEL_TEMPLATE = "epostvarsel-utlopsdato.ftlh";
    private static final String UNLEASH_SLUTTDATO_UTLOPER_EPOST_VARSEL = "nom.ressurs.sluttdato_utloper_email";

    public static final String NAVIDENT = "NAVident";

    private final Logger log = LoggerFactory.getLogger(RessursOpprettetEmailService.class);
    private final EmailClient emailClient;
    private final AdClient adClient;
    private final RessursStateRepository ressursStateRepository;
    private final DvhRepository dvhRepository;
    private final Configuration freemarkerConfig;
    private final Unleash unleash;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final LederResolver lederResolver;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public RessursUtlopersnartEmailService(
        EmailClient emailClient,
        AdClient adClient,
        RessursStateRepository ressursStateRepository,
        DvhRepository dvhRepository,
        Configuration freemarkerConfig,
        Unleash unleash,
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        LederResolver lederResolver,
        OrgEnhetStateRepository orgEnhetStateRepository
    ) {
        this.emailClient = emailClient;
        this.adClient = adClient;
        this.ressursStateRepository = ressursStateRepository;
        this.dvhRepository = dvhRepository;
        this.freemarkerConfig = freemarkerConfig;
        this.unleash = unleash;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.lederResolver = lederResolver;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    @Scheduled(cron = "0 0 19 * * ?") // every day at 18:00
    public void sendEmail() {
        if(!unleash.isEnabled(UNLEASH_SLUTTDATO_UTLOPER_EPOST_VARSEL)) {
            log.info("Sender en e-post om utløpsdato for ressurser til ledere av organisasjonsenheter");
            var ressurserUtloperSnart = getUtlopendeRessurser();
            if(!ressurserUtloperSnart.isEmpty()){
                ressurserUtloperSnart.forEach(this::sendEmail);
            }
        }
    }

    private void sendEmail(UtloperRessursData utlopendeRessurs) {
        Set<String> recipientNavidents = new HashSet<>(utlopendeRessurs.orgenhetData().values());
        var adRessurser = adClient.getAdRessurser(recipientNavidents);
        var recipientList = adRessurser.getRessurser().stream().map(AdRessurs::getEmail).toList();
        log.info("Sender epose varsel om utløpende ressurs: {}({}), til: {}",utlopendeRessurs.ressurNavn(), utlopendeRessurs.ressursNavident(), recipientList);


    }

    public List<UtloperRessursData> getUtlopendeRessurser() {
        return ressursStateRepository.findAllActiveRessurser().values().stream()
            .filter(ressurs -> ressurs.sluttdato().isPresent())
            .filter(ressurs -> ressurs.sluttdato().get().isEqual(LocalDate.now().plusWeeks(2)) ||
                ressurs.sluttdato().get().isEqual(LocalDate.now().plusWeeks(5)))
            .map(ressurs -> {
                var orgTilknytninger = orgtilknytningStateRepository.findByNavident(ressurs.navident());

                // innholder orgenhetNavn og orgenhetLederNavident
                Map<String, String> orgenhetData = orgTilknytninger.stream()
                    .map(orgtilknytningState -> {
                        var orgenhetState = orgEnhetStateRepository.findById(orgtilknytningState.orgEnhetId());
                        String orgenhetNavn = orgenhetState.isPresent() ? orgenhetState.get().navn() : "Ukjent";
                        var leader =  lederResolver.leder(
                            // populate at least agressoId, orgNiv, and navn
                            OrgEnhet.builder()
                                .id(orgenhetState.get().id())
                                .navn(orgenhetState.get().navn())
                                .agressoId(orgenhetState.get().agressoId().orElse(null))
                                .orgNiv(orgenhetState.get().agressoOrgNivaa().orElse(null))
                                .build()
                        );
                        if(!leader.isEmpty()){
                            return Map.entry(orgenhetNavn, leader.get(0).getRessurs().getNavIdent());
                        }
                        return Map.entry(orgenhetNavn, "Ukjent");
                    })
                    .filter(entry -> !entry.getValue().equals("Ukjent"))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

                return UtloperRessursData.builder()
                    .ressurNavn(ressurs.visningsnavn().toString())
                    .ressursNavident(ressurs.navident())
                    .ressursSluttdato(ressurs.sluttdato().get())
                    .orgenhetData(orgenhetData)
                    .build();
            }).toList();
    }


}
