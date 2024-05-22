package no.nav.nom.apps.admin;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.NomStateRessursListener;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;

@Slf4j
@Configuration
@EnableKafka
public class Test {
    private final RessursStateRepository ressursStateRepository;
    private final NomStateRessursListener nomStateRessursListener;

    public Test(RessursStateRepository ressursStateRepository, NomStateRessursListener nomStateRessursListener) throws InterruptedException {
        this.ressursStateRepository = ressursStateRepository;
        this.nomStateRessursListener = nomStateRessursListener;

        nomStateRessursListener.register(getVisitor("Test visitor 1"));
        nomStateRessursListener.register(getVisitor("Test visitor 2"));
        nomStateRessursListener.register(getVisitor("Test visitor 3"));
    }

    private NomStateRessursListener.Visitor getVisitor(String name) {
        return new NomStateRessursListener.Visitor<RessursState>() {
            int count = 0;

            @Override
            public String visitorName() {
                return name;
            }

            @Override
            public void visit(String key, RessursState ressursState) {
                count++;
                if (count % 10000 == 0)
                    log.info("Visitor {} visited with ressursState nr {}: {}", name, count, ressursState);
                else
                    log.trace("Visited ressursState: " + ressursState);
            }
        };
    }
}
