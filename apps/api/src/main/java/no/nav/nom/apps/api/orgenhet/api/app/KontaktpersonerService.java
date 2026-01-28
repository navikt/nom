package no.nav.nom.apps.api.orgenhet.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.clients.kontaktinfo.Kontaktinfo;
import no.nav.nom.apps.api.clients.kontaktinfo.KontaktinfoClient;
import no.nav.nom.apps.api.clients.kontaktinfo.RessursKontaktinfo;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.orgenhet.exceptions.OrgEnhetNotFoundException;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.ressurs.api.app.RessursRestService;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.telefon.TelefonService;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.utils.common.excel.ExcelGenerator;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.common.AppConfig.TEAM_LOGS;

@Slf4j
@Service
public class KontaktpersonerService {

    private final OrgenhetService orgenhetService;
    private final OrgEnhetMapper orgEnhetMapper;
    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final RessursRestService ressursRestService;
    private final KontaktinfoClient kontaktinfoClient;
    private final ExcelGenerator excelGenerator;
    private final TelefonService telefonService;

    public KontaktpersonerService(KontaktinfoClient kontaktinfoClient,
                                  OrgenhetService orgenhetService,
                                  OrgEnhetMapper orgEnhetMapper,
                                  OrgtilknytningStateRepository orgtilknytningStateRepository,
                                  RessursRestService ressursRestService, TelefonService telefonService
    ) {
        this.kontaktinfoClient = kontaktinfoClient;
        this.orgenhetService = orgenhetService;
        this.orgEnhetMapper = orgEnhetMapper;
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.ressursRestService = ressursRestService;
        this.telefonService = telefonService;
        this.excelGenerator = new  ExcelGenerator();
    }

    public void generateExcelFileToServletResponse(HttpServletResponse response, OrgEnhet orgEnhet, List<Ressurs> sortedRessurser) throws IOException {
        var kontaktinfoForRessurser = getKontaktinfoForRessurser(sortedRessurser, orgEnhet.id());

        Map<String, Ressurs> ressursPerNavident = sortedRessurser.stream()
            .collect(Collectors.toMap(r -> r.state().navident(), r -> r));

        log.debug(
            TEAM_LOGS,
            "Generating excel for orgenhet {} with ressurser {}, and contantinfo {}",
            orgEnhet.id(),
            sortedRessurser,
            kontaktinfoForRessurser
        );

        List<Map<String, Object>> ressurserOgKontaktinfoMapList = kontaktinfoToMapList(kontaktinfoForRessurser, ressursPerNavident);

        try {
            var headers = ressurserOgKontaktinfoMapList.getFirst().keySet().stream().toList();
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=MedarbeidereOgKontaktpersoner.xlsx");
            excelGenerator.generateWorkbook(
                response,
                "MedarbeidereOgKontaktpersoner",
                headers,
                ressurserOgKontaktinfoMapList
            );

        } catch (Exception e) {
            log.error("Error generating Excel file for Medarbeidere og kontaktpersoner", e);
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error generating Excel file");
        }
    }


    public void generateExcelFile(HttpServletResponse response, String orgenhetId) throws IOException {
        var orgEnhet = orgenhetService.getById(orgenhetId)
            .map(orgEnhetMapper::mapToOrgEnhet);

        if (orgEnhet.isEmpty()) {
            String notFoundError = "Orgenhet not found while generating Excel file: " + orgenhetId;
            log.error(notFoundError);
            throw new OrgEnhetNotFoundException(notFoundError);
        } else {
            var sortedRessurser = getSortedRessurserInAndUnderliggendeOrgenheter(orgenhetId);

            log.debug(TEAM_LOGS, "Ressurser retrieved for orgenhet {}: {}", orgenhetId, sortedRessurser);
            log.debug("Ressurser retrieved for orgenhet {}: {}", orgenhetId, sortedRessurser.size());

            generateExcelFileToServletResponse(response, orgEnhet.get(), sortedRessurser);

        }
    }

