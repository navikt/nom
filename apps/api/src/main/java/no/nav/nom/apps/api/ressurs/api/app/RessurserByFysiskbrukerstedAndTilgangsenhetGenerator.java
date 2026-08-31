package no.nav.nom.apps.api.ressurs.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgenhetleder.OrgEnhetLederService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.FysiskBrukerstedService;
import no.nav.nom.apps.api.ressurs.model.FysiskBrukersted;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import no.nav.nom.libs.utils.common.excel.ExcelGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;
import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Component
public class RessurserByFysiskbrukerstedAndTilgangsenhetGenerator {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursContactDetailsResolver ressursContactDetailsResolver;
    private final ExcelGenerator excelGenerator;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;
    private final OrgEnhetLederService orgEnhetLederService;
    private final RessursStateRepository ressursStateRepository;
    private final FysiskBrukerstedService fysiskBrukerstedService;

    public RessurserByFysiskbrukerstedAndTilgangsenhetGenerator(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursContactDetailsResolver ressursContactDetailsResolver,
        OrgtilknytningService orgtilknytningService,
        OrgenhetService orgenhetService,
        OrgEnhetLederService orgEnhetLederService,
        RessursStateRepository ressursStateRepository,
        FysiskBrukerstedService fysiskBrukerstedService
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursContactDetailsResolver = ressursContactDetailsResolver;
        this.excelGenerator = new ExcelGenerator();
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
        this.orgEnhetLederService = orgEnhetLederService;
        this.ressursStateRepository = ressursStateRepository;
        this.fysiskBrukerstedService = fysiskBrukerstedService;
    }

    public List<RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse> getRessurserMedFysiskBrukerstedOgTilgangsEnhetIAktivOrgtilknytning(
        RessurserByFysiskBrukerstedAndTilgangsenhetIdRequest ressurserByFysiskBrukerstedAndTilgangsenhetIdRequest
    ) {
        var orgEnhetStates = orgenhetService.findAllWithTilgangsenhetId(ressurserByFysiskBrukerstedAndTilgangsenhetIdRequest.tilgangsenhetId());
        var orgEnhetStateIds = orgEnhetStates.stream().map(OrgEnhetState::id).collect(Collectors.toSet());

        var orgEnhetLedere = orgEnhetLederService.getAktivLederForOrgEnhetIds(orgEnhetStateIds);

        Map<String, RessursState> ressursStateLederMap = new HashMap<>();

        orgEnhetLedere.forEach((orgEnhetId, navident) -> ressursStateLederMap.put(orgEnhetId, ressursStateRepository.findByNavident(navident).orElse(null)));

        var orgtilknytningStates = orgtilknytningService.getActiveOrgtilknytningerByFysiskBrukerstedNr(ressurserByFysiskBrukerstedAndTilgangsenhetIdRequest.fysiskBrukerstedNr())
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .filter(orgtilknytningState -> orgEnhetStateIds.contains(orgtilknytningState.orgEnhetId()))
            .toList();

        var ressursStates = ressursStateRepository.findByNavidentBulk(orgtilknytningStates.stream()
            .map(OrgtilknytningState::navident)
            .filter(Objects::nonNull)
            .toList());

        var ressurserEpost = ressursContactDetailsResolver.fetchEmails(ressursStates.keySet().stream().toList());

        return orgtilknytningStates.stream().map(orgtilknytningState -> {
            var orgEnhetState = orgEnhetStates.stream()
                .filter(o -> Objects.equals(o.id(), orgtilknytningState.orgEnhetId()))
                .findFirst()
                .orElseThrow(() -> new OrgEnhetNotFoundException(orgtilknytningState.orgEnhetId()));
            var ressursState = ressursStates.get(orgtilknytningState.navident());
            var fysiskBrukerstedNavn = fysiskBrukerstedService.findFysiskBrukersted(orgtilknytningState.primaryFysiskBrukerstedNummer())
                .map(FysiskBrukersted::navn)
                .orElse("N/A");
            return create(
                orgtilknytningState,
                ressursState,
                orgEnhetState,
                fysiskBrukerstedNavn,
                ressurserEpost.get(ressursState.navident()).orElse("N/A"),
                ressursStateLederMap.get(orgtilknytningState.orgEnhetId())
            );
        }).toList();
    }

