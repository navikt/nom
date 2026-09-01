package no.nav.nom.apps.sfintegrasjon.organisasjon.diff;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfODataClient;
import no.nav.nom.apps.sfintegrasjon.sfclient.SfProperties;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfBusinessUnit;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfDepartment;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfDivision;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfOrgEnhet;
import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfOrgEnhetType;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.api.graphql.Organisering;
import no.nav.nom.contract.nom.state.OrgEnhetLederState;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrganiseringState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgenhetleder.OrgEnhetLederStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;
import java.util.stream.Stream;

//          NomNivaa
//           LOVNODE -> should correspond to a SF Department
//           ARBEIDSOMRAADE and DRIFTSENHET -> should correspond to a SF Division
//           LINJEENHET -> should correspond to a SF Business Unit

@Slf4j
@Service
public class OrgenhetComparer {
    private final SfODataClient sfODataClient;
    private final LocalDate historicalCutoffDate;
    private final String toppnodeId;

    private final OrgEnhetLederStateRepository orgEnhetLederStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final OrgEnhetStateRepository orgEnhetRepository;

    public OrgenhetComparer(
        SfODataClient sfODataClient,
        SfProperties sfProperties,
        OrgEnhetLederStateRepository orgEnhetLederStateRepository,
        OrganiseringStateRepository organiseringStateRepository,
        OrgEnhetStateRepository orgEnhetRepository
    ) {
        this.sfODataClient = sfODataClient;
        this.historicalCutoffDate = sfProperties.historicalCutoffDate();
        this.toppnodeId = sfProperties.navToppnodeId();
        this.orgEnhetLederStateRepository = orgEnhetLederStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.orgEnhetRepository = orgEnhetRepository;
    }

    public OrgenhetDiff compare(String nomOrgenhetId) {
        return compare(orgEnhetRepository.findById(nomOrgenhetId).orElseThrow(() -> new IllegalArgumentException("OrgEnhetState not found for id")));
    }

    // Will not compare historical before given date - SF do not import historical data.
    public OrgenhetDiff compare(OrgEnhetState nomState) {
        String nomId = nomState.id();

        List<SfOrgEnhet> sfEnheter = versjonerForBehandling(lookupCorrespondingSfEnheter(nomId));

        var differences = findDifferences(nomState, sfEnheter);

        OrgenhetDiff diff = new OrgenhetDiff(nomId, nomState, sfEnheter, differences);

        return diff;
    }

    /**
     * Lookup corresponding SF enheter based on the nomId
     * look up nomid in both Department, Division and BusinessUnit
     */
    List<SfOrgEnhet> lookupCorrespondingSfEnheter(String nomId) {
        return Stream.of(
                sfODataClient.getFODepartmentsByNomOrgEnhetId(nomId).stream().map(SfDepartment::new).map(SfOrgEnhet.class::cast),
                sfODataClient.getFODivisionsByNomOrgEnhetId(nomId).stream().map(SfDivision::new).map(SfOrgEnhet.class::cast),
                sfODataClient.getFOBusinessUnitsByNomOrgEnhetId(nomId).stream().map(SfBusinessUnit::new).map(SfOrgEnhet.class::cast)
            )
            .flatMap(stream -> stream)
            .toList();
    }

    List<DiffDetail> findDifferences(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        if (sfEnheter.isEmpty()) {
            return List.of(new MissingInSf(nomState.id()));
        }

        List<DiffDetail> differences = new ArrayList<>();
        differences.addAll(compareName(nomState, sfEnheter));
        differences.addAll(compareNivaa(nomState, sfEnheter));
        differences.addAll(compareType(nomState, sfEnheter));
        differences.addAll(compareTilgangsenhet(nomState, sfEnheter));
//        differences.addAll(compareKostnadssted(nomState, sfEnheter));   TODO: Denne kan ikke brukes før oppgave ORG-3064 er utført
        differences.addAll(compareLedere(nomState, sfEnheter));
        differences.addAll(compareOrganisering(nomState, sfEnheter));
        differences.addAll(compareSFEnhetsObjekt(nomState, sfEnheter));

        return differences;
    }

