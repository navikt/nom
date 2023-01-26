package no.nav.nom.apps.api.graphql.ressurs;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.orgTilknytning.contract.Orgtilknytning;
import no.nav.nom.apps.api.orgTilknytning.contract.OrgtilknytningService;
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

    private final OrgtilknytningService orgtilknytningService;
    private final IOrgenhetService orgenhetService;

    @Autowired
    public OrgTilknytningResolver(OrgtilknytningService orgtilknytningService, IOrgenhetService orgenhetService) {
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
    }

    public Map<Ressurs, List<RessursOrgTilknytning>> getOrgTilknytning(Collection<Ressurs> ressurser) {
        var navidenter = ressurser.stream().map(Ressurs::getNavIdent).collect(toSet());
        var koblingerContext = orgtilknytningService.getKoblingerByNavidenter(navidenter, it -> it).values().stream().flatMap(Collection::stream).collect(toSet());
        var agressoIdSet = koblingerContext.stream().map(kob -> new IOrgenhet.AgressoIdCombo(kob.getAgressoId(), kob.getOrgNiv())).collect(toSet());
        var orgEnheterContext = orgenhetService.findEnheterByAgressoIdConcatOrgnivIn(agressoIdSet);

        return ressurser.stream()
                .collect(toMap(
                        Function.identity(),
                        ressurs -> orgtilknytningService.getKoblingerByNavIdent(ressurs.getNavIdent())
                                .stream()
                                .map(kobling -> this.fromDvhKobling(kobling, orgEnheterContext))
                                .toList()
                ));
    }

    private RessursOrgTilknytning fromDvhKobling(Orgtilknytning orgtilknytning, List<IOrgenhet> orgenhetContext) {
        final var orgenhet = orgenhetContext.stream()
                .filter(oe -> oe.getOrgNiv().equals(orgtilknytning.getOrgNiv()) && oe.getAgressoId().equals(orgtilknytning.getAgressoId()))
                .findFirst()
                .get();
        final var orgEnhetGqlResponse = OrgEnhetResolver.convertToOrgEnhet(orgenhet);

        return RessursOrgTilknytning.builder()
                .organisasjonsenhet(orgEnhetGqlResponse)
                .orgEnhet(orgEnhetGqlResponse)
                .gyldigFom(orgtilknytning.getGyldigFom())
                .gyldigTom(orgtilknytning.getGyldigTom().orElse(null))
                .build();
    }
}
