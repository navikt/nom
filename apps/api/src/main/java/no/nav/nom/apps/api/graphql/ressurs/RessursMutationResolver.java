package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.graphql.PdlConsumer;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.apps.api.ressurs.contract.RessursService;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.Person;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RessursMutationResolver implements GraphQLMutationResolver {

    private final RessursService ressursService;
    private final PdlConsumer pdlConsumer;

    @Value("${app.graphql.preauth.mutation.ressurs}")
    private List<String> preauth;

    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(String personIdent, Optional<String> forbokstav, DataFetchingEnvironment env) {
        return SecurityUtil.<OpprettRessursResponse>checkAuth(env, preauth).orElseGet(() -> {
            val result = DataFetcherResult.<OpprettRessursResponse>newResult();
            try {
                val person = pdlConsumer.hentPerson(personIdent);

                val ressurs = ressursService.findByPersonIdent(personIdent).orElseGet(() -> {
                            val navIdentForbokstav = (forbokstav.isPresent() ? forbokstav.filter(c -> c.length() == 1).orElseThrow(() -> new IllegalArgumentException("Ugyldig input i forbokstav: " + forbokstav.get())) :
                                    person.orElseThrow(() -> new NoSuchElementException("Ingen resultat fra PDL med gitt personident")).getNavn().getEtternavn().substring(0, 1)).toUpperCase();

                            return ressursService.opprettRessurs(personIdent, null, navIdentForbokstav);
                        });

                if(person.isEmpty()) {
                    result.error(ErrorCode.NOT_FOUND.construct(env, "Oppslag mot PDL feilet"));
                }
                result.data(convert(ressurs, person.orElse(null)));
            } catch (IllegalArgumentException|NoSuchElementException e){
                log.warn(e.getMessage(), e);
                result.error(ErrorCode.BAD_REQUEST.construct(env, e.getMessage()));
            } catch (Exception e){
                log.error("Intern feil ved opprettelse av ny ressurs", e);
                result.error(ErrorCode.SERVER_ERROR.construct(env, "Intern feil ved opprettelse av ny ressurs"));
            }
            return result.build();
        });
    }

    private OpprettRessursResponse convert(RessursI ressurs, Person person){
        return OpprettRessursResponse.builder()
                .nomId(ressurs.getId())
                .navIdent(ressurs.getNavIdent())
                .personIdent(ressurs.getPersonIdent())
                .person(person)
                .build();
    }
}