    List<DiffDetail> compareNivaa(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        String derivedNivaa = getDerivedNivaa(nomState).name();

        return sfEnheter.stream()
            .<DiffDetail>mapMulti((sfEnhet, diffs) -> {
                switch (sfEnhet.cust_nom_nivaa().orElse(null)) {
                    case null ->
                        diffs.accept(new NomNivaaMissingInSF(sfEnhet.startDate(), derivedNivaa));
                    case String nivaa when !nivaa.equals(derivedNivaa) ->
                        diffs.accept(new NomNivaaMismatch(sfEnhet.startDate(), nivaa, derivedNivaa));
                    default -> {}
                }
            })
            .toList();
    }

    List<DiffDetail> compareName(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        return sfEnheter.stream()
            .<DiffDetail>mapMulti((sfEnhet, diffs) -> {
                switch(sfEnhet.name()) {
                    case null ->
                        diffs.accept(new NameMissingInSF(sfEnhet.startDate(), nomState.navn()));
                    case String name when name.isBlank()
                        -> diffs.accept(new NameMissingInSF(sfEnhet.startDate(), nomState.navn()));
                    case String name when !name.equals(nomState.navn()) ->
                        diffs.accept(new NameMismatch(sfEnhet.startDate(), name, nomState.navn()));
                    default -> {}
                }
            })
            .toList();
    }

    List<DiffDetail> compareType(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        return sfEnheter.stream()
            .<DiffDetail>mapMulti((sfEnhet, diffs) -> {
                switch(sfEnhet.cust_orgenhet_type().orElse(null)) {
                    case null ->
                        diffs.accept(new NomTypeMissingInSF(sfEnhet.startDate(), nomState.orgEnhetsType().name()));
                    case String type when type.isBlank()
                        -> diffs.accept(new NomTypeMissingInSF(sfEnhet.startDate(), nomState.orgEnhetsType().name()));
                    case String type when !type.equals(nomState.orgEnhetsType().name()) ->
                        diffs.accept(new NomTypeMismatch(sfEnhet.startDate(), type, nomState.orgEnhetsType().name()));
                    default -> {}
                }
            })
            .toList();
    }

    List<DiffDetail> compareTilgangsenhet(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        return sfEnheter.stream()
            .<DiffDetail>mapMulti((sfEnhet, diffs) -> {
                switch(sfEnhet.cust_Tilgangsenhet().orElse(null)) {
                    case null ->
                        diffs.accept(new NomTilgangsenhetMissingInSF(sfEnhet.startDate(), nomState.tilgangsenhetId()));
                    case String tilgangsenhet when tilgangsenhet.isBlank()
                        -> diffs.accept(new NomTilgangsenhetMissingInSF(sfEnhet.startDate(), nomState.tilgangsenhetId()));
                    case String tilgangsenhet when !tilgangsenhet.equals(nomState.tilgangsenhetId()) ->
                        diffs.accept(new NomTilgangsenhetMismatch(sfEnhet.startDate(), tilgangsenhet, nomState.tilgangsenhetId()));
                    default -> {}
                }
            })
            .toList();
    }

    /* TODO: Denne kan ikke brukes før oppgave ORG-3064 er utført
    List<DiffDetail> compareKostnadssted(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        return sfEnheter.stream()
            .<DiffDetail>mapMulti((sfEnhet, diffs) -> {
                switch(sfEnhet.costCenter().orElse(null)) {
                    case null ->
                        diffs.accept(new KostnadsstedMissingInSF(sfEnhet.startDate(), nomState.kostnadsted()));
                    case String kostnadsted when kostnadsted.isBlank()
                        -> diffs.accept(new KostnadsstedMissingInSF(sfEnhet.startDate(), nomState.kostnadsted()));
                    case String KOSTNADSTED when !KOSTNADSTED.equals(nomState.kostnadsted()) ->
                        diffs.accept(new KostnadsstedMismatch(sfEnhet.startDate(), KOSTNADSTED, nomState.kostnadsted()));
                    default -> {}
                }
            })
            .toList();
    }
     */