    public List<RessursKontaktperson> getKontaktpersonerForRessurserUnder(String orgenhetId) {
        var orgEnhet = orgenhetService.getById(orgenhetId)
            .map(orgEnhetMapper::mapToOrgEnhet);

        if (orgEnhet.isEmpty()) {
            String notFoundError = "Orgenhet not found while retrieving kontaktinfo for ressurser: " + orgenhetId;
            log.error(notFoundError);
            throw new OrgEnhetNotFoundException(notFoundError);
        }

        var sortedRessurser = getSortedRessurserInAndUnderliggendeOrgenheter(orgenhetId);
        var ressursMap = sortedRessurser.stream().collect(Collectors.toMap(r -> r.state().navident(), Function.identity()));
        var ressursKontaktinfo = getKontaktinfoForRessurser(sortedRessurser, orgEnhet.get().id());

        return ressursKontaktinfo.stream().map(kontaktinfo ->
            RessursKontaktperson.from(
                kontaktinfo,
                ressursMap.get(kontaktinfo.ressursNavident()).state().visningsnavn(),
                telefonService.getPrioritertTelefonnummer(kontaktinfo.ressursNavident()),
                getPrimaryTelefonForRessurs(ressursMap.get(kontaktinfo.ressursNavident()))
            )
        ).flatMap(List::stream).toList();
    }


    public void generate(HttpServletResponse response, OrgEnhet orgEnhet, List<Ressurs> sortedRessurser) {
        log.info(TEAM_LOGS, "Getting content info for ressurser: {}", sortedRessurser);
        log.info("Getting content info for {} ressurser", sortedRessurser.size());
        List<RessursKontaktinfo> kontaktinfoForRessurser;

        // try to get the contact info for the ressurser
        try {
            kontaktinfoForRessurser = kontaktinfoClient
                .getKontaktinfoForRessurser(
                    sortedRessurser.stream().map(r -> r.state().navident()).toList(),
                    orgEnhet.id()
                );
            log.info(TEAM_LOGS, "Received kontaktinfo for this orgenhet: {}-{}", orgEnhet.id(), kontaktinfoForRessurser);
            log.info("Received {} kontaktinfo for this orgenhet: {}", kontaktinfoForRessurser.size(), orgEnhet.id());
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) {
                throw e;
            } else {
                log.error("Failed to get kontaktinfo for orgenhet {}", orgEnhet.id(), e);
                throw new RuntimeException(e.getMessage());
            }
        }

        Map<String, Ressurs> ressursPerNavident = sortedRessurser.stream()
            .collect(Collectors.toMap(r -> r.state().navident(), r -> r));

        // generate excel
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Kontaktpersoner - " + orgEnhet.navn());
            var rowcount = 0;

            // index -1 is for the header row
            addRow(sheet.createRow(rowcount++), null, null, -1);

            log.info(
                TEAM_LOGS,
                "Generating excel for orgenhet {} with ressurser {}, and contantinfo {}",
                orgEnhet.id(),
                sortedRessurser,
                kontaktinfoForRessurser
            );
            for (RessursKontaktinfo info : kontaktinfoForRessurser) {

                Ressurs ressurs = ressursPerNavident.get(info.ressursNavident());

                // index determines the row number for ressurs.
                // If there are multiple contact info for a ressurs, we will have multiple rows for that ressurs.
                // However, medarbeider info will be displayed only once for that ressurs.
                int index = 0;
                if (info.kontaktinfo().isEmpty()) {
                    addRow(
                        sheet.createRow(rowcount++),
                        ressurs, null,
                        index
                    );
                } else {
                    for (Kontaktinfo k : info.kontaktinfo()) {
                        addRow(
                            sheet.createRow(rowcount++),
                            ressurs, k,
                            index
                        );
                        index++;
                    }
                }
            }

            var headerRow = sheet.getRow(0);
            var font = workbook.createFont();
            var style = workbook.createCellStyle();
            font.setBold(true);
            style.setFont(font);
            headerRow.cellIterator().forEachRemaining(cell -> cell.setCellStyle(style));
            sheet.createFreezePane(0, 1);

