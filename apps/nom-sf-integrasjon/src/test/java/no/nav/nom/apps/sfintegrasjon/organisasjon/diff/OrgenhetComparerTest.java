package no.nav.nom.apps.sfintegrasjon.organisasjon.diff;

import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfDepartment;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfDivision;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfOrgEnhetType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrgenhetComparerTest {
    private LocalDate cutOffDate = LocalDate.now().minusMonths(1);

    private final OrgEnhetLederStateRepository lederRepository = mock(OrgEnhetLederStateRepository.class);
    private final OrganiseringStateRepository organiseringRepository = mock(OrganiseringStateRepository.class);
    private final OrgEnhetStateRepository orgenhetRepository = mock(OrgEnhetStateRepository.class);
    private final OrgenhetComparer comparer = new OrgenhetComparer(
        null, new SfProperties(null, null, null, null, null, null, cutOffDate, "TOPPPNODE_ID"),
        lederRepository, organiseringRepository, orgenhetRepository
    );

    @Test
    void reportsNomLeaderChangeBetweenSfVersionsAndDoesNotRepeatUnchangedMismatch() {
        var nom = mock(OrgEnhetState.class);
        when(nom.id()).thenReturn("enhet");
        when(lederRepository.findAllByOrgEnhetId("enhet")).thenReturn(List.of(
            leder("A", cutOffDate.minusDays(10), cutOffDate.plusDays(2)),
            leder("B", cutOffDate.plusDays(3), null)));

        assertThat(comparer.compareLedere(
            nom,
            List.of(
                sfDepartment(cutOffDate.plusDays(5), "A"),
                sfDepartment(cutOffDate.minusDays(4), "A"),
                sfDepartment(cutOffDate.plusDays(1), "A"))))
            .containsExactly(
                new LederMismatchDetail(cutOffDate.plusDays(3), "A", "B"),
                new LederMismatchDetail(cutOffDate.plusDays(5), "A", "B")
            );
    }

    @Test
    void reportsMissingLeaderAcrossSfVersions() {
        var nom = mock(OrgEnhetState.class);
        when(nom.id()).thenReturn("enhet");
        when(lederRepository.findAllByOrgEnhetId("enhet"))
            .thenReturn(List.of(leder("A", cutOffDate.minusDays(1), null)));

        assertThat(comparer.compareLedere(nom, List.of(sfDepartment(cutOffDate, null), sfDepartment(cutOffDate.plusDays(2), null))))
            .containsExactly(
                new LederMissingInSF(cutOffDate, "A"),
                new LederMissingInSF(cutOffDate.plusDays(2), "A")
            );
    }

    @Test
    void respectsInclusiveNomEndDateAndDoesNotCompareBeforeFirstFutureSfVersion() {
        var nom = mock(OrgEnhetState.class);
        when(nom.id()).thenReturn("enhet");
        when(lederRepository.findAllByOrgEnhetId("enhet")).thenReturn(List.of(
            leder("A", cutOffDate, cutOffDate.plusDays(2)),
            leder("B", cutOffDate.plusDays(5), null)));

        assertThat(comparer.compareLedere(nom, List.of(sfDepartment(cutOffDate.plusDays(1), "X"))))
            .containsExactly(
                new LederMismatchDetail(cutOffDate.plusDays(1), "X", "A"),
                new LederMissingInNOM(cutOffDate.plusDays(3), "X"),
                new LederMismatchDetail(cutOffDate.plusDays(5), "X", "B")
            );
    }

    @Test
    void returnsNoDiffWhenNomHasNoLeaderOrSfLeaderMatches() {
        var nom = mock(OrgEnhetState.class);
        when(nom.id()).thenReturn("enhet");
        when(lederRepository.findAllByOrgEnhetId("enhet"))
            .thenReturn(List.of(leder("A", cutOffDate.plusDays(2), null)));

        assertThat(comparer.compareLedere(nom, List.of(sfDepartment(cutOffDate, "A"))))
            .containsExactly(new LederMissingInNOM(cutOffDate, "A"));
    }

    @Test
    void reportsEachSfLeaderMismatch() {
        var nom = mock(OrgEnhetState.class);
        when(nom.id()).thenReturn("enhet");
        when(lederRepository.findAllByOrgEnhetId("enhet"))
            .thenReturn(List.of(leder("A", cutOffDate.minusDays(2), null)));

        assertThat(comparer.compareLedere(nom, List.of(
                sfDepartment(cutOffDate.plusDays(4), "A"),
                sfDepartment(cutOffDate, "B"),
                sfDepartment(cutOffDate.plusDays(2), "C"))))
            .containsExactly(
                new LederMismatchDetail(cutOffDate, "B", "A"),
                new LederMismatchDetail(cutOffDate.plusDays(2), "C", "A"));
    }

    @Test
    void comparesLeaderAtCutoffUsingSfVersionAndNomPeriodStartedEarlier() {
        var nom = nom("enhet");
        when(lederRepository.findAllByOrgEnhetId("enhet")).thenReturn(List.of(
            leder("old", cutOffDate.minusDays(20), cutOffDate.minusDays(10)),
            leder("A", cutOffDate.minusDays(9), null)));

        assertThat(comparer.compareLedere(nom, List.of(sfDepartment(cutOffDate.minusDays(5), "B"))))
            .containsExactly(new LederMismatchDetail(cutOffDate, "B", "A"));
    }

    @Test
    void leaderChangeOnDayAfterInclusiveEndIsCompared() {
        var nom = nom("enhet");
        when(lederRepository.findAllByOrgEnhetId("enhet")).thenReturn(List.of(
            leder("A", cutOffDate.minusDays(1), cutOffDate.plusDays(2)),
            leder("B", cutOffDate.plusDays(3), null)));

        assertThat(comparer.compareLedere(nom, List.of(sfDepartment(cutOffDate, "A"))))
            .containsExactly(new LederMismatchDetail(cutOffDate.plusDays(3), "A", "B"));
    }

    @Test
    void comparesParentAtCutoffAndWhenNomPeriodChangesBetweenSfVersions() {
        var nom = nom("enhet");
        when(organiseringRepository.findById("enhet")).thenReturn(List.of(
            organisering("P1", cutOffDate.minusDays(10), cutOffDate.plusDays(2)),
            organisering("P2", cutOffDate.plusDays(3), null)));

        assertThat(comparer.compareOrganisering(nom, List.of(
            sfDivision(cutOffDate.plusDays(5), "P1"),
            sfDivision(cutOffDate.minusDays(4), "P1"))))
            .containsExactly(
                new ParentMismatchDetail(cutOffDate.plusDays(3), "P1", "P2"),
                new ParentMismatchDetail(cutOffDate.plusDays(5), "P1", "P2"));
    }

    @Test
    void reportsMissingNomParentAfterInclusiveEndAndNextNomParentAtNewStart() {
        var nom = nom("enhet");
        when(organiseringRepository.findById("enhet")).thenReturn(List.of(
            organisering("P1", cutOffDate, cutOffDate.plusDays(2)),
            organisering("P2", cutOffDate.plusDays(5), null)));

        assertThat(comparer.compareOrganisering(nom, List.of(sfDivision(cutOffDate.plusDays(1), "P1"))))
            .containsExactly(
                new ParentMissingInNOM(cutOffDate.plusDays(3), "P1"),
                new ParentMismatchDetail(cutOffDate.plusDays(5), "P1", "P2"));
    }

    @Test
    void reportsMissingSfParentAndMatchesLaterSfVersion() {
        var nom = nom("enhet");
        when(organiseringRepository.findById("enhet"))
            .thenReturn(List.of(organisering("P1", cutOffDate.minusDays(1), null)));

        assertThat(comparer.compareOrganisering(nom, List.of(
            sfDivision(cutOffDate, null), sfDivision(cutOffDate.plusDays(2), "P1"))))
            .containsExactly(new ParentMissingInSF(cutOffDate, "P1"));
    }

    @Test
    void scalarComparisonsReportMissingAndMismatchingValuesWithoutDiffForMatch() {
        var nom = mock(OrgEnhetState.class);
        when(nom.navn()).thenReturn("Riktig navn");
        when(nom.nomNivaa()).thenReturn(NomNivaa.LINJEENHET);
        when(nom.orgEnhetsType()).thenReturn(OrgEnhetsType.NAV_KONTOR);
        when(nom.tilgangsenhetId()).thenReturn("T1");
        var missing = sfDepartment(cutOffDate);
        var mismatch = sfDepartment(cutOffDate.plusDays(1));
        var match = sfDepartment(cutOffDate.plusDays(2));
        when(missing.cust_nom_nivaa()).thenReturn(Optional.empty());
        when(missing.cust_orgenhet_type()).thenReturn(Optional.empty());
        when(missing.cust_Tilgangsenhet()).thenReturn(Optional.empty());
        when(mismatch.name()).thenReturn("Annet navn");
        when(mismatch.cust_nom_nivaa()).thenReturn(Optional.of("DRIFTSENHET"));
        when(mismatch.cust_orgenhet_type()).thenReturn(Optional.of("ANNEN_TYPE"));
        when(mismatch.cust_Tilgangsenhet()).thenReturn(Optional.of("T2"));
        when(match.name()).thenReturn("Riktig navn");
        when(match.cust_nom_nivaa()).thenReturn(Optional.of("LINJEENHET"));
        when(match.cust_orgenhet_type()).thenReturn(Optional.of("NAV_KONTOR"));
        when(match.cust_Tilgangsenhet()).thenReturn(Optional.of("T1"));

        var sf = List.of(missing, mismatch, match);
        assertThat(comparer.compareName(nom, sf)).containsExactly(
            new NameMissingInSF(cutOffDate, "Riktig navn"),
            new NameMismatch(cutOffDate.plusDays(1), "Annet navn", "Riktig navn"));
        assertThat(comparer.compareNivaa(nom, sf)).containsExactly(
            new NomNivaaMissingInSF(cutOffDate, "LINJEENHET"),
            new NomNivaaMismatch(cutOffDate.plusDays(1), "DRIFTSENHET", "LINJEENHET"));
        assertThat(comparer.compareType(nom, sf)).containsExactly(
            new NomTypeMissingInSF(cutOffDate, "NAV_KONTOR"),
            new NomTypeMismatch(cutOffDate.plusDays(1), "ANNEN_TYPE", "NAV_KONTOR"));
        assertThat(comparer.compareTilgangsenhet(nom, sf)).containsExactly(
            new NomTilgangsenhetMissingInSF(cutOffDate, "T1"),
            new NomTilgangsenhetMismatch(cutOffDate.plusDays(1), "T2", "T1"));
    }

    @Test
    void findDifferencesReturnsOnlyMissingInSfWhenSfHasNoVersions() {
        assertThat(comparer.findDifferences(nom("enhet"), List.of()))
            .containsExactly(new MissingInSf("enhet"));
    }

    @Test
    void reportsWrongSfObjectTypeForNomLineUnit() {
        var nom = mock(OrgEnhetState.class);
        when(nom.nomNivaa()).thenReturn(NomNivaa.LINJEENHET);

        assertThat(comparer.compareSFEnhetsObjekt(nom, List.of(sfDivision(cutOffDate, null))))
            .containsExactly(new SFEnhetsObjektMismatch(cutOffDate, SfOrgEnhetType.DIVISION, SfOrgEnhetType.BUSINESS_UNIT));
    }

    @Test
    void emptyInputReturnsEmptyList() {
        assertThat(comparer.versjonerForBehandling(List.of())).isEmpty();
    }

    @Test
    void onlyHistoricalUnitsReturnsLatest() {
        SfOrgEnhet older = sfDepartment(cutOffDate.minusDays(10));
        SfOrgEnhet latest = sfDepartment(cutOffDate.minusDays(2));

        assertThat(comparer.versjonerForBehandling(List.of(latest, older)))
            .containsExactly(latest);
    }

    @Test
    void includesAllUnitsWithLatestHistoricalStartDate() {
        SfOrgEnhet older = sfDepartment(cutOffDate.minusDays(10));
        SfOrgEnhet first = sfDepartment(cutOffDate.minusDays(2));
        SfOrgEnhet second = sfDepartment(cutOffDate.minusDays(2));

        assertThat(comparer.versjonerForBehandling(List.of(older, first, second)))
            .containsExactly(first, second);
    }

    @Test
    void includesTodayAndFutureButExcludesHistoricalUnits() {
        SfOrgEnhet historical = sfDepartment(cutOffDate.minusDays(1));
        SfOrgEnhet current = sfDepartment(cutOffDate);
        SfOrgEnhet future = sfDepartment(cutOffDate.plusDays(1));

        assertThat(comparer.versjonerForBehandling(List.of(future, historical, current)))
            .containsExactly(current, future);
    }

    @Test
    void onlyFutureUnitsReturnsAll() {
        SfOrgEnhet first = sfDepartment(cutOffDate.plusDays(1));
        SfOrgEnhet second = sfDepartment(cutOffDate.plusDays(5));

        assertThat(comparer.versjonerForBehandling(List.of(second, first)))
            .containsExactly(first, second);
    }

    @Test
    void sortsUnorderedUnitsByStartDate() {
        SfOrgEnhet historical = sfDepartment(cutOffDate.minusDays(3));
        SfOrgEnhet current = sfDepartment(cutOffDate.minusDays(1));
        SfOrgEnhet later = sfDepartment(cutOffDate.plusDays(10));
        SfOrgEnhet next = sfDepartment(cutOffDate.plusDays(2));

        assertThat(comparer.versjonerForBehandling(List.of(later, historical, next, current)))
            .containsExactly(current, next, later);
    }

    private static SfOrgEnhet sfDepartment(LocalDate startDate) {
        SfDepartment unit = mock(SfDepartment.class);
        when(unit.startDate()).thenReturn(startDate);
        return unit;
    }

    private static SfOrgEnhet sfDepartment(LocalDate startDate, String lederIdent) {
        SfDepartment unit = mock(SfDepartment.class);
        when(unit.startDate()).thenReturn(startDate);
        when(unit.cust_LederNavIdent()).thenReturn(Optional.ofNullable(lederIdent));
        return unit;
    }

    private static OrgEnhetLederState leder(String ident, LocalDate start, LocalDate slutt) {
        return new OrgEnhetLederState(null, ident, "enhet", start, slutt, null);
    }

    private static OrgEnhetState nom(String id) {
        var nom = mock(OrgEnhetState.class);
        when(nom.id()).thenReturn(id);
        return nom;
    }

    private static OrganiseringState organisering(String parentId, LocalDate fra, LocalDate til) {
        return new OrganiseringState(null, "enhet", null, parentId, null, fra, til);
    }

    private static SfDivision sfDivision(LocalDate startDate, String parentId) {
        var division = mock(SfDivision.class);
        when(division.startDate()).thenReturn(startDate);
        if (parentId != null) {
            var parent = mock(SfDivision.class);
            when(parent.externalCode()).thenReturn(parentId);
            when(division.parentEnhet()).thenReturn(parent);
        }
        return division;
    }
}