    List<DiffDetail> compareLedere(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter)  {
        sfEnheter = sfEnheter.stream().sorted(Comparator.comparing(SfOrgEnhet::startDate)).toList();
        var grenser = new TreeSet<LocalDate>();

        var nomLedere = orgEnhetLederStateRepository.findAllByOrgEnhetId(nomState.id()).stream()
            .filter(leder -> leder.sluttdato() == null || !leder.sluttdato().isBefore(historicalCutoffDate))
            .sorted(Comparator.comparing(OrgEnhetLederState::startdato))
            .toList();

        var foersteSfDato = sfEnheter.getFirst().startDate(); // sortert liste
        var fraDato = foersteSfDato.isAfter(historicalCutoffDate)
            ? foersteSfDato
            : historicalCutoffDate;
        grenser.add(fraDato);

        sfEnheter.stream().map(SfOrgEnhet::startDate).filter(d -> d.isAfter(fraDato)).forEach(grenser::add);
        var minSfDate = grenser.stream().min(Comparator.naturalOrder()).orElse(historicalCutoffDate);
        nomLedere.stream().map(OrgEnhetLederState::startdato).filter(d -> !d.isBefore(minSfDate)).forEach(grenser::add);

        nomLedere.stream().map(OrgEnhetLederState::sluttdato).filter(Objects::nonNull)
            .map(d -> d.plusDays(1)).filter(d -> !d.isBefore(minSfDate)).forEach(grenser::add); // Ta med sluttdatoer + 1 in case NOM har hull i periodene

        var diffs = new ArrayList<DiffDetail>();
        for (var dato : grenser) {
            var sfEnhet = sfEnheter.stream() // Siste enhet med startdato <= dato
                .filter(sf -> !sf.startDate().isAfter(dato))
                .reduce((_, neste) -> neste)
                .orElse(null);
            var nomLeder = nomLedere.stream() // siste nomLeder med periode som dekker dato. Kan være null hvis NOM har hull i periodene
                .filter(leder -> !leder.startdato().isAfter(dato)
                    && (leder.sluttdato() == null || !leder.sluttdato().isBefore(dato)))
                .reduce((_, neste) -> neste)
                .orElse(null);

            var sfIdent = sfEnhet == null ? null : sfEnhet.cust_LederNavIdent().orElse(null);
            var nomIdent = nomLeder == null ? null : nomLeder.navident();

            if (nomLeder == null) {
                diffs.add(new LederMissingInNOM(dato, sfIdent));
            }
            if (sfIdent == null || sfIdent.isBlank()) {
                diffs.add(new LederMissingInSF(dato, nomIdent));
            }
            if (sfIdent != null && nomIdent != null && !sfIdent.equals(nomIdent)) {
                diffs.add(new LederMismatchDetail(dato, sfIdent, nomIdent));
            }
        }
        return diffs;
    }

    List<DiffDetail> compareOrganisering(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        sfEnheter = sfEnheter.stream().sorted(Comparator.comparing(SfOrgEnhet::startDate)).toList();
        var grenser = new TreeSet<LocalDate>();

        var nomOrganiseringer = organiseringStateRepository.findById(nomState.id()).stream()
            .filter(o -> o.gyldigTom() == null || !o.gyldigTom().isBefore(historicalCutoffDate))
            .sorted(Comparator.comparing(OrganiseringState::gyldigFom))
            .toList();

        var foersteSfDato = sfEnheter.getFirst().startDate(); // sortert liste
        var fraDato = foersteSfDato.isAfter(historicalCutoffDate)
            ? foersteSfDato
            : historicalCutoffDate;
        grenser.add(fraDato);

        sfEnheter.stream().map(SfOrgEnhet::startDate).filter(d -> d.isAfter(fraDato)).forEach(grenser::add);
        var minSfDate = grenser.stream().min(Comparator.naturalOrder()).orElse(historicalCutoffDate);

        nomOrganiseringer.stream().map(OrganiseringState::gyldigFom).filter(d -> !d.isBefore(minSfDate)).forEach(grenser::add);
        nomOrganiseringer.stream().map(OrganiseringState::gyldigTom).filter(Objects::nonNull)
            .map(d -> d.plusDays(1)).filter(d -> !d.isBefore(minSfDate)).forEach(grenser::add); // Ta med sluttdatoer + 1 in case NOM har hull i periodene

        var diffs = new ArrayList<DiffDetail>();
        for (var dato : grenser) {
            var sfEnhet = sfEnheter.stream() // Siste enhet med startdato <= dato
                .filter(sf -> !sf.startDate().isAfter(dato))
                .reduce((_, neste) -> neste)
                .orElse(null);
            var nomOrganisering = nomOrganiseringer.stream() // siste nomLeder med periode som dekker dato. Kan være null hvis NOM har hull i periodene
                .filter(o -> !o.gyldigFom().isAfter(dato)
                    && (o.gyldigTom() == null || !o.gyldigTom().isBefore(dato)))
                .reduce((_, neste) -> neste)
                .orElse(null);

            var sfParentId = sfEnhet == null ? null :
                switch (sfEnhet) {
                    case SfDepartment d -> //noinspection DataFlowIssue
                         d.parentEnhet() == null ? null : d.parentEnhet().externalCode();
                    case SfDivision d -> //noinspection DataFlowIssue
                        d.parentEnhet() == null ? null : d.parentEnhet().externalCode();
                    case SfBusinessUnit _ -> toppnodeId; // Vi forventer at alle FOBusinessUnits har NAV som toppnode
                };

            var nomParentId = nomOrganisering == null ? null : nomOrganisering.idOver();

            if (nomParentId == null) {
                diffs.add(new ParentMissingInNOM(dato, sfParentId));
            }
            if (sfParentId == null || sfParentId.isBlank()) {
                diffs.add(new ParentMissingInSF(dato, nomParentId));
            }
            if (sfParentId != null && nomParentId != null && !sfParentId.equals(nomParentId)) {
                diffs.add(new ParentMismatchDetail(dato, sfParentId, nomParentId));
            }
        }
        return diffs;
    }

