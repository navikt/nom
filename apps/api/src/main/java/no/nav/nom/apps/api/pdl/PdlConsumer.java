package no.nav.nom.apps.api.pdl;

import com.fasterxml.jackson.databind.JsonNode;
import graphql.kickstart.execution.GraphQLRequest;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.pdl.Person;
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

    public Optional<Person> hentPerson(String personIdent){
        val headers = new HttpHeaders();
        val token = tokenRepository.getSystemAadToken(appProperties.getPdl().getAadClientId());
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token.serialize());
        headers.add("Tema", appProperties.getPdl().getTema());

        val body = new GraphQLRequest();
        body.setQuery("query($ident: ID!){\n" +
                "  hentPerson(ident: $ident) {\n" +
                "       navn {\n" +
                "           fornavn\n" +
                "           mellomnavn\n" +
                "           etternavn\n" +
                "       }\n" +
                "       kjoenn {\n" +
                "           kjoenn\n" +
                "       }\n" +
                "   }\n" +
                "}");
        body.getVariables().put("ident", personIdent);


            val result = restTemplate.exchange(appProperties.getPdl().getBaseUrl() + "/graphql", HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);
        System.out.println("pldresult\n" + result.getBody().asText());
            if (result.getBody().get("data").hasNonNull("hentPerson")) {
                val person = result.getBody().get("data").get("hentPerson").get("navn").get(0);
                System.out.println("Person:\n" + person.asText());
                val mellomnavn = person.hasNonNull("mellomnavn") ? person.get("mellomnavn").asText() : null;
                //val kjonn = person.get("kjoenn").get(0).get("kjoenn").asText();

                return Optional.of(Person.builder()
                        .fornavn(person.get("fornavn").asText())
                        .mellomnavn(mellomnavn)
                        .etternavn(person.get("etternavn").asText())
                        //.kjonn(Kjonn.valueOf(kjonn))
                        .build());
            }

            return Optional.empty();
    }
}
