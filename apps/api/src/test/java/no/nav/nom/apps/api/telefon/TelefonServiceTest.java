package no.nav.nom.apps.api.telefon;

import no.nav.nom.apps.api.common.TestData;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.RessursCommandService;
import no.nav.nom.apps.api.ressurs.eventsourcing.producer.commands.OppdaterJobbTelefonCommand;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.apps.api.dvh.dto.DvhTelefon.DvhTelefonType.KONTORTELEFON;
import static no.nav.nom.apps.api.dvh.dto.DvhTelefon.DvhTelefonType.TJENESTETELEFON;
import static no.nav.nom.contract.nom.TelefonType.NAV_TJENESTE_TELEFON;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TelefonServiceTest {

    private static final String NAV_IDENT_A = "A123456";
    private static final String KONTORTELEFON_A = "4777777777";
    private static final String TJENESTETELEFON_A = "4788888888";
    private static final String PRIVATTELEFON_A = "4799999999";

    private static final String NAV_IDENT_B = "B234567";
    private static final String PRIVATTELEFON_B = "4766666666";

    private static final String NAV_IDENT_X = "X123456";
    private static final String TJENESTETELEFON_X = "4711111111";
    
    @Mock
    private DvhRepository dvhRepositoryMock;

    @Mock
    private RessursStateRepository ressursStateRepositoryMock;

    @Mock
    private RessursCommandService ressursCommandServiceMock;

    @InjectMocks
    private TelefonService telefonService;
    

    @Test
    void skal_returnere_liste_av_telefonobjekter_for_ressurser_som_har_privattelefon() {
        var ressurser = Map.of(
                NAV_IDENT_A, ressursState(NAV_IDENT_A, PRIVATTELEFON_A),
                NAV_IDENT_B, ressursState(NAV_IDENT_B, PRIVATTELEFON_B),
                NAV_IDENT_X, TestData.ressursState()
        );
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(ressurser);

        var ressurserMedTelefon = telefonService.fetchPrivattelefoner();

        assertThat(ressurserMedTelefon.size()).isEqualTo(2);
        var telefon = ressurserMedTelefon.getFirst();
        assertThat(telefon).isInstanceOf(Telefon.class);
        assertThat(telefon.type()).isEqualTo(TelefonType.PRIVAT_TELEFON);
    }

    @Test
    void should_combine_phone_numbers_and_return_a_map_with_lists_of_sorted_numbers() {
        var telefoner = List.of(
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(KONTORTELEFON_A).type(KONTORTELEFON).beskrivelse("Bla, bla").build(),
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(TJENESTETELEFON_A).type(TJENESTETELEFON).beskrivelse("Bla, bla").build(),
                DvhTelefon.builder().navIdent(NAV_IDENT_X).nummer(TJENESTETELEFON_X).type(TJENESTETELEFON).beskrivelse("Bla, bla").build()
        );
        when(dvhRepositoryMock.getNavidentTelefonNrMap()).thenReturn(telefoner);

        var ressurser = Map.of(
                NAV_IDENT_A, ressursState(NAV_IDENT_A, PRIVATTELEFON_A),
                NAV_IDENT_B, ressursState(NAV_IDENT_B, PRIVATTELEFON_B),
                NAV_IDENT_X, TestData.ressursState()
        );
        when(ressursStateRepositoryMock.findAllActiveRessurser()).thenReturn(ressurser);

        var sorterteTelefonnummer = telefonService.hentPrioriterteTelefonnummer();


        assertThat(sorterteTelefonnummer.size()).isEqualTo(3);
        assertThat(sorterteTelefonnummer.get(NAV_IDENT_A).getFirst().type()).isEqualTo(NAV_TJENESTE_TELEFON);
    }

    @Test
    void should_return_tjenestetelefon_for_single_user_which_has_all_types() {
        var telefoner = List.of(
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(KONTORTELEFON_A).type(KONTORTELEFON).beskrivelse("Bla, bla").build(),
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(TJENESTETELEFON_A).type(TJENESTETELEFON).beskrivelse("Bla, bla").build()
        );
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAV_IDENT_A)).thenReturn(telefoner);

        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A);
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_A);
        assertThat(prioritertNummer).isEqualTo(TJENESTETELEFON_A);
    }

    @Test
    void should_return_kontortelefon_for_single_user_which_has_kontortelefon_and_private_number() {
        var telefoner = List.of(
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(KONTORTELEFON_A).type(KONTORTELEFON).beskrivelse("Bla, bla").build()
        );
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAV_IDENT_A)).thenReturn(telefoner);

        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A);
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_A);
        assertThat(prioritertNummer).isEqualTo(KONTORTELEFON_A);
    }

    @Test
    void should_return_privattelefon_for_single_user_which_only_has_private_number() {
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAV_IDENT_A)).thenReturn(Collections.emptyList());

        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A);
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_A);
        assertThat(prioritertNummer).isEqualTo(PRIVATTELEFON_A);
    }

    @Test
    void should_return_null_for_single_user_which_no_phone_numbers() {
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAV_IDENT_X)).thenReturn(Collections.emptyList());

        var ressursState = TestData.ressursState();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_X)).thenReturn(Optional.of(ressursState));

        var prioritertNummer = telefonService.getPrioritertTelefonnummer(NAV_IDENT_X);
        assertThat(prioritertNummer).isNull();
    }

    @Test
    void should_return_sorted_list_of_numbers_for_single_user() {
        var telefoner = List.of(
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(KONTORTELEFON_A).type(KONTORTELEFON).beskrivelse("Bla, bla").build(),
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(TJENESTETELEFON_A).type(TJENESTETELEFON).beskrivelse("Bla, bla").build()
        );
        when(dvhRepositoryMock.getTelefonNrListByNavIdent(NAV_IDENT_A)).thenReturn(telefoner);

        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A);
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        var alleTelefonnummer = telefonService.getAlleTelefonnummer(NAV_IDENT_A);
        assertThat(alleTelefonnummer.size()).isEqualTo(3);
    }

    private RessursState ressursState(String navident, String primaryTelefonnummer) {
        return TestData.ressursState()
                .toBuilder()
                .navident(navident)
                .primaryTelefonnummer(Optional.of(primaryTelefonnummer))
                .build();
    }

    @Test
    void should_call_ressurs_command_service() {
        var telefoner = List.of(
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(KONTORTELEFON_A).type(KONTORTELEFON).beskrivelse("Bla, bla").build(),
                DvhTelefon.builder().navIdent(NAV_IDENT_A).nummer(TJENESTETELEFON_A).type(TJENESTETELEFON).beskrivelse("Bla, bla").build()
        );

        var ressursState = ressursState(NAV_IDENT_A, PRIVATTELEFON_A);
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_A)).thenReturn(Optional.ofNullable(ressursState));

        telefonService.oppdaterRessursStateJobbtelefonBulk(telefoner);

        verify(ressursCommandServiceMock, times(2)).oppdaterJobbtelefonEvent(any(OppdaterJobbTelefonCommand.class), any(InputMetadata.class));
    }
}