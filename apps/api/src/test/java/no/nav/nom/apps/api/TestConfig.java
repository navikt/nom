package no.nav.nom.apps.api;

import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

@Import({ApiStarter.class})
class TestConfig {
    @Bean
    @Primary
    public OrgtilknytningStateRepository orgtilknytningStateRepository(@Qualifier("oldOrgtilknytningStateRepository") OrgtilknytningStateRepository orgtilknytningStateRepository) {
        return orgtilknytningStateRepository;
    }
}