            workbook.write(response.getOutputStream());
            log.info("Generated contact info excel for orgenhet {}", orgEnhet.id());
        } catch (Exception e) {
            log.error("Error generating excel file. ", e);
            throw new RuntimeException(e.getMessage());
        }
    }

    private List<RessursKontaktinfo> getKontaktinfoForRessurser(List<Ressurs> ressurser, String orgenhetId) {
        List<RessursKontaktinfo> kontaktinfoForRessurser;

        // Try to get the contact info for ressurser
        try {
            kontaktinfoForRessurser = kontaktinfoClient
                .getKontaktinfoForRessurser(
                    ressurser.stream().map(r -> r.state().navident()).toList(),
                    orgenhetId
                );
            log.info(TEAM_LOGS, "Received kontaktinfo for orgenhet {}: {}", orgenhetId, kontaktinfoForRessurser);
            log.info("Received {} kontaktinfo for orgenhet {}", kontaktinfoForRessurser.size(), orgenhetId);
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) {
                throw e;
            } else {
                log.error("Failed to get kontaktinfo for orgenhet {}", orgenhetId, e);
                throw new RuntimeException(e.getMessage());
            }
        }

        return kontaktinfoForRessurser;
    }

    private static void addRow(Row row, Ressurs ressurs, Kontaktinfo kontaktinfo, int index) {
        var medarbeiderNavident = row.createCell(0, CellType.STRING);
        var medarbeiderFornavn = row.createCell(1, CellType.STRING);
        var medarbeiderEtternavn = row.createCell(2, CellType.STRING);
        var kontaktpersonNavn = row.createCell(3, CellType.STRING);
        var kontaktpersonRelasjon = row.createCell(4, CellType.STRING);
        var kontaktpersonTelefonnr = row.createCell(5, CellType.STRING);
        var kontaktpersonProritet = row.createCell(6, CellType.STRING);

        if (ressurs == null || index == -1) {
            medarbeiderNavident.setCellValue("Medarbeider-navident");
            medarbeiderFornavn.setCellValue("Medarbeider-fornavn");
            medarbeiderEtternavn.setCellValue("Medarbeider-etternavn");
            kontaktpersonNavn.setCellValue("Kontaktperson-navn");
            kontaktpersonRelasjon.setCellValue("Kontaktperson-relasjon");
            kontaktpersonTelefonnr.setCellValue("Kontaktperson-telefonnr");
            kontaktpersonProritet.setCellValue("Kontaktperson-prioritet");
        } else {
            // if index is 0, then we will display the medarbeider info in the first row for that ressurs.
            if (index == 0) {
                medarbeiderNavident.setCellValue(ressurs.state().navident());
                medarbeiderFornavn.setCellValue(ressurs.state().visningsnavn().fornavn());
                medarbeiderEtternavn.setCellValue(ressurs.state().visningsnavn().etternavn());
            }

            // if index is >=0, then we will display the kontaktinfo for that ressurs.
            if (index >= 0 && kontaktinfo != null) {
                kontaktpersonNavn.setCellValue(kontaktinfo.fornavn() + " " + kontaktinfo.etternavn());
                kontaktpersonRelasjon.setCellValue(kontaktinfo.relasjon() != null ? kontaktinfo.relasjon() : "");
                kontaktpersonTelefonnr.setCellValue(kontaktinfo.telefon() != null ? kontaktinfo.telefon() : "");
                kontaktpersonProritet.setCellValue(kontaktinfo.prioritet() != null ? kontaktinfo.prioritet().toString() : "");
            }
        }
    }

    public List<Map<String, Object>> kontaktinfoToMapList(List<RessursKontaktinfo> ressursKontaktinfoListe, Map<String, Ressurs> ressurser) {
        List<Map<String, Object>> result = new ArrayList<>();

        for (RessursKontaktinfo ressursKontaktinfo : ressursKontaktinfoListe) {
            Optional<Visningsnavn> visningsnavn = Optional.of(ressurser.get(ressursKontaktinfo.ressursNavident()))
                .map(Ressurs::state)
                .map(RessursState::visningsnavn);

            String ressursFornavn = visningsnavn.map(Visningsnavn::fornavn).orElse("");
            String ressursEtternavn = visningsnavn.map(Visningsnavn::etternavn).orElse("");

            String ressursJobbtelefon = telefonService.getPrioritertTelefonnummer(ressursKontaktinfo.ressursNavident());
            String ressursPrimarytelefon = getPrimaryTelefonForRessurs(ressurser.get(ressursKontaktinfo.ressursNavident()));

            if (ressursKontaktinfo.kontaktinfo().isEmpty()) {
                LinkedHashMap<String, Object> map = new LinkedHashMap<>();

                map.put("Medarbeider-navident", ressursKontaktinfo.ressursNavident());
                map.put("Medarbeider-fornavn", ressursFornavn);
                map.put("Medarbeider-etternavn", ressursEtternavn);

                map.put("Medarbeider-jobbtelefon", ressursJobbtelefon);
                map.put("Medarbeider-privattelefon", ressursPrimarytelefon);

                map.put("Kontaktperson-navn", "");
                map.put("Kontaktperson-relasjon", "");
                map.put("Kontaktperson-telefonnr", "");
                map.put("Kontaktperson-prioritet", "");

                result.add(map);
            } else {
                for (Kontaktinfo kontaktinfo : ressursKontaktinfo.kontaktinfo()) {
                    LinkedHashMap<String, Object> map = new LinkedHashMap<>();

                    map.put("Medarbeider-navident", ressursKontaktinfo.ressursNavident());
                    map.put("Medarbeider-fornavn", ressursFornavn);
                    map.put("Medarbeider-etternavn", ressursEtternavn);

                    map.put("Medarbeider-jobbtelefon", ressursJobbtelefon);
                    map.put("Medarbeider-privattelefon", ressursPrimarytelefon);

                    map.put("Kontaktperson-navn", kontaktpersonNavn(kontaktinfo));
                    map.put("Kontaktperson-relasjon", kontaktinfo.relasjon() != null ? kontaktinfo.relasjon() : "");
                    map.put("Kontaktperson-telefonnr", kontaktinfo.telefon() != null ? kontaktinfo.telefon() : "");
                    map.put("Kontaktperson-prioritet", kontaktinfo.prioritet() != null ? kontaktinfo.prioritet() : "");

                    result.add(map);
                }
            }
        }

        return result;
    }

    private String getPrimaryTelefonForRessurs(Ressurs ressurs) {
        if (ressurs == null || ressurs.state() == null || ressurs.state().primaryTelefonnummer() == null) {
            return "";
        }
        return ressurs.state().primaryTelefonnummer();
    }

    private String kontaktpersonNavn(Kontaktinfo kontaktinfo) {
        StringBuilder sb = new StringBuilder();
        if (kontaktinfo.fornavn() != null) {
            sb.append(kontaktinfo.fornavn());
            sb.append(" ");
        }
        if (kontaktinfo.etternavn() != null) {
            sb.append(kontaktinfo.etternavn());
        }
        return sb.toString();
    }

    private List<Ressurs> getSortedRessurserInAndUnderliggendeOrgenheter(String orgenhetId) {
        log.debug("Finner navidenter i og under enhet {}", orgenhetId);
        var underliggendeEnheter = orgenhetService.findAllUnderliggendeOrgenheter(orgenhetId);
        var aktiveEllerFremtidigeNavidenter = orgtilknytningStateRepository.getAll().stream()
            .filter(ot -> ot.orgEnhetId().equals(orgenhetId) || underliggendeEnheter.contains(ot.orgEnhetId()))
            .filter(PeriodeUtil::isCurrentOrFuturePeriod)
            .map(OrgtilknytningState::navident)
            .toList();

        log.debug("Henter {} ressurser og sorterer", aktiveEllerFremtidigeNavidenter.size());
        var ressurser = ressursRestService.findRessurserByNavident(aktiveEllerFremtidigeNavidenter);

        // sort ressurser by fornavn and etternavn
        return ressurser.stream().sorted((r1, r2) -> {
            var n1 = r1.state().visningsnavn();
            var n2 = r2.state().visningsnavn();
            return n1.fornavn().compareTo(n2.fornavn()) == 0 ? n1.etternavn().compareTo(n2.etternavn()) : n1.fornavn().compareTo(n2.fornavn());
        }).toList();
    }
}
