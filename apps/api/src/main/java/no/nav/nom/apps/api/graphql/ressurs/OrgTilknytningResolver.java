package no.nav.nom.apps.api.graphql.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.kobling.contract.Kobling;
import no.nav.nom.apps.api.kobling.contract.KoblingService;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.IOrgenhetService;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursOrgTilknytning;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;

@Slf4j
@Component

public class OrgTilknytningResolver {

    private final KoblingService koblingService;
    private final IOrgenhetService orgenhetService;

    @Autowired
    public OrgTilknytningResolver(KoblingService koblingService, IOrgenhetService orgenhetService) {
        this.koblingService = koblingService;
        this.orgenhetService = orgenhetService;
    }

    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytning(Collection<Ressurs> ressurser) {
        var navidenter = ressurser.stream().map(Ressurs::getNavIdent).collect(toSet());
        var koblingerContext = koblingService.getKoblingerByNavidenter(navidenter, it -> it).values().stream().flatMap(Collection::stream).collect(toSet());
        var agressoIdSet = koblingerContext.stream().map(kob -> new IOrgenhet.AgressoIdCombo(kob.getAgressoId(), kob.getOrgNiv())).collect(toSet());
        var orgEnheterContext = orgenhetService.findEnheterByAgressoIdConcatOrgnivIn(agressoIdSet);

        return ressurser.stream()
                .collect(toMap(
                        Function.identity(),
                        ressurs -> koblingService.getKoblingerByNavIdent(ressurs.getNavIdent())
                                .stream()
                                .map(kobling -> this.fromDvhKobling(kobling, orgEnheterContext))
                                .toList()
                ));
    }

    private RessursOrgTilknytning fromDvhKobling(Kobling kobling, List<IOrgenhet> orgenhetContext) {
        final var orgenhet = orgenhetContext.stream()
                .filter(oe -> oe.getOrgNiv().equals(kobling.getOrgNiv()) && oe.getAgressoId().equals(kobling.getAgressoId()))
                .findFirst()
                .get();
        final var orgEnhetGqlResponse = OrgEnhetResolver.convertToOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
                .organisasjonsenhet(orgEnhetGqlResponse)
                .orgEnhet(orgEnhetGqlResponse)
                .gyldigFom(kobling.getGyldigFom())
                .gyldigTom(kobling.getGyldigTom().orElse(null))
                .build();
    }
}
