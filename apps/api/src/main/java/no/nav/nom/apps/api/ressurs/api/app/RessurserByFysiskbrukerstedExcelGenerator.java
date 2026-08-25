package no.nav.nom.apps.api.ressurs.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.utils.common.excel.ExcelGenerator;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;

@Slf4j
@Component
public class RessurserByFysiskbrukerstedExcelGenerator {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursContactDetailsResolver ressursContactDetailsResolver;
    private final ExcelGenerator excelGenerator;

    private static final Map<String, String> sektorMap = Map.of(
        "NAV_STATLIG", "Statlig",
        "NAV_KOMMUNAL", "Kommunal",
        "EKSTERN", "Ekstern"
    );

    public RessurserByFysiskbrukerstedExcelGenerator(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        RessursContactDetailsResolver ressursContactDetailsResolver
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.ressursContactDetailsResolver = ressursContactDetailsResolver;
        this.excelGenerator = new ExcelGenerator();
    }

    public void generate(HttpServletResponse response, List<RessursState> ressurser, String fysiskBrukerstedNr) throws IOException {
        var navidenter = ressurser.stream().map(RessursState::navident).toList();
        var otMap = orgtilknytningStateRepository.findByNavidentIn(navidenter).stream()
            .filter(PeriodeUtil::isCurrentOrFuturePeriod)
            .collect(Collectors.groupingBy(
                OrgtilknytningState::navident
            ));

        var orgenhetNavnMap = orgEnhetStateRepository.findByIdIn(
            otMap.values().stream()
                .flatMap(List::stream)
                .map(OrgtilknytningState::orgEnhetId)
                .collect(Collectors.toSet())
        ).stream().collect(Collectors.toMap(OrgEnhetState::id, orgEnhet -> orgEnhet));

        var eposter = ressursContactDetailsResolver.fetchEmails(navidenter);

        List<Map<String, Object>> result = new ArrayList<>();
        for (RessursState ressurs : ressurser) {
            var entry = otMap.get(ressurs.navident());
            if (entry == null || entry.isEmpty()) {
                log.error("Ressurs {} har ingen OTer.", ressurs.navident());
                result.add(generateRow(
                        ressurs,
                        "UKJENT",
                        "UKJENT",
                        "UKJENT",
                        "UKJENT",
                        null,
                        eposter.get(ressurs.navident()).orElse("UKJENT")
                    )
                );
            } else {
                var gyldigFomCandidates = entry.stream().map(OrgtilknytningState::gyldigFom).filter(Objects::nonNull).toList();
                var gyldigFom = Collections.min(gyldigFomCandidates);

                var orgEnheterIdOgState = otMap.get(ressurs.navident()).stream()
                    .map(OrgtilknytningState::orgEnhetId)
                    .distinct()
                    .collect(Collectors.toMap(orgEnhetId -> orgEnhetId, orgenhetNavnMap::get));

                for (Map.Entry<String, OrgEnhetState> orgEnhetEntry : orgEnheterIdOgState.entrySet()) {
                    result.add(generateRow(ressurs, orgEnhetEntry.getKey(), orgEnhetEntry.getValue().agressoId(), orgEnhetEntry.getValue().navn(), orgEnhetEntry.getValue().tilgangsenhetId(), gyldigFom, eposter.get(ressurs.navident()).orElse("UKJENT")));
                }
            }
        }

        var sortedResult = result.stream()
            .sorted(Comparator.comparing((Map<String, Object> map) -> (String) map.get("Organisasjonsenhet Navn"))
                .thenComparing((Map<String, Object> map) -> (String) map.get("Etternavn")))
            .toList();

        var excelHeaders = sortedResult.getFirst().keySet().stream().toList();
        var filename = String.format(
            "Ressurser med fysiskbrukersted %s-%s",
            fysiskBrukerstedNr,
            LocalDate.now().format(DateTimeFormatter.ofPattern("dd-MM-yyyy"))
        );
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=%s.xlsx", filename));
        excelGenerator.generateWorkbook(response, "Fysisk brukersted " + fysiskBrukerstedNr, excelHeaders, sortedResult);
    }

    private Map<String, Object> generateRow(RessursState ressurs, String orgEnhetId, String agressoId, String orgEnhetNavn, String tilgangsenhet, LocalDate gyldigFom, String epost) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        map.put("Navident",                 ressurs.navident());
        map.put("Etternavn",                ressurs.visningsnavn().etternavn());
        map.put("Fornavn",                  ressurs.visningsnavn().fornavn());
        map.put("Mellomnavn",               ressurs.visningsnavn().mellomnavn().orElse(null));
        map.put("Sektor",                   isNull(ressurs.sektor()) ? "UKJENT" : sektorMap.get(ressurs.sektor().name()));
        map.put("Organisasjonsenhet ID",    requireNonNullElse(orgEnhetId, "UKJENT"));
        map.put("Agresso ID",               requireNonNullElse(agressoId, "UKJENT"));
        map.put("Organisasjonsenhet Navn",  requireNonNullElse(orgEnhetNavn, "UKJENT"));
        map.put("Tilgangsenhet",            requireNonNullElse(tilgangsenhet, "UKJENT"));
        map.put("Fra og med",               isNull(gyldigFom) ? "UKJENT" : gyldigFom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
        map.put("Jobbtelefon",              isNull(ressurs.jobbtelefon()) ? "" : ressurs.jobbtelefon().nummer());
        map.put("Privat telefon",           requireNonNullElse(ressurs.primaryTelefonnummer(), ""));
        map.put("Epost",                    epost);
        return map;
    }
}
