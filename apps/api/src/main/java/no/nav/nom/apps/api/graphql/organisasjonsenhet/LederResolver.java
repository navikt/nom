//package no.nav.nom.apps.api.graphql.organisasjonsenhet;
//
//import graphql.execution.DataFetcherResult;
//import graphql.kickstart.tools.GraphQLResolver;
//import graphql.schema.DataFetchingEnvironment;
//import lombok.RequiredArgsConstructor;
//import lombok.val;
//import no.nav.nom.apps.api.dvh.DvhRepository;
//import no.nav.nom.apps.api.ressurs.contract.IRessursService;
//import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
//import no.nav.nom.contract.nom.api.graphql.OrgEnhetsLeder;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//
//@Component
//@RequiredArgsConstructor
//public class LederResolver implements GraphQLResolver<OrgEnhet> {
//
//    private final DvhRepository dvhRepository;
//    private final IRessursService ressursService;
//
//    public DataFetcherResult<List<OrgEnhetsLeder>> leder(OrgEnhet orgEnhet, DataFetchingEnvironment env) {
//        val lederRessurser = dvhRepository.getLederByAgressoIdAndNiv(orgEnhet.getAgressoId(), orgEnhet.getOrgNiv()).stream()
//                .map(l -> OrgEnhetsLeder.builder()
//                        .ressurs(
//                                ressursService.findByNavIdent(l.getNavIdent())
//                                        .map(ressursService::convertToGraphQlResponse)
//                                        .orElseThrow()
//                        )
//                        .build()
//                )
//                .collect(Collectors.toUnmodifiableList());
//
//        return DataFetcherResult.<List<OrgEnhetsLeder>>newResult()
//                .data(lederRessurser)
//                .build();
//    }
//}
