package no.nav.nom.apps.api.pdl;

import com.fasterxml.jackson.databind.JsonNode;
import io.getunleash.Unleash;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.OnBehalfOfTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.graphql.GraphQlResponse;
import org.springframework.graphql.client.HttpGraphQlClient;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PdlClient {

    private final Unleash unleash;
    public static final String UNLEASH_USE_ONBEHALF_OF_TOKEN_IN_PDLCLIENT = "nom.pdlclient.useOnBehalfOfToken";

    private static final Logger logger = LoggerFactory.getLogger(PdlClient.class);
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
                foedsel {
                   foedselsdato
                   foedselsaar
                }
           }
        }
        """;
    private final HttpGraphQlClient graphQlClient;
    private final OnBehalfOfTokenRepository onBehalfOfTokenRepository;

    private final String pdlScope;

    @Autowired
    public PdlClient(
        AppProperties appProperties,
        OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
        OnBehalfOfTokenRepository onBehalfOfTokenRepository,
        Unleash unleash
    ) {
        final var pdlProperties = appProperties.clients().pdl();

        ServletOAuth2AuthorizedClientExchangeFilterFunction oAuth2Filter = new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oAuth2Filter.setDefaultClientRegistrationId("pdl");
        this.graphQlClient = HttpGraphQlClient.builder(WebClient.builder().filter(oAuth2Filter))
            .url(pdlProperties.baseUrl() + "/graphql")
            .headers(headers -> {
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.add("Tema", pdlProperties.tema());
                headers.add("behandlingsnummer", pdlProperties.behandlingsnummer());
            })
            .build();

        this.onBehalfOfTokenRepository = onBehalfOfTokenRepository;
        this.pdlScope = pdlProperties.aadScope();
        this.unleash = unleash;
    }

    public Optional<Person> hentPerson(String personIdent) {
        if (unleash.isEnabled(UNLEASH_USE_ONBEHALF_OF_TOKEN_IN_PDLCLIENT)){
            String authToken = onBehalfOfTokenRepository.getTokenFromSpringAuthenticationContext();
            if(authToken == null) {
                logger.error("Fikk ingen auth token fra tokenRepository. Skipper oppslag mot PDL");
                return Optional.empty();
            }

            // if the context token is for a user principal, we need to get the onBehalfOfToken for the user
            if(onBehalfOfTokenRepository.isContextTokenForAnUserPrincipal(authToken)) {
                String onBehalfOfToken = onBehalfOfTokenRepository.getOnbehalfofTokenFor(pdlScope);
                if(onBehalfOfToken == null) {
                    logger.error("Fikk ingen onBehalfOfToken fra tokenRepository. Skipper oppslag mot PDL");
                    return Optional.empty();
                } else {
                    // use a graphql client with the onBehalfOfToken
                    var mutatedGraphqlClientWithOnBehalfOfToken = graphQlClient.mutate().headers(headers -> headers.setBearerAuth(onBehalfOfToken)).build();
                    return hentPerson(mutatedGraphqlClientWithOnBehalfOfToken, personIdent);
                }
            }
            // if the context token is not for a user principal, we can use the service token directly from oAuth2AuthorizedClientManager
        }
        return hentPerson(graphQlClient, personIdent);
    }

    private Optional<Person> hentPerson(HttpGraphQlClient graphQlClient, String personIdent) {
        return graphQlClient.document(PDL_HENT_PERSON_QUERY)
            .variable("ident", personIdent)
            .execute()
            .filter(GraphQlResponse::isValid)
            .mapNotNull(res -> {
                if (!res.getErrors().isEmpty()) {
                    logger.warn("Error in lookup from PDL: {}", res.getErrors());
                    return null;
                }
                return res.field("hentPerson").toEntity(JsonNode.class);
            })
            .onErrorResume(throwable -> {
                logger.warn("Error in lookup from PDL", throwable);
                return Mono.empty();
            })
            .blockOptional()
            .map(this::convertToApiPerson);
    }

    private Person convertToApiPerson(JsonNode person) {
        final var navn = person.get("navn")
            .get(0);
        final var mellomnavn = navn.get("mellomnavn");
        final var kjonn = person.get("kjoenn")
            .get(0)
            .get("kjoenn")
            .asText();

        final var jsonFoedsel = person.get("foedsel").get(0);

        // foedselsdato might be missing, foedselsaar is always present.
        LocalDate foedselsdato = jsonFoedsel.hasNonNull("foedselsdato") ? LocalDate.parse(jsonFoedsel.get("foedselsdato").asText()) :
            LocalDate.of(jsonFoedsel.get("foedselsaar").asInt(), 1, 1);


        return Person.builder()
            .fornavn(navn.get("fornavn")
                .asText())
            .mellomnavn(mellomnavn.isTextual() ? mellomnavn.asText() : null)
            .etternavn(navn.get("etternavn")
                .asText())
            .kjonn(Kjonn.valueOf(kjonn))
            .foedselsdato(foedselsdato)
            .build();
    }
}
