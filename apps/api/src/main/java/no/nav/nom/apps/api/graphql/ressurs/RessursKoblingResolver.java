package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.graphql.common.ConverterUtils;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.RessursKobling;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class RessursKoblingResolver implements GraphQLResolver<Ressurs>{

    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<RessursKobling>> koblinger(Ressurs ressurs, DataFetchingEnvironment env) {
        final List<RessursKobling> result = dvhRepository.getKoblingerForRessurs(ressurs.getNavIdent().substring(1))
                .stream()
                .map(dvh -> {
                    val orgenhet = dvhRepository.findEnhetByNummer(dvh.getAgressoId()).get();
                    return RessursKobling.builder()
                                    .organisasjonsenhet(ConverterUtils.convert(orgenhet))
                                    .gyldigFom(dvh.getGyldigFom())
                                    .gyldigTom(dvh.getGyldigTom())
                                    .build();
                })
                .collect(Collectors.toUnmodifiableList());

        return DataFetcherResult.<List<RessursKobling>>newResult()
                .data(result)
                .build();
    }

}
