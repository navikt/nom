package no.nav.nom.apps.api.graphql;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.kickstart.execution.GraphQLRequest;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.contract.nom.api.graphql.FolkeregisterPerson;
import no.nav.nom.contract.nom.api.graphql.RessursNavn;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class PdlConsumer {

    private final AppProperties appProperties;
    private final TokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    public Optional<FolkeregisterPerson> hentPerson(String personIdent){
        val headers = new HttpHeaders();
        val token = tokenRepository.getSystemAadToken(appProperties.getPdl().getAadClientId());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token.serialize());
        headers.add("Tema", appProperties.getPdl().getTema());

        val body = new GraphQLRequest();
        body.setQuery("query($ident: ID!){\n" +
                "  hentPerson(ident: $ident) {\n" +
                "       navn(historikk: false) {\n" +
                "           fornavn\n" +
                "           mellomnavn\n" +
                "           etternavn\n" +
                "    }\n" +
                "  }\n" +
                "}");
        body.getVariables().put("ident", personIdent);


            val result = restTemplate.exchange(appProperties.getPdl().getBaseUrl() + "/graphql", HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
            if (result.getBody().get("data").hasNonNull("hentPerson")) {
                val navnResult = result.getBody().get("data").get("hentPerson").get("navn").get(0);
                val mellomnavn = navnResult.hasNonNull("mellomnavn") ? navnResult.get("mellomnavn").asText() : null;
                return Optional.of(
                        FolkeregisterPerson.builder()
                        .navn(
                                RessursNavn.builder()
                                        .fornavn(navnResult.get("fornavn").asText())
                                        .mellomnavn(mellomnavn)
                                        .etternavn(navnResult.get("etternavn").asText())
                                        .build()
                        )
                        .build()
                );
            }

            return Optional.empty();
    }
}
