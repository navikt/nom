package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.domain.Ressurs;
import no.nav.nom.apps.api.domain.RessursService;
import no.nav.nom.apps.api.graphql.PdlConsumer;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.Person;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RessursMutationResolver implements GraphQLMutationResolver {

    private final RessursService ressursService;
    private final PdlConsumer pdlConsumer;

    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(String personIdent, DataFetchingEnvironment env) {
        return SecurityUtil.<OpprettRessursResponse>checkAuth(env).orElseGet(() -> {
            val result = DataFetcherResult.<OpprettRessursResponse>newResult();
            try {
                val person = pdlConsumer.hentPerson(personIdent);
                val ressurs = ressursService.createOpprettetHendelse(personIdent, person.getNavn().getEtternavn().substring(0,1));
                result.data(convert(ressurs, person));
            } catch (Exception e){
                result.error(ErrorCode.SERVER_ERROR.construct(env, "Intern feil ved opprettelse av ny ressurs"));
            }
            return result.build();
        });
    }

    private OpprettRessursResponse convert(Ressurs ressurs, Person person){
        return OpprettRessursResponse.builder()
                .nomId(ressurs.getId())
                .navIdent(ressurs.getNavIdent())
                .personIdent(ressurs.getPersonIdent())
                .person(person)
                .build();
    }
}
