//package no.nav.nom.apps.api.graphql.organisasjonsenhet;
//
//import graphql.execution.DataFetcherResult;
//import graphql.kickstart.tools.GraphQLResolver;
//import graphql.schema.DataFetchingEnvironment;
//import lombok.RequiredArgsConstructor;
//import lombok.val;
//import no.nav.nom.apps.api.graphql.common.ConverterUtils;
//import no.nav.nom.apps.api.orgenhet.contract.IOrgenhetService;
//import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
//import no.nav.nom.contract.nom.api.graphql.Organisering;
//import org.springframework.stereotype.Component;
//
//
//@Component
//@RequiredArgsConstructor
//public class OrganiseringToOrgEnhetResolver implements GraphQLResolver<Organisering> {
//    private final IOrgenhetService orgenhetService;
//
//    public DataFetcherResult<OrgEnhet> orgEnhet(Organisering organisering, DataFetchingEnvironment env) {
//        val orgEnhet = orgenhetService.getEnhetByAgressoIdNivaAndName(organisering.getOrgEnhetNr(), organisering.getOrgNiv(), organisering.getOrgEnhetNavn())
//                .map(ConverterUtils::convertToGraphQlResponse)
//                .orElseThrow();
//
//        return DataFetcherResult.<OrgEnhet>newResult()
//                .data(orgEnhet)
//                .build();
//    }
//
//    public DataFetcherResult<OrgEnhet> organisasjonsenhet(Organisering organisering, DataFetchingEnvironment env) {
//        return orgEnhet(organisering, env);
//    }
//}
