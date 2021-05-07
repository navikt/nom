package no.nav.nom.apps.api.graphql.common.loader;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.GraphQLException;
import graphql.execution.DataFetcherResult;
import graphql.kickstart.execution.GraphQLRequest;
import graphql.schema.DataFetchingEnvironment;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.graphql.common.ErrorCode;
import no.nav.nom.contract.nom.api.graphql.Person;
import no.nav.nom.contract.nom.api.graphql.RessursNavn;
import org.dataloader.BatchLoaderEnvironment;
import org.dataloader.MappedBatchLoaderWithContext;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
@RequiredArgsConstructor
public class PersonBatchLoader implements MappedBatchLoaderWithContext<String, DataFetcherResult<Person>> {
    public static String PERSON_DATALOADER = "PersonDataLoader";


    private final TokenRepository tokenRepository;
    private final AppProperties appProperties;
    private final RestTemplate restTemplate;

    @Override
    public CompletionStage<Map<String, DataFetcherResult<Person>>> load(Set<String> set, BatchLoaderEnvironment batchLoaderEnvironment) {
        return CompletableFuture.supplyAsync(() -> getNameBatch(set, batchLoaderEnvironment));
    }

    @SneakyThrows
    public Map<String, DataFetcherResult<Person>> getNameBatch(Set<String> personidenter, BatchLoaderEnvironment batchLoaderEnvironment) {
        val headers = new HttpHeaders();
        val token = tokenRepository.getAadToken(appProperties.getProxy().getAadClientId());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        headers.add("Tema", appProperties.getPdl().getTema());

        val body = new GraphQLRequest();
        body.setQuery("query($identer: [ID!]!){\n" +
                "    hentPersonBolk(identer: $identer){\n" +
                "            ident\n" +
                "            person{\n" +
                "                navn(historikk: false){\n" +
                "                    fornavn\n" +
                "                    etternavn\n" +
                "                    mellomnavn\n" +
                "                }\n" +
                "            }\n" +
                "            code\n" +
                "    }\n" +
                "}");
        body.getVariables().put("identer", personidenter);

        try {
            val result = restTemplate.exchange(appProperties.getProxy().getBaseUrl() + "/proxy/pdl", HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

            if (result.getBody().hasNonNull("data")) {
                val navnResult = result.getBody().get("data").get("hentPersonBolk");
                return StreamSupport.stream(navnResult.spliterator(), false)
                        .collect(Collectors.toUnmodifiableMap(o -> o.get("ident").asText(), o -> convert(o, (DataFetchingEnvironment) batchLoaderEnvironment.getKeyContexts().get(o.get("ident").asText()))));
            }
            throw new GraphQLException(result.getBody().get("errors").asText());

        } catch (Exception exception) {
            log.error("Feil i oppslag mot PDL", exception);
            return personidenter.stream()
                    .collect(Collectors.toUnmodifiableMap(p -> p,
                            p -> convertToError("server_error", "Oppslag mot PDL feilet", (DataFetchingEnvironment) batchLoaderEnvironment.getKeyContexts().get(p))));
        }
    }

    private DataFetcherResult<Person> convert(JsonNode personResult, DataFetchingEnvironment env) {
        val code = personResult.get("code").asText();
        if (personResult.get("code").asText().equals("ok")) {
            val navn = personResult.get("person").get("navn").get(0);
            val mellomnavn = navn.hasNonNull("mellomnavn") ? navn.get("mellomnavn").asText() : null;

            return DataFetcherResult.<Person>newResult()
                    .data(Person.builder()
                            .navn(
                                    RessursNavn.builder()
                                            .fornavn(navn.get("fornavn").asText())
                                            .mellomnavn(mellomnavn)
                                            .etternavn(navn.get("etternavn").asText())
                                            .build()
                            )
                            .build()
                    )
                    .build();
        }
        log.warn("Manglende data fra PDL", new GraphQLException("Manglende data fra PDL med kode: " + code));
        return convertToError(code, "PDL har ingen data på denne personidenten", env);
    }

    private DataFetcherResult<Person> convertToError(String code, String message, DataFetchingEnvironment env) {
        return DataFetcherResult.<Person>newResult()
                .error(ErrorCode.valueOf(code.toUpperCase()).construct(env, message))
                .build();
    }
}
