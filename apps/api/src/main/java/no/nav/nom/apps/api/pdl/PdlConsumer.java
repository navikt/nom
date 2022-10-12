package no.nav.nom.apps.api.pdl;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

@Service
public class PdlConsumer {

    private final AppProperties.Pdl pdlProperties;
    private final TokenRepository tokenRepository;
    private final HttpGraphQlClient graphQlClient;

    private final static String PDL_HENT_PERSON_QUERY = // language=graphql
            """
                query($ident: ID!) {
                       hentPerson(ident: $ident) {
                            navn {
                                fornavn
                                mellomnavn
                                etternavn
                            }
                            kjoenn {
                                kjoenn
                            }
                       }
                }
                """;

    @Autowired
    public PdlConsumer(
            AppProperties appProperties,
            TokenRepository tokenRepository,
            WebClient webClient
    ) {
        this.pdlProperties = appProperties.pdl();
        this.tokenRepository = tokenRepository;
        this.graphQlClient = HttpGraphQlClient.builder(webClient)
                .url(pdlProperties.baseUrl() + "/graphql")
                .headers(headers -> {
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.add("Tema", pdlProperties.tema());
                })
                .build();
    }

    public Optional<Person> hentPerson(String personIdent) {
        var response = graphQlClient.mutate()
                .headers((headers) -> headers.setBearerAuth(tokenRepository.getSystemAadToken(pdlProperties.aadClientId()).serialize()))
                .build()
                .document(PDL_HENT_PERSON_QUERY)
                .variable("ident", personIdent)
                .retrieve("hentPerson")
                .toEntity(JsonNode.class);

        return response
                .blockOptional()
                .map(person -> {
                    System.out.println("pdlresult\n" + person.asText());
                    final var navn = person.get("navn").get(0);
                    System.out.println("Person:\n" + navn.asText());
                    final var mellomnavn = navn.get("mellomnavn");
                    final var kjonn = navn.get("kjoenn").get(0).get("kjoenn").asText();

                    return Person.builder()
                            .fornavn(navn.get("fornavn").asText())
                            .mellomnavn(mellomnavn.isTextual() ? mellomnavn.asText() : null)
                            .etternavn(navn.get("etternavn").asText())
                            .kjonn(Kjonn.valueOf(kjonn))
                            .build();
                });
    }
}
