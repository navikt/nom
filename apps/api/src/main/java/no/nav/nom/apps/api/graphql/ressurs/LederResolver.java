package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.LederOrgEnhet;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursLeder;
import no.nav.nom.libs.utils.common.util.DateUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static no.nav.nom.libs.utils.common.util.StreamUtils.distinctByKey;

@Component
public class LederResolver {

    private final DvhRepository dvhRepository;
    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;
    private final KoblingService koblingService;

    public LederResolver(DvhRepository dvhRepository, IRessursService ressursService, IOrgenhetService orgenhetService, KoblingService koblingService) {
        this.dvhRepository = dvhRepository;
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
        this.koblingService = koblingService;
    }

    public List<LederOrgEnhet> lederFor(Ressurs ressurs) {
        return dvhRepository.getEnheterByLeder(ressurs.getNavIdent())
                .stream()
                .map(OrgEnhetResolver::convertToOrgEnhet)
                .map(LederOrgEnhet::new)
                .toList();
    }

    public List<RessursLeder> ledere(Ressurs ressurs) {
        return koblingService.getKoblingerByNavIdent(ressurs.getNavIdent())
                .stream()
                .filter(dto -> DateUtil.isNow(dto.getGyldigFom(), dto.getGyldigTom().orElse(null)))
                .filter(distinctByKey(k -> k.getAgressoId()+k.getOrgNiv()))
                .flatMap(kobling -> {
                    String agressoId = kobling.getAgressoId();
                    String orgNiv = kobling.getOrgNiv();
                    String orgenhetNavn = kobling.getOrgenhetNavn();
                    var ledere = lederFor(agressoId, orgNiv, ressurs.getNavIdent());

                    if (!ledere.isEmpty()) {
                        return ledere.stream();
                    }
                    // Om ressurs er leder for sin enhet henter vi leder fra nivå over
                    return lederForRecursive(agressoId, orgNiv, orgenhetNavn, ressurs.getNavIdent()).stream();
                })
                .map(RessursLeder::new)
                .collect(Collectors.toList());
    }

    private List<Ressurs> lederForRecursive(String agressoId, String orgNiv, String orgenhetNavn, String excludeNavIdent) {
        return dvhRepository.getOrganiseringer(agressoId, orgNiv, orgenhetNavn).stream()
                .filter(o -> o.getAgressoIdUnder().equals(agressoId) && o.getOrgNivUnder().equals(orgNiv))
                .map(o -> orgenhetService.getEnhetByAgressoIdNivaAndName(o.getAgressoIdOver(), o.getOrgNivOver(), o.getNavnOver()))
                .filter(Optional::isPresent)
                .map(Optional::get).findFirst()
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
                .map(ressursService::findByNavIdent)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(r -> !r.getNavIdent().equals(excludeNavIdent))
                .map(ressursService::convertToGraphQlResponse)
                .collect(Collectors.toList());
    }

}
