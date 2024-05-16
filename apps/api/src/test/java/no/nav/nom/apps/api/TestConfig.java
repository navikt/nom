package no.nav.nom.apps.api;

import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Import({ApiStarter.class})
class TestConfig {
    @Bean
    @Profile("!DvhRessursControllerTest-FAKES")
    @Primary
    public RessursStateRepository ressursStateRepository(@Qualifier("oldRessursStateRepository") RessursStateRepository ressursStateRepository) {
        return ressursStateRepository;
    }
    @Bean
    @Primary
    public OrgEnhetStateRepository orgEnhetStateRepository(@Qualifier("oldOrgEnhetStateRepository") OrgEnhetStateRepository orgEnhetStateRepository) {
        return orgEnhetStateRepository;
    }
    @Bean
    @Primary
    public OrgtilknytningStateRepository orgtilknytningStateRepository(@Qualifier("oldOrgtilknytningStateRepository") OrgtilknytningStateRepository orgtilknytningStateRepository) {
        return orgtilknytningStateRepository;
    }

    @Bean
    @Primary
    public OrganiseringStateRepository organiseringStateRepository(@Qualifier("oldOrganiseringStateRepository") OrganiseringStateRepository organiseringStateRepository) {
        return organiseringStateRepository;
    }
}
