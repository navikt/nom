package no.nav.nom.apps.api.graphql.controller;

import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.apps.api.graphql.organisasjonsenhet.OrgEnhetResolver;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
//import org.apache.catalina.security.SecurityUtil;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.stereotype.Controller;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Controller
public class GraphQLSearchController {

    private final IRessursService ressursService;
    private final IOrgenhetService orgenhetService;
//    private final SecurityUtil securityUtil;

    public GraphQLSearchController(IRessursService ressursService, IOrgenhetService orgenhetService) {
        this.ressursService = ressursService;
        this.orgenhetService = orgenhetService;
    }

    @QueryMapping
    public List<Object> search(@Argument String term) {
//        return securityUtil.<List<Object>>checkAuth(env, null).orElseGet(() -> {
        final var matchedOrgenhetIds = Stream.concat(
                        orgenhetService.matchByAgressoId(term).stream(),
                        orgenhetService.matchByNavn(term).stream()
                )
                .map(IOrgenhet::getAgressoId)
                .collect(Collectors.toSet());

        return Stream.concat(
                orgenhetService.findEnheterByAgressoIdIn(matchedOrgenhetIds).stream().map(OrgEnhetResolver::convertToOrgEnhet),
                ressursService.matchByNavident(term).stream().map(ressursService::convertToGraphQlResponse)
        ).toList();
//        });
    }

}
