package no.nav.nom.apps.gql.resolvers;

import no.nav.nom.contract.nom.api.graphql.OrgEnhet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class RemedyEnhetIdResolver {

    private final static Logger log = LoggerFactory.getLogger(RemedyEnhetIdResolver.class);

    private RemedyEnhetIdFetcher remedyEnhetIdFetcher = null;

    public String remedyEnhetId(OrgEnhet orgEnhet) {
        if(remedyEnhetIdFetcher == null) {
            log.info("Cannot provide remedy ID for orgenhet with ID " + orgEnhet.getRemedyEnhetId());
            return null;
        }
        return this.remedyEnhetIdFetcher.remedyEnhetId(orgEnhet.getId());
    }

    public void setRemedyEnhetIdFetcher(RemedyEnhetIdFetcher remedyEnhetIdFetcher) {
        this.remedyEnhetIdFetcher = remedyEnhetIdFetcher;
    }

    public interface RemedyEnhetIdFetcher{
        String remedyEnhetId(String nomOrgenhetId);
    }
}
