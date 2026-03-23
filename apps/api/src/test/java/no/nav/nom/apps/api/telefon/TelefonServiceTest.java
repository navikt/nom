package no.nav.nom.apps.api.telefon;

import no.nav.nom.apps.api.common.TestData;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterJobbTelefonCommand;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Jobbtelefon;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static no.nav.nom.contract.nom.TelefonType.NAV_KONTOR_TELEFON;
import static no.nav.nom.contract.nom.TelefonType.NAV_TJENESTE_TELEFON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelefonServiceTest {

    private static final String FID_A = "ressurs-XXXXXXXXXXXXXXXXXXXXXXXXXX";
    private static final String NAV_IDENT_A = "A123456";
    private static final String KONTORTELEFON_A = "4777777777";
    private static final String TJENESTETELEFON_A = "4788888888";
    private static final String PRIVATTELEFON_A = "4799999999";

    private static final String NAV_IDENT_B = "B234567";
    private static final String PRIVATTELEFON_B = "4766666666";

    private static final String NAV_IDENT_X = "X123456";

    @Mock
    private RessursStateRepository ressursStateRepositoryMock;

    @Mock
    private RessursCommandService ressursCommandServiceMock;

    @InjectMocks
    private TelefonService telefonService;

    @Test
    void skal_ikke_ta_med_privattelefonnr_som_er_tom_streng() {
        var ressurser = Map.of(
            NAV_IDENT_A, ressursState(NAV_IDENT_A, PRIVATTELEFON_A),
            NAV_IDENT_B, ressursState(NAV_IDENT_B, "")
        );
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(ressurser);

        var ressurserMedTelefon = telefonService.hentTelefonnummreForAktiveRessurser();

        assertThat(ressurserMedTelefon.size()).isEqualTo(3);
    }

    @Test
    void should_combine_phone_numbers_and_return_a_map_with_lists_of_sorted_numbers() {
        var ressurser = Map.of(
                NAV_IDENT_A, ressursState(NAV_IDENT_A, PRIVATTELEFON_A),
                NAV_IDENT_B, ressursState(NAV_IDENT_B, PRIVATTELEFON_B),
                NAV_IDENT_X, TestData.ressursState()
        );

        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(ressurser);

        var sorterteTelefonnummer = telefonService.hentTelefonnummreForAktiveRessurser();


        assertThat(sorterteTelefonnummer.size()).isEqualTo(5);
        assertThat(sorterteTelefonnummer)
            .anyMatch(telefon -> telefon.navident().equals(NAV_IDENT_A) && telefon.type() == NAV_TJENESTE_TELEFON);
    }

    @Test
    void should_return_tjenestetelefon_for_single_user_which_has_all_types() {
        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A).toBuilder().jobbtelefon(new Jobbtelefon(TJENESTETELEFON_A, NAV_TJENESTE_TELEFON)).build();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_A);
        assertThat(prioritertNummer).isEqualTo(TJENESTETELEFON_A);
    }

    @Test
    void should_return_kontortelefon_for_single_user_which_has_kontortelefon_and_private_number() {

        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A).toBuilder().jobbtelefon(new Jobbtelefon(KONTORTELEFON_A, NAV_KONTOR_TELEFON)).build();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_A);
        assertThat(prioritertNummer).isEqualTo(KONTORTELEFON_A);
    }

    @Test
    void should_return_privattelefon_for_single_user_which_only_has_private_number() {
        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A).toBuilder().jobbtelefon(null).build();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_A);
        assertThat(prioritertNummer).isEqualTo(PRIVATTELEFON_A);
    }

    @Test
    void should_return_null_for_single_user_which_no_phone_numbers() {
        var ressursState = TestData.ressursState().toBuilder().jobbtelefon(null).build();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_X)).thenReturn(Optional.of(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_X);
        assertThat(prioritertNummer).isNull();
    }

    @Test
    void should_return_sorted_list_of_numbers_for_single_user() {
        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A);
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var alleTelefonnummer = telefonService.getAlleTelefonnummer(NAV_IDENT_A);
        assertThat(alleTelefonnummer.size()).isEqualTo(2);
    }

    private RessursState ressursState(String navident, String primaryTelefonnummer) {
        return TestData.ressursState()
            .toBuilder()
            .navident(navident)
            .primaryTelefonnummer(primaryTelefonnummer)
            .build();
    }

    @Test
    void should_call_ressurs_command_service() {
        Jobbtelefon telefon = new Jobbtelefon(KONTORTELEFON_A, NAV_TJENESTE_TELEFON);

        telefonService.oppdaterRessursStateJobbtelefon(FID_A, NAV_IDENT_A, telefon);
        telefonService.oppdaterRessursStateJobbtelefon(FID_A, NAV_IDENT_A, null);

        verify(ressursCommandServiceMock, times(2)).oppdaterJobbtelefonEvent(any(OppdaterJobbTelefonCommand.class), any(InputMetadata.class));
    }
}