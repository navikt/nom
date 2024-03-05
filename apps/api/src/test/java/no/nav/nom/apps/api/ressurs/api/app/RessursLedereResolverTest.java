package no.nav.nom.apps.api.ressurs.api.app;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.organisering.OrganiseringConfig;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.implementations.OrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.api.dto.RessursLeder;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class RessursLedereResolverTest {

    private static final String ORG_ENHET_1_ID = "orgEnhet_1_id";
    private static final String ORG_ENHET_1_NAVN = "orgEnhet_1_navn";
    private static final String NAV_IDENT_RESSURS = "R111111";
    private static final String ORG_ENHET_1_AGRESSO_ID = "orgEnhet_1_agresso_id";
    private static final String ORG_ENHET_1_ORG_NIV = "ORGENHET";

    private static final String ORG_ENHET_2_ID = "orgEnhet_2_id";
    private static final String ORG_ENHET_2_NAVN = "orgEnhet_2_navn";
    private static final String ORG_ENHET_2_AGRESSO_ID = "orgEnhet_2_agresso_id";
    private static final String ORG_ENHET_2_ORG_NIV = "ORGNIV3";

    private static final String ORG_ENHET_3_ID = "orgEnhet_3_id";
    private static final String ORG_ENHET_3_NAVN = "orgEnhet_3_navn";
    private static final String ORG_ENHET_3_AGRESSO_ID = "orgEnhet_3_agresso_id";
    private static final String ORG_ENHET_3_ORG_NIV = "ORGNIV2";


    private static final String NAV_IDENT_LEDER = "L999999";


    @Mock
    private Unleash unleash;

    @Mock
    private OrganiseringStateRepository organiseringStateRepositoryMock;

    @Mock
    private OrgenhetService orgenhetServiceMock;

    @Mock
    private DvhRepository dvhRepositoryMock;

    @Mock
    private RessursStateRepository ressursStateRepositoryMock;

    @Mock
    private OrgtilknytningService orgtilknytningServiceMock;


    @InjectMocks
    private RessursLedereResolver ressursLedereResolver;

    @Test
    @DisplayName("Finne leder ett nivå over")
    void should_find_leder_on_level_above() {
        OrganiseringState organisering = OrganiseringState.builder()
            .id(ORG_ENHET_1_ID)
            .navn(ORG_ENHET_1_NAVN)
            .idOver(ORG_ENHET_2_ID)
            .navnOver(ORG_ENHET_2_NAVN)
            .build();
        when(organiseringStateRepositoryMock.findActiveById(ORG_ENHET_1_ID)).thenReturn(Optional.of(organisering));

        OrgEnhet orgEnhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(ORG_ENHET_2_ID)
            .navn(ORG_ENHET_2_NAVN)
            .agressoId(Optional.of(ORG_ENHET_2_AGRESSO_ID))
            .agressoOrgNivaa(Optional.of(ORG_ENHET_2_ORG_NIV))
            .build());
        when(orgenhetServiceMock.getById(ORG_ENHET_2_ID)).thenReturn(Optional.of(orgEnhetOver));


        DvhLeder dvhLeder = DvhLeder.builder()
            .navIdent(NAV_IDENT_LEDER)
            .orgEnhetNr(ORG_ENHET_2_AGRESSO_ID)
            .orgNiv(ORG_ENHET_2_ORG_NIV)
            .orgenhetNavn(ORG_ENHET_2_NAVN)
            .build();
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(ORG_ENHET_2_AGRESSO_ID, ORG_ENHET_2_ORG_NIV, ORG_ENHET_2_NAVN))
            .thenReturn(List.of(dvhLeder));


        RessursState lederRessursState = RessursState.builder()
            .navident(NAV_IDENT_LEDER)
            .personident(NAV_IDENT_LEDER + "4321")
            .build();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_LEDER)).thenReturn(Optional.ofNullable(lederRessursState));


        List<RessursState> ledereReturned = ressursLedereResolver.lederForRecursiveUsingNewOrganisering(ORG_ENHET_1_ID, NAV_IDENT_RESSURS);

        assertThat(ledereReturned.size()).isEqualTo(1);
        assertThat(ledereReturned.get(0).navident()).isEqualTo(NAV_IDENT_LEDER);
    }

    @Test
    @DisplayName("Finne leder to nivåer over")
    void should_find_leder_two_levels_above() {
        OrganiseringState organisering = OrganiseringState.builder()
            .id(ORG_ENHET_1_ID)
            .navn(ORG_ENHET_1_NAVN)
            .idOver(ORG_ENHET_2_ID)
            .navnOver(ORG_ENHET_2_NAVN)
            .build();
        OrganiseringState organisering2 = OrganiseringState.builder()
            .id(ORG_ENHET_2_ID)
            .navn(ORG_ENHET_2_NAVN)
            .idOver(ORG_ENHET_3_ID)
            .navnOver(ORG_ENHET_3_NAVN)
            .build();
        when(organiseringStateRepositoryMock.findActiveById(ORG_ENHET_1_ID)).thenReturn(Optional.of(organisering));
        when(organiseringStateRepositoryMock.findActiveById(ORG_ENHET_2_ID)).thenReturn(Optional.of(organisering2));

        OrgEnhet orgEnhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(ORG_ENHET_2_ID)
            .navn(ORG_ENHET_2_NAVN)
            .agressoId(Optional.of(ORG_ENHET_2_AGRESSO_ID))
            .agressoOrgNivaa(Optional.of(ORG_ENHET_2_ORG_NIV))
            .build());
        OrgEnhet orgEnhetLeder = new OrgEnhet(OrgEnhetState.builder()
            .id(ORG_ENHET_3_ID)
            .navn(ORG_ENHET_3_NAVN)
            .agressoId(Optional.of(ORG_ENHET_3_AGRESSO_ID))
            .agressoOrgNivaa(Optional.of(ORG_ENHET_3_ORG_NIV))
            .build());
        when(orgenhetServiceMock.getById(ORG_ENHET_2_ID)).thenReturn(Optional.of(orgEnhetOver));
        when(orgenhetServiceMock.getById(ORG_ENHET_3_ID)).thenReturn(Optional.of(orgEnhetLeder));

        // Ressurs is leder on her own level and the level above
        DvhLeder dvhRessurs = DvhLeder.builder()
            .navIdent(NAV_IDENT_RESSURS)
            .orgEnhetNr(ORG_ENHET_2_AGRESSO_ID)
            .orgNiv(ORG_ENHET_2_ORG_NIV)
            .orgenhetNavn(ORG_ENHET_2_NAVN)
            .build();
        DvhLeder dvhLeder = DvhLeder.builder()
            .navIdent(NAV_IDENT_LEDER)
            .orgEnhetNr(ORG_ENHET_3_AGRESSO_ID)
            .orgNiv(ORG_ENHET_3_ORG_NIV)
            .orgenhetNavn(ORG_ENHET_3_NAVN)
            .build();
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(ORG_ENHET_2_AGRESSO_ID, ORG_ENHET_2_ORG_NIV, ORG_ENHET_2_NAVN))
            .thenReturn(List.of(dvhRessurs));
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(ORG_ENHET_3_AGRESSO_ID, ORG_ENHET_3_ORG_NIV, ORG_ENHET_3_NAVN))
            .thenReturn(List.of(dvhLeder));

        RessursState ressursRessursState = RessursState.builder()
            .navident(NAV_IDENT_RESSURS)
            .personident(NAV_IDENT_RESSURS + "4321")
            .build();
        RessursState lederRessursState = RessursState.builder()
            .navident(NAV_IDENT_LEDER)
            .personident(NAV_IDENT_LEDER + "4321")
            .build();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_RESSURS)).thenReturn(Optional.ofNullable(ressursRessursState));
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_LEDER)).thenReturn(Optional.ofNullable(lederRessursState));


        List<RessursState> ledereReturned = ressursLedereResolver.lederForRecursiveUsingNewOrganisering(ORG_ENHET_1_ID, NAV_IDENT_RESSURS);

        assertThat(ledereReturned.size()).isEqualTo(1);
        assertThat(ledereReturned.get(0).navident()).isEqualTo(NAV_IDENT_LEDER);
    }


    @ParameterizedTest(name = "Bruke ny implementasjon: {0}")
    @MethodSource("use_new_implementation")
    @DisplayName("Finne leder for ressurs rekursivt")
    void finn_leder_for_ressurs(boolean useNewImplementation) {
        if (useNewImplementation) {
            when(unleash.isEnabled(OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE)).thenReturn(true);
        } else {
            when(unleash.isEnabled(OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE)).thenReturn(false);
        }

        OrgEnhet orgEnhetRessurs = new OrgEnhet(OrgEnhetState.builder()
            .id(ORG_ENHET_1_ID)
            .navn(ORG_ENHET_1_NAVN)
            .agressoId(Optional.of(ORG_ENHET_1_AGRESSO_ID))
            .agressoOrgNivaa(Optional.of(ORG_ENHET_1_ORG_NIV))
            .build());
        OrgEnhet orgEnhetOver = new OrgEnhet(OrgEnhetState.builder()
            .id(ORG_ENHET_2_ID)
            .navn(ORG_ENHET_2_NAVN)
            .agressoId(Optional.of(ORG_ENHET_2_AGRESSO_ID))
            .agressoOrgNivaa(Optional.of(ORG_ENHET_2_ORG_NIV))
            .build());
        when(orgenhetServiceMock.getById(ORG_ENHET_1_ID)).thenReturn(Optional.of(orgEnhetRessurs));
        if (useNewImplementation) {
            when(orgenhetServiceMock.getById(ORG_ENHET_2_ID)).thenReturn(Optional.of(orgEnhetOver));
        } else {
            when(orgenhetServiceMock.getEnhetByAgressoIdNivaAndName(ORG_ENHET_2_AGRESSO_ID, ORG_ENHET_2_ORG_NIV, ORG_ENHET_2_NAVN))
                .thenReturn(Optional.of(orgEnhetOver));
        }


        if (useNewImplementation) {
            OrganiseringState organisering = OrganiseringState.builder()
                .id(ORG_ENHET_1_ID)
                .navn(ORG_ENHET_1_NAVN)
                .idOver(ORG_ENHET_2_ID)
                .navnOver(ORG_ENHET_2_NAVN)
                .build();
            when(organiseringStateRepositoryMock.findActiveById(ORG_ENHET_1_ID)).thenReturn(Optional.of(organisering));
        } else {
            DvhOrganisering dvhOrganisering = DvhOrganisering.builder()
                .agressoIdUnder(ORG_ENHET_1_AGRESSO_ID)
                .navnUnder(ORG_ENHET_1_NAVN)
                .orgNivUnder(ORG_ENHET_1_ORG_NIV)
                .agressoIdOver(ORG_ENHET_2_AGRESSO_ID)
                .navnOver(ORG_ENHET_2_NAVN)
                .orgNivOver(ORG_ENHET_2_ORG_NIV)
                .build();
            when(dvhRepositoryMock.getOrganiseringer(ORG_ENHET_1_AGRESSO_ID, ORG_ENHET_1_ORG_NIV, ORG_ENHET_1_NAVN))
                .thenReturn(List.of(dvhOrganisering));
        }

        DvhLeder dvhRessurs = DvhLeder.builder()
            .navIdent(NAV_IDENT_RESSURS)
            .orgEnhetNr(ORG_ENHET_2_AGRESSO_ID)
            .orgNiv(ORG_ENHET_2_ORG_NIV)
            .orgenhetNavn(ORG_ENHET_2_NAVN)
            .build();
        DvhLeder dvhLeder = DvhLeder.builder()
            .navIdent(NAV_IDENT_LEDER)
            .orgEnhetNr(ORG_ENHET_2_AGRESSO_ID)
            .orgNiv(ORG_ENHET_2_ORG_NIV)
            .orgenhetNavn(ORG_ENHET_2_NAVN)
            .build();
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(ORG_ENHET_1_AGRESSO_ID, ORG_ENHET_1_ORG_NIV, ORG_ENHET_1_NAVN))
            .thenReturn(List.of(dvhRessurs));
        when(dvhRepositoryMock.getLederByAgressoIdAndNivAndNavn(ORG_ENHET_2_AGRESSO_ID, ORG_ENHET_2_ORG_NIV, ORG_ENHET_2_NAVN))
            .thenReturn(List.of(dvhLeder));

        RessursState ressursRessursState = RessursState.builder()
            .navident(NAV_IDENT_RESSURS)
            .personident(NAV_IDENT_RESSURS + "4321")
            .build();
        RessursState lederRessursState = RessursState.builder()
            .navident(NAV_IDENT_LEDER)
            .personident(NAV_IDENT_LEDER + "4321")
            .build();
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_RESSURS)).thenReturn(Optional.ofNullable(ressursRessursState));
        when(ressursStateRepositoryMock.findByNavident(NAV_IDENT_LEDER)).thenReturn(Optional.ofNullable(lederRessursState));

        OrgtilknytningState orgtilknytning = OrgtilknytningState.builder()
            .navident(NAV_IDENT_RESSURS)
            .orgEnhetId(ORG_ENHET_1_ID)
            .gyldigFom(LocalDate.now().minusMonths(6))
            .gyldigTom(Optional.of(LocalDate.now().plusMonths(6)))
            .build();
        when(orgtilknytningServiceMock.getByNavIdent(NAV_IDENT_RESSURS)).thenReturn(List.of(orgtilknytning));


        List<RessursLeder> ledere = ressursLedereResolver.ledere(NAV_IDENT_RESSURS);

        assertThat(ledere.size()).isEqualTo(1);
        assertThat(ledere.get(0).leder().navident()).isEqualTo(NAV_IDENT_LEDER);
    }

    public static Stream<Arguments> use_new_implementation() {
        return Stream.of(
            Arguments.of(false),
            Arguments.of(true)
        );
    }
}