    public List<RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse> getRessurserMedFysiskBrukerstedIAktivOrgtilknytning(
        String fysiskBrukerstedNr
    ) {
        var orgtilknytningStates = orgtilknytningService.getActiveOrgtilknytningerByFysiskBrukerstedNr(fysiskBrukerstedNr)
            .stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .toList();

        var orgEnhetStates = orgEnhetStateRepository.findByIdIn(orgtilknytningStates.stream().map(OrgtilknytningState::orgEnhetId).collect(Collectors.toSet()))
            .stream()
            .collect(Collectors.toMap(OrgEnhetState::id, orgEnhetState -> orgEnhetState));

        var orgEnhetLedere = orgEnhetLederService.getAktivLederForOrgEnhetIds(new HashSet<>(orgEnhetStates.keySet()));

        Map<String, RessursState> ressursStateLederMap = new HashMap<>();

        orgEnhetLedere.forEach((orgEnhetId, navident) -> ressursStateLederMap.put(orgEnhetId, ressursStateRepository.findByNavident(navident).orElse(null)));

        var ressursStates = ressursStateRepository.findByNavidentBulk(orgtilknytningStates.stream()
            .map(OrgtilknytningState::navident)
            .filter(Objects::nonNull)
            .toList());

        var ressurserEpost = ressursContactDetailsResolver.fetchEmails(ressursStates.keySet().stream().toList());

        return orgtilknytningStates.stream().map(orgtilknytningState -> {
            var orgEnhetState = orgEnhetStates.get(orgtilknytningState.orgEnhetId());
            var ressursState = ressursStates.get(orgtilknytningState.navident());
            var fysiskBrukerstedNavn = fysiskBrukerstedService.findFysiskBrukersted(orgtilknytningState.primaryFysiskBrukerstedNummer())
                .map(FysiskBrukersted::navn)
                .orElse("N/A");
            return create(
                orgtilknytningState,
                ressursState,
                orgEnhetState,
                fysiskBrukerstedNavn,
                ressurserEpost.get(ressursState.navident()).orElse("N/A"),
                ressursStateLederMap.get(orgtilknytningState.orgEnhetId())
            );
        }).toList();
    }

    public List<RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse> getRessurserMedTilgangsEnhetIdIAktivOrgtilknytning(
        String tilgangsenhetId
    ) {
        var orgEnhetStates = orgenhetService.findAllWithTilgangsenhetId(tilgangsenhetId);

        var orgEnhetLedere = orgEnhetLederService.getAktivLederForOrgEnhetIds(orgEnhetStates.stream().map(OrgEnhetState::id).collect(Collectors.toSet()));

        Map<String, RessursState> ressursStateLederMap = new HashMap<>();

        orgEnhetLedere.forEach((orgEnhetId, navident) -> ressursStateLederMap.put(orgEnhetId, ressursStateRepository.findByNavident(navident).orElse(null)));

        var orgtilknytningStates = orgtilknytningStateRepository.getOrgtilknytningerPaaEnheter(
            orgEnhetStates.stream()
                .map(OrgEnhetState::id)
                .collect(Collectors.toSet()), OrgtilknytningStateRepository.DOLIs.TRUE
        ).values().stream().flatMap(Collection::stream).toList();

        var ressursStates = ressursStateRepository.findByNavidentBulk(orgtilknytningStates.stream()
            .map(OrgtilknytningState::navident)
            .filter(Objects::nonNull)
            .toList());

        var ressurserEpost = ressursContactDetailsResolver.fetchEmails(ressursStates.keySet().stream().toList());

        return orgtilknytningStates.stream().map(orgtilknytningState -> {
            var orgEnhetState = orgEnhetStates.stream()
                .filter(o -> Objects.equals(o.id(), orgtilknytningState.orgEnhetId()))
                .findFirst()
                .orElseThrow(() -> new OrgEnhetNotFoundException(orgtilknytningState.orgEnhetId()));
            var ressursState = ressursStates.get(orgtilknytningState.navident());
            var fysiskBrukerstedNavn = fysiskBrukerstedService.findFysiskBrukersted(orgtilknytningState.primaryFysiskBrukerstedNummer())
                .map(FysiskBrukersted::navn)
                .orElse("N/A");
            return create(
                orgtilknytningState,
                ressursState,
                orgEnhetState,
                fysiskBrukerstedNavn,
                ressurserEpost.get(ressursState.navident()).orElse("N/A"),
                ressursStateLederMap.get(orgtilknytningState.orgEnhetId())
            );
        }).toList();
    }


