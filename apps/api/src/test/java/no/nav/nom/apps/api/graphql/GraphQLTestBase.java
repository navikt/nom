package no.nav.nom.apps.api.graphql;

import com.graphql.spring.boot.test.GraphQLTestTemplate;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.dvh.Ressurs;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;

public abstract class GraphQLTestBase extends TestBase {

    @Autowired
    protected GraphQLTestTemplate graphQLTestTemplate;

    @Autowired
    private DvhRepository dvhRepository;

    @Autowired
    protected KafkaUtils kafkaUtils;

    @AfterEach
    protected void clearRepository(){
        dvhRepository.clearRepository();
    }

    protected Enhet enhetTestTemplate(String suffix, String overEnhetKode, int fraTilIncrement){
        return Enhet.builder()
                .enhetKode("testId"+suffix)
                .enhetNavn("testNavn"+suffix)
                .enhetType("NAVENHET")
                .enhetGyldigFra(LocalDate.now().minusMonths(fraTilIncrement))
                .enhetGyldigTil(LocalDate.now().plusMonths(fraTilIncrement))
                .overEnhetKode(overEnhetKode != null ? overEnhetKode : "testId"+"Over"+suffix)
                .orgGyldigFra(LocalDate.now().minusMonths(fraTilIncrement))
                .orgGyldigTil(LocalDate.now().plusMonths(fraTilIncrement))
                .build();
    }

    protected Ressurs ressursTestTemplate(String suffix){
        return Ressurs.builder()
                .navident("testNavIdent"+suffix)
                .personIdent("testPersonIdent"+suffix)
                .build();
    }
}
