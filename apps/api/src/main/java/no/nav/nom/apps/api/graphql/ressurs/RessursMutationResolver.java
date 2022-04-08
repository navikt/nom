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
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursRequest2;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse;
import no.nav.nom.contract.nom.api.graphql.OpprettRessursResponse2;
import no.nav.nom.contract.nom.api.graphql.Person;
import no.nav.nom.contract.nom.api.graphql.Ressurstype;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RessursMutationResolver implements GraphQLMutationResolver {

    private final IRessursService ressursService;
    private final PdlConsumer pdlConsumer;
    private final SecurityUtil securityUtil;

    @Value("${app.graphql.preauth.mutation.ressurs}")
    private List<String> preauth;

    public DataFetcherResult<OpprettRessursResponse> opprettRessurs(String personIdent, Optional<String> forbokstav, DataFetchingEnvironment env) {
        return securityUtil.<OpprettRessursResponse>checkAuth(env, preauth).orElseGet(() -> {
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

    public DataFetcherResult<OpprettRessursResponse2> opprettRessurs2(OpprettRessursRequest2 request, DataFetchingEnvironment env) {
        return securityUtil.<OpprettRessursResponse2>checkAuth(env, preauth).orElseGet(() -> {
            val result = DataFetcherResult.<OpprettRessursResponse2>newResult();
            val person = pdlConsumer.hentPerson(request.getPersonident());
            val ressurs = ressursService.findByPersonIdent(request.getPersonident());
            if (person.isEmpty()) {
                result.error(ErrorCode.NOT_FOUND.construct(env, "Oppslag mot PDL feilet"));
            }else{
                if(ressurs.isEmpty()){
                    result.data(OpprettRessursResponse2.builder()
                                    .person(person.get())
                                    .navident("Z999999")
                                    .personident(request.getPersonident())
                                    .fornavn(request.getFornavn())
                                    .etternavn(request.getEtternavn())
                            .build());
                    return result.build();
                }
                val existingRessurs = ressurs.get();
                result.data(OpprettRessursResponse2.builder()
                                .navident(existingRessurs.getNavIdent())
                                .fornavn("Vidar")
                                .etternavn("Visningsen")
                                .personident(existingRessurs.getPersonIdent())
                        .build());
                result.build();
                return result.build();
            }
            return result.error(ErrorCode.SERVER_ERROR.construct(env,"Server error")).build();
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
