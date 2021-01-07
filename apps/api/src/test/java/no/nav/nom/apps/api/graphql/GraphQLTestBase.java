package no.nav.nom.apps.api.graphql;

import com.graphql.spring.boot.test.GraphQLTestTemplate;
import no.nav.nom.apps.api.KafkaUtils;
import no.nav.nom.apps.api.TestBase;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.contract.dvh.Enhet;
import no.nav.nom.contract.dvh.Ressurs;
import no.nav.nom.contract.dvh.Stilling;
import no.nav.nom.contract.dvh.StillingTabell;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;

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

    protected Enhet enhetTestTemplate(String suffix, String overEnhetKode, int fraTilIncrement, String lederRessursId){
        return Enhet.builder()
                .enhetKode("testId"+suffix)
                .enhetNavn("testNavn"+suffix)
                .enhetType("NAVENHET")
                .enhetGyldigFra(LocalDate.now().minusMonths(fraTilIncrement))
                .enhetGyldigTil(LocalDate.now().plusMonths(fraTilIncrement))
                .overEnhetKode(overEnhetKode != null ? overEnhetKode : "testId"+"Over"+suffix)
                .orgGyldigFra(LocalDate.now().minusMonths(fraTilIncrement))
                .orgGyldigTil(LocalDate.now().plusMonths(fraTilIncrement))
                .lederRessursId(lederRessursId)
                .build();
    }

    protected Ressurs ressursTestTemplate(String suffix){
        return Ressurs.builder()
                .navident("testNavIdent"+suffix)
                .personIdent("testPersonIdent"+suffix)
                .build();
    }

    protected Stilling stillingTestTemplate(String agressoNr, String navIdent, int fraTilIncrement) {
        return Stilling.builder()
                .orgEnhet(agressoNr)
                .resourceId(navIdent.substring(1))
                .fraDato(LocalDate.now().minusDays(fraTilIncrement))
                .tilDato(LocalDate.now().plusDays(fraTilIncrement))
                .ansettelsesTypeBesk("dummy")
                .stillingIBunn(1.0)
                .timestamp(LocalDateTime.now())
                .orgEnhetBesk("dummy")
                .permisjon("JOBBER")
                .build();
    }

    protected StillingTabell stillingTabellTestTemplate(Stilling... stillinger) {
        return StillingTabell.builder().elementer(Arrays.asList(stillinger.clone())).build();
    }

}
