package no.nav.nom.apps.api.graphql.hentenhet;

import graphql.kickstart.tools.GraphQLQueryResolver;
import lombok.Value;
import org.springframework.stereotype.Component;

@Component
class HentEnhetResolver implements GraphQLQueryResolver {

    public EnhetTo hentEnhet(String id) {
        return new EnhetTo("derp", "0101");
    }

    @Value
    public static class EnhetTo {
        String navn;
        String nummer;
    }
}
