package no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon;

import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.client.NomApiClient;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.exceptions.RequiredValueMissingException;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateState;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.aggregatestate.AggregateStateRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.errorstate.ErrorStateRepository;
import no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.repository.remedy.NomIntegrasjonEntity;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.platform.LeaderElection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.epochSecondsNorwegianTimeNoon;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.formatFornavnWithMellomnavn;
import static no.nav.nom.apps.nomremedyintegrasjon.nomintegrasjon.Utils.nomSektorToRemedyCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class NomRemedyIntegrasjonServiceTest {

    @InjectMocks
    private NomRemedyIntegrasjonService nomRemedyIntegrasjonService;

    @Mock
    private RemedyIntegrasjonRepositoryService remedyIntegrasjonRepositoryService;

    @Mock
    private LeaderElection leaderElection;

    @Mock
    private NomApiClient nomApiClient;

    @Mock
    private AggregateStateRepository aggregateStateRepository;

    @Mock
    private RessursStateRepository ressursStateRepository;

    @Mock
    private OrgtilknytningStateRepository orgtilknytningStateRepository;

    @Mock
    private ErrorStateRepository errorStateRepository;

    @BeforeEach
    public void setUp() {
        // Clear the repositories before each test
        lenient().doReturn(true).when(leaderElection).isLeader();
        lenient().doReturn("2990").when(nomApiClient).getRemedyEnhetId(anyString());
        lenient().doReturn("12345678").when(nomApiClient).getTelefonnummer(anyString());
    }

    @Test
    @DisplayName("Burde kjenne igjen en ny ressurs som har blitt opprettet og skrive til Remedy Integrasjon tabellen")
    void test_001() {

        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().plusMonths(2).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom);

        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, gyldigFom);

        var aggregateState = aggregate(ressursFid, ressursState.getClass().getSimpleName(), navident);

        var nomIntegrasjonEntity = nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, false);

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByFid(ressursFid);

        doReturn(List.of(orgtilknytningState)).when(orgtilknytningStateRepository).findByNavident(navident);

        doReturn(nomIntegrasjonEntity).when(remedyIntegrasjonRepositoryService).saveNewNomIntegrasjonEntityNew(nomIntegrasjonEntity, ressursState.sistEndretAv());

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(1)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }

    @Test
    @DisplayName("Endret DOL orgtilknytning, lagre ny rad i Remedy Integrasjon tabellen")
    void test_002() {
        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now();

        var ressursState = ressurs(ressursFid, navident, gyldigFom.minusYears(2).withDayOfMonth(1)).toBuilder().sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, gyldigFom);
        var aggregateState = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident);
        var nomIntegrasjonEntity = nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, false);

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        doReturn(nomIntegrasjonEntity).when(remedyIntegrasjonRepositoryService).saveNewNomIntegrasjonEntityNew(nomIntegrasjonEntity, orgtilknytningState.sistEndretAv());

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(1)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }

    @Test
    @DisplayName("Ikke DOL orgtilknytning har dukket opp i tabellen, burde ikke lagre til Remedy Integrasjon tabellen")
    void test_003() {
        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now();

        var ressursState = ressurs(ressursFid, navident, gyldigFom.minusYears(2).withDayOfMonth(1)).toBuilder().sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, gyldigFom).toBuilder().erDagligOppfolging(false).build();
        var aggregateState = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident);

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(0)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }

    @Test
    @DisplayName("Fremtidig DOL i tabellen, blir oppdatert med delay dato i aggregate tabellen")
    void test_004() {
        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().plusMonths(2).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom.minusYears(2).withDayOfMonth(1)).toBuilder().sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, gyldigFom);
        var aggregateState = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident);
        var currentOrgtilknytningState = orgtilknytning(UUID.randomUUID().toString(), navident, LocalDate.now().minusYears(2).withDayOfMonth(1)).toBuilder()
                .orgEnhetId("orgEnhetId-321")
                .build();

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        doReturn(List.of(currentOrgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByNavident(navident);

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).save(aggregateState.toBuilder().delayedUntil(orgtilknytningState.gyldigFom()).build());
        verify(aggregateStateRepository, times(0)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(0)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }

    @Test
    @DisplayName("Utsatt OrgtilknytningState har nådd delay datoen sin, burde lagre til Remedy Integrasjon tabellen")
    void test_005() {
        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().plusMonths(2).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom.minusYears(2).withDayOfMonth(1)).toBuilder().sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, LocalDate.now());
        var aggregateState = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident).toBuilder().delayedUntil(LocalDate.now()).build();

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAggregateStatesByMasseflyttingIsFalseOrMasseflyttingIsNullAndDelayedUntilIsLessThanEqual(LocalDate.now());

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        // When
        nomRemedyIntegrasjonService.executeDelayedStateUpdate();

        // Then
        verify(aggregateStateRepository, times(0)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).findAggregateStatesByMasseflyttingIsFalseOrMasseflyttingIsNullAndDelayedUntilIsLessThanEqual(any());
        verify(aggregateStateRepository, times(0)).save(aggregateState.toBuilder().delayedUntil(orgtilknytningState.gyldigFom()).build());
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(1)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }

    @Test
    @DisplayName("Masseflytting av OrgtilknytningState, burde lagre til Remedy Integrasjon tabellen")
    void test_006() {
        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().plusMonths(2).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom.minusYears(2).withDayOfMonth(1)).toBuilder().sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, LocalDate.now()).toBuilder().masseflyttetFraFid(UUID.randomUUID().toString()).sistEndretAv("T123456").build();
        var aggregateState = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident).toBuilder().masseflytting(true).build();
        var nomIntegrasjonEntity = nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, true);

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        doReturn(nomIntegrasjonEntity).when(remedyIntegrasjonRepositoryService).saveNewNomIntegrasjonEntityNew(nomIntegrasjonEntity, orgtilknytningState.sistEndretAv());

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(1)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }

    @Test
    @DisplayName("Masseflytting av fremtidig OrgtilknytningState, burde lagre til Remedy Integrasjon tabellen")
    void test_007() {
        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().plusMonths(2).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom.minusYears(2).withDayOfMonth(1)).toBuilder().sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, LocalDate.now().plusMonths(2).withDayOfMonth(1)).toBuilder().masseflyttetFraFid(UUID.randomUUID().toString()).sistEndretAv("T123456").build();
        var aggregateState = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident).toBuilder().masseflytting(true).build();
        var nomIntegrasjonEntity = nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, true);

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        doReturn(nomIntegrasjonEntity).when(remedyIntegrasjonRepositoryService).saveNewNomIntegrasjonEntityNew(nomIntegrasjonEntity, orgtilknytningState.sistEndretAv());

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(1)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }

    @Test
    @DisplayName("Ressurs har sluttdato satt og er i fortiden, skal ikke lagres i Remedy og slettes i AggregateStateRepository")
    void test_008() {
        // When
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().minusYears(2).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom).toBuilder().sluttdato(LocalDate.now().minusDays(1)).sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, gyldigFom).toBuilder().gyldigTom(LocalDate.now().minusDays(2)).sistEndretAv("X123456").build();
        var aggregateState = aggregate(ressursFid, ressursState.getClass().getSimpleName(), navident);
        var nomIntegrasjonEntity = nomIntegrasjonEntityFromNomData(ressursState, orgtilknytningState, false).toBuilder().entryOperasjon("Ny").build();

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(nomIntegrasjonEntity))
                .when(remedyIntegrasjonRepositoryService)
                .findTopByNavIdEqualsOrderByEntryIdDesc(navident);

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByFid(ressursFid);

        doReturn(List.of())
                .when(orgtilknytningStateRepository)
                .findByNavident(navident);

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(remedyIntegrasjonRepositoryService, times(1)).markRessursForDeletion(navident, ressursState.sistEndretAv());
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
    }

    @Test
    @DisplayName("Orgtilknytning er masseflytting og fremtidig, skal lagres i remedy tabellen")
    void test_009() {
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().minusYears(2).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom).toBuilder().sistEndretAv("X123456").build();
        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, gyldigFom)
                .toBuilder()
                .gyldigFom(LocalDate.now().plusMonths(1).withDayOfMonth(1))
                .masseflyttetFraFid(UUID.randomUUID().toString())
                .sistEndretAv("X123456")
                .build();
        var aggregateState = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident)
                .toBuilder()
                .masseflyttetFraFid(orgtilknytningState.masseflyttetFraFid())
                .masseflytting(true)
                .build();

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(remedyIntegrasjonRepositoryService, times(1)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));

    }

    @Test
    @DisplayName("Ressurs sluttdato endres til en dato i fremtiden, med en fremtidig ny orgtilknytning")
    void test_010() {
        String ressursFid = UUID.randomUUID().toString();
        String orgtilknytningFid = UUID.randomUUID().toString();
        String orgtilknytningFid2 = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().minusYears(2).withDayOfMonth(1);
        LocalDate gyldigFom2 = LocalDate.now().plusMonths(1).withDayOfMonth(15);

        var ressursState = ressurs(ressursFid, navident, gyldigFom).toBuilder()
            .sektor(Sektor.EKSTERN)
                .sistEndretAv("X123456")
                .build();

        var orgtilknytningState = orgtilknytning(orgtilknytningFid, navident, gyldigFom)
            .toBuilder()
            .sektor(Sektor.EKSTERN)
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .gyldigTom(LocalDate.now().plusMonths(1).withDayOfMonth(14))
            .sistEndretAv("X123456")
            .build();

        var orgtilknytningState2 = orgtilknytning(orgtilknytningFid2, navident, gyldigFom2)
            .toBuilder()
            .sektor(Sektor.EKSTERN)
            .orgtilknytningType(Tilknytningtype.KONSULENT)
            .sistEndretAv("X123456")
            .build();

        var aggregateState = aggregate(ressursFid, ressursState.getClass().getSimpleName(), navident)
            .toBuilder()
            .masseflyttetFraFid(null)
            .build();

        var aggregateStateOrgtilknytningOne = aggregate(orgtilknytningFid, orgtilknytningState.getClass().getSimpleName(), navident)
                .toBuilder()
                .masseflyttetFraFid(orgtilknytningState.masseflyttetFraFid())
                .build();

        var aggregateStateOrgtilknytningTwo = aggregate(orgtilknytningFid2, orgtilknytningState2.getClass().getSimpleName(), navident)
                .toBuilder()
                .masseflyttetFraFid(orgtilknytningState2.masseflyttetFraFid())
                .build();

        // And
        doReturn(List.of(aggregateState, aggregateStateOrgtilknytningOne, aggregateStateOrgtilknytningTwo))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByFid(ressursFid);

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(Optional.of(orgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid);

        doReturn(Optional.of(orgtilknytningState2))
                .when(orgtilknytningStateRepository)
                .findByFid(orgtilknytningFid2);

        doReturn(List.of(orgtilknytningState))
            .when(orgtilknytningStateRepository)
                .findByNavident(navident);

        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(remedyIntegrasjonRepositoryService, times(2)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
        verify(aggregateStateRepository, times(1)).deleteAllById(anyList());
        verify(aggregateStateRepository, times(1)).save(any(AggregateState.class));
    }

    @Test
    @DisplayName("Ressurs har sluttet som ekstern og skal starte noen år senere som Statlig")
    void test_011() {
        String ressursFid = UUID.randomUUID().toString();
        String pastOrgtilknytningFid = UUID.randomUUID().toString();
        String futureOrgtilknytningFid = UUID.randomUUID().toString();
        String navident = "S123456";
        LocalDate gyldigFom = LocalDate.now().minusYears(4).withDayOfMonth(1);

        var ressursState = ressurs(ressursFid, navident, gyldigFom).toBuilder()
                .sektor(Sektor.EKSTERN)
                .sluttdato(LocalDate.now().minusYears(3))
                .sistEndretAv("X123456")
                .build();

        var pastOrgtilknytningState = orgtilknytning(pastOrgtilknytningFid, navident, gyldigFom)
                .toBuilder()
                .sektor(Sektor.EKSTERN)
                .gyldigTom(LocalDate.now().minusYears(3))
                .sistEndretAv("X123456")
                .build();

        var futureOrgtilknytningState = orgtilknytning(futureOrgtilknytningFid, navident, LocalDate.now().plusMonths(1).withDayOfMonth(1))
                .toBuilder()
                .sektor(Sektor.NAV_STATLIG)
                .orgtilknytningType(Tilknytningtype.FAST)
                .sistEndretAv("X123456")
                .build();

        var aggregateState = aggregate(futureOrgtilknytningFid, futureOrgtilknytningState.getClass().getSimpleName(), navident);
        var nomIntegrasjonEntity = nomIntegrasjonEntityFromNomData(ressursState, futureOrgtilknytningState, false).toBuilder().entryOperasjon("Ny").build();

        // And
        doReturn(List.of(aggregateState))
                .when(aggregateStateRepository)
                .findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();

        doReturn(Optional.of(futureOrgtilknytningState))
            .when(orgtilknytningStateRepository)
                .findByFid(futureOrgtilknytningFid);

        doReturn(Optional.of(ressursState))
                .when(ressursStateRepository)
                .findByNavident(navident);

        doReturn(List.of(pastOrgtilknytningState, futureOrgtilknytningState))
                .when(orgtilknytningStateRepository)
                .findByNavident(navident);

        // When
        nomRemedyIntegrasjonService.executeImmediateStateUpdate();

        // Then
        verify(aggregateStateRepository, times(1)).findAllOlderThanFiveSecondsWhereDelayedUntilIsNull();
        verify(aggregateStateRepository, times(1)).deleteAllById(List.of(aggregateState.getAggregateStateId()));
        verify(remedyIntegrasjonRepositoryService, times(1)).saveNewNomIntegrasjonEntityNew(any(NomIntegrasjonEntity.class), anyString());
    }


    private Integer getSektor(RessursState ressursState, OrgtilknytningState dolOrgtilknytning) {
        return isNull(dolOrgtilknytning.sektor()) ? nomSektorToRemedyCode(ressursState.sektor()) : nomSektorToRemedyCode(dolOrgtilknytning.sektor());
    }

    private AggregateState aggregate(String fid, String stateType, String navident) {
        return AggregateState.builder()
                .aggregateStateId(UUID.randomUUID())
                .stateFid(fid)
                .stateType(stateType)
                .masseflyttetFraFid(null)
                .createdAt(LocalDateTime.now().minusSeconds(5))
                .delayedUntil(null)
                .navident(navident)
                .masseflytting(false)
                .build();
    }

    private OrgtilknytningState orgtilknytning(String orgtilknytningFid, String navident, LocalDate gyldigFom) {
        return OrgtilknytningState.builder()
                .fid(orgtilknytningFid)
                .navident(navident)
                .orgEnhetId("orgEnhetId-123")
                .sektor(Sektor.NAV_STATLIG)
                .erDagligOppfolging(true)
                .gyldigFom(gyldigFom)
                .gyldigTom(null)
                .orgtilknytningType(Tilknytningtype.FAST)
                .masseflyttetFraFid(null)
                .sistEndretAv("T123456")
                .build();
    }

    private RessursState ressurs(String ressursFid, String navident, LocalDate gyldigFom) {
        return RessursState.builder()
                .fid(ressursFid)
                .personident("1234567812345")
                .navident(navident)
                .sektor(Sektor.NAV_STATLIG)
                .visningsnavn(new Visningsnavn("Nav", Optional.empty(), "Navesen"))
                .primaryTelefonnummer(null)
                .startdato(gyldigFom)
                .sluttdato(null)
                .primaryFysiskBrukersted("2700")
                .fysiskeBrukersteder(Collections.emptyList())
                .identType(IdentType.STANDARD)
                .sistEndretAv("T123456")
                .build();
    }

    private Optional<LocalDate> getUtlopsdato(Integer sektor, OrgtilknytningState dolOrgtilknytning) {
        var maaHaSluttDato = erKonsulentSomMaaHaSluttDato(sektor, dolOrgtilknytning);
        if (maaHaSluttDato && isNull(dolOrgtilknytning.gyldigTom()) ) {
            throw new RequiredValueMissingException("Utløpsdato er påkrevd for ekstern sektor");
        }
        return maaHaSluttDato ? Optional.of(dolOrgtilknytning.gyldigTom()): Optional.empty();
    }

    private boolean erKonsulentSomMaaHaSluttDato(Integer sektor, OrgtilknytningState ot) {
        var tilknytningstype = requireNonNullElse(ot.orgtilknytningType(), Tilknytningtype.ANNET);
        //Hvis 'Ekstern' sektor, krev sluttdato med unntak av HELFO og Skatteetaten
        return sektor == 2 && tilknytningstype != Tilknytningtype.HELFO && tilknytningstype != Tilknytningtype.SKATTEETATEN;
    }

    private NomIntegrasjonEntity nomIntegrasjonEntityFromNomData(RessursState ressursState, OrgtilknytningState dolOrgtilknytning, boolean isMasseflytting) {
        var sektor = getSektor(ressursState, dolOrgtilknytning);
        var utlopsdato = getUtlopsdato(sektor, dolOrgtilknytning);

        return NomIntegrasjonEntity.builder()
                .fornavn(formatFornavnWithMellomnavn(ressursState.visningsnavn()).trim())
                .etternavn(ressursState.visningsnavn().etternavn().trim())
                .navId(ressursState.navident())
                .fodselsnummer(ressursState.personident())
                .sektor(sektor)
                .enhetsId("2990")
                .fysiskBrukerstedId(ressursState.primaryFysiskBrukersted())
                .tiltredelsesdato(epochSecondsNorwegianTimeNoon(dolOrgtilknytning.gyldigFom()))
                .utlopsdato(utlopsdato.map(Utils::epochSecondsNorwegianTimeEndOfDay).orElse(null))
                .mobil("12345678")
                .masseflytting(isMasseflytting ? "X" : null)
                .build();
    }
}
