package no.nav.nom.skjermede.personer.skjerming;

import no.nav.nom.contract.nom.dto.RessursDTO;
import no.nav.nom.contract.nom.dto.Ressurstype;
import no.nav.nom.skjermede.personer.skjerming.kafka.listener.NomRessurserListener;
import no.nav.nom.skjermede.personer.skjerming.kafka.listener.SkjermingLogikkKafka;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(SpringExtension.class)
class NomRessurserListenerTest {

    @InjectMocks
    private NomRessurserListener nomRessurserListener;

    @Mock
    private SkjermingLogikkKafka skjermingLogikkKafka;

    private RessursDTO ressurs;

    @BeforeEach
    void testDataSetup(){
        ressurs = RessursDTO.builder()
                .navident("T123456")
                .build();
    }

    @Test
    void onMessageIntern() {
        ressurs.setRessurstype(Ressurstype.INTERN);
        nomRessurserListener.onMessage(ressurs);

        verify(skjermingLogikkKafka).handterAnsattHendelse(NomRessurserListener.converted(ressurs), ressurs.getNavident());
    }

    @Test
    void onMessageEkstern() {
        ressurs.setRessurstype(Ressurstype.EKSTERN);
        nomRessurserListener.onMessage(ressurs);

        verify(skjermingLogikkKafka, never()).handterAnsattHendelse(NomRessurserListener.converted(ressurs), ressurs.getNavident());
    }
}