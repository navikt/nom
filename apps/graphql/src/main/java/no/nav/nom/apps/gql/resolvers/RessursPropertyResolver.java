package no.nav.nom.apps.gql.resolvers;

import graphql.ErrorClassification;
import graphql.GraphQLError;
import graphql.execution.DataFetcherResult;
import graphql.schema.DataFetchingEnvironment;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RessursPropertyResolver {


    private static final Logger log = LoggerFactory.getLogger(RessursPropertyResolver.class);

    private EmailFetcher emailFetcher = null;

    public RessursPropertyResolver setEmailFetcher(EmailFetcher emailFetcher) {
        this.emailFetcher = emailFetcher;
        return this;
    }


    public Map<Ressurs, DataFetcherResult<String>> epost(List<Ressurs> ressurser, DataFetchingEnvironment dataFetchingEnvironment) {
        var out = new HashMap<Ressurs, DataFetcherResult<String>>(ressurser.size());
        if(emailFetcher == null){
            for(var ressurs : ressurser){
                // todo: selve feilmeldingen vises ikke i GraphQl responsen
                var errorItem = GraphQLError.newError()
                    .location(dataFetchingEnvironment.getSource())
                    .message("Kunne ikke hente epost siden `EmailFetcher` ikke er konfigurert")
                    .build();
                out.put(ressurs, DataFetcherResult.<String>newResult().error(errorItem).build());
            }
            return out;
        }

        var emails = emailFetcher.emailByNavident(ressurser.stream().map(Ressurs::getNavident).toList());

        for(var r : ressurser){
            var emailResult = emails.get(r.getNavident());
            if(emailResult.hasErrors()){
                // todo, sjekk om riktig måte å logge på
                log.error(emailResult.getErrors().stream().map(Object::toString).collect(Collectors.joining("\n")));
                out.put(r, DataFetcherResult.<String>newResult().error(GraphQLError.newError().message("Couldn't fetch email for ident " + r.getNavident()).build()).build());
            }else{
                out.put(r, emailResult);
            }
        }

        return out;
    }


    public interface EmailFetcher {
        Map<String,DataFetcherResult<String>> emailByNavident(List<String> navIdentList);
    }

}