    public void generate(
        HttpServletResponse response,
        List<RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse> ressurser,
        String fysiskBrukerstedNr,
        String tilgangsEnhetId
    ) throws IOException {

        List<Map<String, Object>> result = new ArrayList<>();
        for (RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse excelResponse : ressurser) {
            result.add(generateRow(excelResponse));
        }

        var excelHeaders = result.getFirst().keySet().stream().toList();
        String filename;
        if (isBlank(tilgangsEnhetId)) {
            filename = String.format(
                "Ressurser med fysiskbrukersted %s-%s",
                fysiskBrukerstedNr,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            );
        } else if (isBlank(fysiskBrukerstedNr)) {
            filename = String.format(
                "Ressurser med fysiskbrukersted og tilgangsEnhetId %s-%s",
                tilgangsEnhetId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            );
        } else {
            filename = String.format(
                "Ressurser med fysiskbrukersted og tilgangsEnhetId %s-%s-%s",
                fysiskBrukerstedNr,
                tilgangsEnhetId,
                LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            );
        }

        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s.xlsx", filename));
        excelGenerator.generateWorkbook(response, "Fysisk brukersted " + fysiskBrukerstedNr, excelHeaders, result);
    }

    private Map<String, Object> generateRow(
        RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse ressurs
    ) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("Navident", ressurs.navident());
        map.put("Fornavn", ressurs.fornavn());
        map.put("Mellomnavn", ressurs.mellomnavn());
        map.put("Etternavn", ressurs.etternavn());
        map.put("Organisasjonsenhet ID", requireNonNullElse(ressurs.orgEnhetId(), "UKJENT"));
        map.put("Organisasjonsenhet Navn", requireNonNullElse(ressurs.orgEnhetNavn(), "UKJENT"));
        map.put("Tilgangsenhet ID", requireNonNullElse(ressurs.tilgangsenhetId(), "UKJENT"));
        map.put("Fysisk Brukersted Nummer", requireNonNullElse(ressurs.fysiskBrukerstedNr(), "UKJENT"));
        map.put("Fysisk Brukersted Navn", requireNonNullElse(ressurs.fysiskBrukerstedNavn(), "UKJENT"));
        map.put("Jobbtelefon", requireNonNullElse(ressurs.telefonnummer(), "UKJENT"));
        map.put("Sektor", requireNonNullElse(ressurs.sektor(), "UKJENT"));
        map.put("Tilknytnings Type", requireNonNullElse(ressurs.tilknytningsType(), "UKJENT"));
        map.put("Startdato", ressurs.startdato().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        map.put("Epost", ressurs.epost());
        map.put("Leder Navident", requireNonNullElse(ressurs.lederNavident(), "UKJENT"));
        map.put("Leder Navn", requireNonNullElse(ressurs.lederNavn(), "UKJENT"));
        return map;
    }

    private RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse create(
        OrgtilknytningState orgtilknytningState,
        RessursState ressursState,
        OrgEnhetState orgEnhetState,
        String fysiskBrukerstedNavn,
        String ressursEpost,
        RessursState leder
    ) {
        return new RessurserByFysiskBrukerstedAndTilgangsenhetIdExcelResponse(
            orgtilknytningState.navident(),
            ressursState.visningsnavn().fornavn(),
            ressursState.visningsnavn().mellomnavn().orElse(""),
            ressursState.visningsnavn().etternavn(),
            orgtilknytningState.orgEnhetId(),
            orgEnhetState.navn(),
            orgEnhetState.tilgangsenhetId(),
            orgtilknytningState.primaryFysiskBrukerstedNummer(),
            fysiskBrukerstedNavn,
            nonNull(ressursState.jobbtelefon()) ? ressursState.jobbtelefon().nummer() : null,
            ressursState.sektor().name(),
            nonNull(ressursState.tilknytningtype()) ? ressursState.tilknytningtype().name() : null,
            ressursState.startdato(),
            ressursEpost,
            leder.navident(),
            leder.visningsnavn().getFormattedVisningsnavn()
        );
    }
}