    List<DiffDetail> compareSFEnhetsObjekt(OrgEnhetState nomState, List<SfOrgEnhet> sfEnheter) {
        var derivedNomNivaa = getDerivedNivaa(nomState);

        // TODO: Fikses når 'derive nom nivaa' er implementert, og evt når nomState.erLovenode() er implementert.
        var expectedSFEnhetsObjekt = derivedNomNivaa == NomNivaa.LINJEENHET ? SfOrgEnhetType.BUSINESS_UNIT
            : derivedNomNivaa.name().equals("LOVNODE") ? SfOrgEnhetType.DEPARTMENT : SfOrgEnhetType.DIVISION;

        return sfEnheter.stream()
            .<DiffDetail>mapMulti((sfEnhet, diffs) -> {
                switch(sfEnhet) {
                    case SfBusinessUnit _ when expectedSFEnhetsObjekt != SfOrgEnhetType.BUSINESS_UNIT
                        -> diffs.accept(new SFEnhetsObjektMismatch(sfEnhet.startDate(), SfOrgEnhetType.BUSINESS_UNIT, expectedSFEnhetsObjekt));
                    case SfDivision _ when expectedSFEnhetsObjekt != SfOrgEnhetType.DIVISION
                        -> diffs.accept(new SFEnhetsObjektMismatch(sfEnhet.startDate(), SfOrgEnhetType.DIVISION, expectedSFEnhetsObjekt));
                    case SfDepartment _ when expectedSFEnhetsObjekt != SfOrgEnhetType.DEPARTMENT
                        -> diffs.accept(new SFEnhetsObjektMismatch(sfEnhet.startDate(), SfOrgEnhetType.DEPARTMENT, expectedSFEnhetsObjekt));
                    default -> {}
                }
            })
            .toList();
    }

    // SF vil når de mottar endret Nivå - lage ett 'innstikk' på dagens dato
    // Så denne metoden sjekker kun på SF enheter versjoner som dekker fra og med historisk cutoff dato.
    List<SfOrgEnhet> versjonerForBehandling(List<SfOrgEnhet> sfEnheter) {
        var minStartDate = sfEnheter.stream()
            .map(SfOrgEnhet::startDate)
            .filter(startDate -> !startDate.isAfter(historicalCutoffDate))
            .max(Comparator.naturalOrder())
            .orElse(historicalCutoffDate);

        return
             sfEnheter.stream()
                .filter(sfe -> !sfe.startDate().isBefore(minStartDate))
                .sorted(Comparator.comparing(SfOrgEnhet::startDate))
                .toList();
    }

    // Trur denne er implementert av Hama i - generer Orgenhet payload til SF
    // TODO: Følg opp dette og benytt samme metode/oppslag
    NomNivaa getDerivedNivaa(OrgEnhetState nomState) {
        return nomState.nomNivaa();
    }
}
