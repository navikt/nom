package no.nav.nom.apps.api.ressurs.api.app;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
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
import static no.nav.nom.apps.api.organisering.OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE;

//TODO: Gjenbruker meste av koden fra graphql.LederResolver. Fjern dupliser kode fra graphql Resolvere
@Component
public class RessursLedereResolver {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DvhRepository dvhRepository;
    private final RessursStateRepository ressursStateRepository;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final Unleash unleash;

    public RessursLedereResolver(
            DvhRepository dvhRepository,
            RessursStateRepository ressursStateRepository,
            OrgenhetService orgenhetService,
            OrgtilknytningService orgtilknytningService, OrganiseringStateRepository organiseringStateRepository, Unleash unleash
    ) {
        this.dvhRepository = dvhRepository;
        this.ressursStateRepository = ressursStateRepository;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.organiseringStateRepository = organiseringStateRepository;
        this.unleash = unleash;
    }


    public List<RessursLeder> ledere(String navident) {
        logger.debug("Kaller ledere navident={} ", navident);

        var aktiveKoblinger = orgtilknytningService.getByNavIdent(navident)
            .stream()
            .filter(kobling -> DateUtil.isNow(kobling.gyldigFom(), kobling.gyldigTom().orElse(null))).collect(Collectors.toList());

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
                        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
                            ledere = lederForRecursiveUsingNewOrganisering(orgenhet.get().getId(), navident);
                        } else {
                            ledere = lederForRecursive(orgenhet.get().getAgressoId(), orgenhet.get().getOrgNiv(), orgenhet.get().getNavn(), navident);
                        }
                    }

                    return ledere.stream().map(leder -> new RessursLeder(leder, navident, kobling.erDagligOppfolging()));
                } else {
                    return Stream.empty();
                }

            })
            .collect(Collectors.toList());
    }

    private List<RessursState> lederForRecursive(String agressoId, String orgNiv, String orgenhetNavn, String excludeNavIdent) {
        logger.debug("Kaller lederForRecursive agressoId={} orgNiv={} navn={} excludeNavIdent={}", agressoId, orgNiv, orgenhetNavn, excludeNavIdent);

        return dvhRepository.getOrganiseringer(agressoId, orgNiv, orgenhetNavn).stream()
            .filter(o -> o.getAgressoIdUnder().equals(agressoId) && o.getOrgNivUnder().equals(orgNiv))
            .map(o -> orgenhetService.getEnhetByAgressoIdNivaAndName(o.getAgressoIdOver(), o.getOrgNivOver(), o.getNavnOver()))
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst()
            .map(o -> {
                var ledere = lederFor(o.getAgressoId(), o.getOrgNiv(), o.getNavn(), excludeNavIdent);
                if (!ledere.isEmpty() || agressoId.equals(o.getAgressoId())) {
                    return ledere;
                }
                return lederForRecursive(o.getAgressoId(), o.getOrgNiv(), o.getNavn(), excludeNavIdent);
            })
            .orElse(List.of());
    }

    List<RessursState> lederForRecursiveUsingNewOrganisering(String orgEnhetId, String excludeNavIdent) {
        logger.debug("Kaller lederForRecursiveUsingNewOrganisering orgEnhetId={} excludeNavIdent={}", orgEnhetId, excludeNavIdent);

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
                return lederForRecursiveUsingNewOrganisering(orgEnhetOver.get().getId(), excludeNavIdent);
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
            .collect(Collectors.toList());
    }

}
