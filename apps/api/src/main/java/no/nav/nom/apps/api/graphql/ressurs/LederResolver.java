package no.nav.nom.apps.api.graphql.ressurs;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.graphql.MappingUtils;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.contract.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.IRessursService;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.utils.common.util.DateUtil;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.function.Predicate.not;
import static java.util.stream.Collectors.toMap;

@Component
public class LederResolver {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;
    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningStateService;

    public LederResolver(DvhRepository dvhRepository, IRessursService ressursService, OrgenhetService orgenhetService,
                         OrgtilknytningService orgtilknytningStateService) {
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
        this.orgtilknytningStateService = orgtilknytningStateService;
    }

    public <T> Map<Ressurs, List<T>> load(Collection<Ressurs> ressurser, Function<Ressurs, List<T>> loader) {
        // Load a map of resources using the specified loader function
        return ressurser.stream().collect(toMap(Function.identity(), loader));
    }

    public List<LederOrgEnhet> lederFor(Ressurs ressurs) {
        return dvhRepository.getEnheterByLeder(ressurs.getNavIdent())
                .stream()
                .map(MappingUtils::toOrgEnhet)
                .map(LederOrgEnhet::new)
                .toList();
    }

    public List<RessursLeder> ledere(Ressurs ressurs) {
        return ledere(ressurs.getNavIdent());
    }

    public List<RessursLeder> ledere(String navIdent) {
        var aktiveKoblinger = orgtilknytningStateService.getByNavIdent(navIdent)
            .stream()
            .filter(kobling -> DateUtil.isNow(kobling.gyldigFom(), kobling.gyldigTom().orElse(null))).collect(Collectors.toList());

        // If there are no active koblinger, we find which date the earlist kobling will start, and select all koblinger starting that day.
        var dateForNextKobling = orgtilknytningStateService.getByNavIdent(navIdent)
            .stream()
            .map(OrgtilknytningState::gyldigFom)
            .filter(gyldigFom -> gyldigFom.isAfter(LocalDate.now()))
            .min(LocalDate::compareTo);


        var koblinger = aktiveKoblinger.size() > 0 ? aktiveKoblinger : orgtilknytningStateService.getByNavIdent(navIdent)
            .stream()
            .filter(kobling -> kobling.gyldigFom().isEqual(dateForNextKobling.orElse(LocalDate.MIN)))
            .toList();

        return koblinger.stream()
            .flatMap(kobling -> {
                String orgenhetId = kobling.orgEnhetId();
                IOrgenhet orgenhet = orgenhetService.getById(orgenhetId).get();
                var ledere = lederFor(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), navIdent);

                if (ledere.isEmpty()) {
                    // Om ressurs er leder for sin enhet henter vi leder fra nivå over
                    ledere = lederForRecursive(orgenhet.getAgressoId(), orgenhet.getOrgNiv(), orgenhet.getNavn(), navIdent);
                }

                return ledere.stream().map(leder -> new RessursLeder(leder, kobling.erDagligOppfolging()));
            })
            .collect(Collectors.toList());
    }

    private List<Ressurs> lederForRecursive(String agressoId, String orgNiv, String orgenhetNavn, String excludeNavIdent) {
        return dvhRepository.getOrganiseringer(agressoId, orgNiv, orgenhetNavn).stream()
                .filter(o -> o.getAgressoIdUnder().equals(agressoId) && o.getOrgNivUnder().equals(orgNiv))
                .map(o -> orgenhetService.getEnhetByAgressoIdNivaAndName(o.getAgressoIdOver(), o.getOrgNivOver(), o.getNavnOver()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .map(o -> {
                    var ledere = lederFor(o.getAgressoId(), o.getOrgNiv(), excludeNavIdent);
                    if (!ledere.isEmpty() || agressoId.equals(o.getAgressoId())) {
                        return ledere;
                    }
                    return lederForRecursive(o.getAgressoId(), o.getOrgNiv(), o.getNavn(), excludeNavIdent);
                })
                .orElse(List.of());
    }

    private List<Ressurs> lederFor(String agressoId, String orgNiv, String excludeNavIdent) {
        return dvhRepository.getLederByAgressoIdAndNiv(agressoId, orgNiv)
                .stream()
                .map(DvhLeder::getNavIdent)
                .map(ressursService::findByNavident)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(not(r -> r.navident().equals(excludeNavIdent)))
                .map(MappingUtils::toRessurs)
                .collect(Collectors.toList());
    }
}
