package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

//TODO: Gjenbruker meste av koden fra graphql.LederResolver. Fjern dupliser kode fra graphql Resolvere
@Component
public class RessursLedereResolver {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrganiseringStateRepository organiseringStateRepository;

    public RessursLedereResolver(
        DvhRepository dvhRepository,
        RessursStateRepository ressursStateRepository,
        OrgenhetService orgenhetService,
        OrgtilknytningService orgtilknytningService, OrganiseringStateRepository organiseringStateRepository
    ) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.organiseringStateRepository = organiseringStateRepository;
    }


    public List<RessursLeder> ledere(String navident) {
        logger.debug("Kaller ledere navident={} ", navident);

        var aktiveKoblinger = orgtilknytningService.getByNavIdent(navident)
            .stream()
            .filter(kobling -> DateUtil.isNow(kobling.gyldigFom(), kobling.gyldigTom())).toList();

        // If there are no active koblinger, we find which date the earliest kobling will start, and select all koblinger starting that day.
        var dateForNextKobling = orgtilknytningService.getByNavIdent(navident)
            .stream()
            .map(OrgtilknytningState::gyldigFom)
            .filter(gyldigFom -> gyldigFom.isAfter(LocalDate.now()))
            .min(LocalDate::compareTo);


        var koblinger = !aktiveKoblinger.isEmpty() ? aktiveKoblinger : orgtilknytningService.getByNavIdent(navident)
            .stream()
            .filter(kobling -> kobling.gyldigFom().isEqual(dateForNextKobling.orElse(LocalDate.MIN)))
            .toList();

        return koblinger.stream()
            .flatMap(kobling -> {
                String orgenhetId = kobling.orgEnhetId();
                var orgenhet = orgenhetService.getById(orgenhetId);

                if (orgenhet.isPresent()) {
                    var ledere = lederFor(orgenhet.get().getAgressoId(), orgenhet.get().getOrgNiv(), orgenhet.get().getNavn(), navident);

                    if (ledere.isEmpty()) {
                        // Om ressurs er leder for sin enhet henter vi leder fra nivå over
                        ledere = lederForRecursive(orgenhet.get().getId(), navident);
                    }

                    return ledere.stream().map(leder -> new RessursLeder(leder, navident, kobling.erDagligOppfolging()));
                } else {
                    return Stream.empty();
                }

            })
            .collect(Collectors.toList());
    }

    List<RessursState> lederForRecursive(String orgEnhetId, String excludeNavIdent) {
        logger.debug("Kaller lederForRecursive orgEnhetId={} excludeNavIdent={}", orgEnhetId, excludeNavIdent);

        Optional<OrganiseringState> startOrganisering = organiseringStateRepository.findActiveById(orgEnhetId);
        if (startOrganisering.isPresent()) {
            Optional<IOrgenhet> orgEnhetOver = orgenhetService.getById(startOrganisering.get().idOver());
            if (orgEnhetOver.isPresent()) {
                List<RessursState> ledere = lederFor(
                    orgEnhetOver.get().getAgressoId(),
                    orgEnhetOver.get().getOrgNiv(),
                    orgEnhetOver.get().getNavn(),
                    excludeNavIdent
                );

                if (!ledere.isEmpty() || orgEnhetId.equals(orgEnhetOver.get().getId())) {
                    return ledere;
                }
                return lederForRecursive(orgEnhetOver.get().getId(), excludeNavIdent);
            }
        }
        return List.of();
    }

    private List<RessursState> lederFor(String agressoId, String orgNiv, String orgNavn, String excludeNavIdent) {
        return dvhRepository.getLederByAgressoIdAndNivAndNavn(agressoId, orgNiv, orgNavn)
            .stream()
            .map(DvhLeder::getNavIdent)
            .map(ressursStateRepository::findByNavident)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .filter(not(r -> r.navident().equals(excludeNavIdent)))
            .map(this::ressursWithObfuscatedPersonNr)
            .collect(Collectors.toList());
    }

    private RessursState ressursWithObfuscatedPersonNr(RessursState ressurs) {
        var obfuscatedPersonIdent = ressurs.personident().replaceFirst(".{5}$", "*****");
        return ressurs.toBuilder().personident(obfuscatedPersonIdent).build();
    }
}
