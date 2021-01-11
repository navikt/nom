package no.nav.nom.apps.dvh.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import no.nav.nom.apps.dvh.TestBase;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.ConsumerFactory;

abstract class StateTestBase extends TestBase {

    @Autowired
    StateLoader loader;

    @Autowired
    ConsumerFactory<String, String> consumerFactory;

    @Autowired
    ObjectMapper mapper;
}
