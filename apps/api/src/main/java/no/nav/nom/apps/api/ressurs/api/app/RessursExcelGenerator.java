package no.nav.nom.apps.api.ressurs.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.requireNonNullElse;
import static no.nav.nom.libs.utils.common.util.ObjectUtils.isNotNull;

@Slf4j
@Component
public class RessursExcelGenerator {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgenhetService orgenhetService;

    private static final Map<String, String> sektorMap = Map.of(
        "NAV_STATLIG", "Statlig",
        "NAV_KOMMUNAL", "Kommunal",
        "EKSTERN", "Ekstern"
    );
    private final AuthResolver authResolver;

    public RessursExcelGenerator(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgenhetService orgenhetService,
        AuthResolver authResolver
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgenhetService = orgenhetService;
        this.authResolver = authResolver;
    }

    public void generate(HttpServletResponse response, List<Ressurs> ressurser, List<String> navidenter, String topOrgEnhetId) {
        var kanSeSluttdato = authResolver.kanOppretteRessurs(topOrgEnhetId);
        var underliggendeEnheter = orgenhetService.findAllUnderliggendeOrgenheter(topOrgEnhetId);
        var otMap = orgtilknytningStateRepository.findByNavidentIn(navidenter).stream()
            .filter(ot -> ot.orgEnhetId().equals(topOrgEnhetId) || underliggendeEnheter.contains(ot.orgEnhetId()))
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

        List<ExcelRows> excelRows = new ArrayList<>();

        for (Ressurs ressurs : ressurser) {
            var entry = otMap.get(ressurs.state().navident());
            if (entry == null || entry.isEmpty()) {
                log.error("Ressurs {} har ingen OTer.", ressurs.state().navident());
                excelRows.add(createExcelRow(
                        ressurs,
                        "UKJENT",
                        Optional.of("UKJENT"),
                        "UKJENT",
                        null,
                        kanSeSluttdato
                    )
                );
            } else {
                var gyldigFromCandidates = entry.stream().map(OrgtilknytningState::gyldigFom).filter(Objects::nonNull).toList();
                var gyldigFom = Collections.min(gyldigFromCandidates);

                var orgEnheterIdOgNavn = otMap.get(ressurs.state().navident()).stream()
                    .map(orgtilknytningState -> orgenhetNavnMap.get(orgtilknytningState.orgEnhetId()))
                    .collect(Collectors.toMap(OrgEnhetState::id, orgEnhet -> orgEnhet));

                for (Map.Entry<String, OrgEnhetState> orgEnhetEntry : orgEnheterIdOgNavn.entrySet()) {
                    excelRows.add(createExcelRow(ressurs, orgEnhetEntry.getKey(), orgEnhetEntry.getValue().agressoId(), orgEnhetEntry.getValue().navn(), gyldigFom, kanSeSluttdato));
                }
            }
        }

        excelRows.sort(
            Comparator.comparing(ExcelRows::orgNavn, Comparator.nullsLast(String::compareTo))
                .thenComparing(ExcelRows::etternavn, Comparator.nullsLast(String::compareTo))
        );

        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Ressurser");
            var rowcount = 0;

            addRow(sheet.createRow(rowcount++), null, kanSeSluttdato);

            for (ExcelRows excelRow : excelRows) {
                addRow(
                    sheet.createRow(rowcount++),
                    excelRow,
                    kanSeSluttdato
                );
            }

            sheet.createFreezePane(0, 1);

            XSSFSheet xssfSheet = (XSSFSheet) sheet;
            int lastRow = xssfSheet.getLastRowNum();
            int lastColumn = xssfSheet.getRow(0).getLastCellNum() - 1;

            AreaReference areaRef = new AreaReference(
                new CellReference(0, 0),
                new CellReference(lastRow, lastColumn),
                xssfSheet.getWorkbook().getSpreadsheetVersion()
            );

            XSSFTable table = xssfSheet.createTable(areaRef);
            table.setName("RessursTable");
            table.setDisplayName("RessursTable");

            table.getCTTable().addNewTableStyleInfo().setName("TableStyleMedium4");
            table.setStyleName("TableStyleMedium4");
            table.getCTTable().setTotalsRowShown(false);
            table.getCTTable().getTableStyleInfo().setShowRowStripes(true);
            table.getCTTable().getTableStyleInfo().setShowColumnStripes(false);
            table.getCTTable().addNewAutoFilter();

            for (int col = 0; col <= lastColumn; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(response.getOutputStream());
        } catch (IOException ioException) {
            throw new RuntimeException("IO-Exception", ioException);
        } catch (Exception e) {
            log.error("Feil ved generering av ressurs-Excel", e);
            throw e;
        }
    }

    private static void addRow(Row row, ExcelRows excelRows, boolean kanSeSluttdato) {
        var navident = row.createCell(0, CellType.STRING);
        var etternavn = row.createCell(1, CellType.STRING);
        var fornavn = row.createCell(2, CellType.STRING);
        var mellomnavn = row.createCell(3, CellType.STRING);
        var sektor = row.createCell(4, CellType.STRING);
        var orgId = row.createCell(5, CellType.STRING);
        var agressoId = row.createCell(6, CellType.STRING);
        var orgNavn = row.createCell(7, CellType.STRING);
        var fom = row.createCell(8, CellType.STRING);
        var jobbtelefon = row.createCell(9, CellType.STRING);
        var privatTelefon = row.createCell(10, CellType.STRING);
        var epost = row.createCell(11, CellType.STRING);
        var sluttdato = row.createCell(12, CellType.STRING);


        if (excelRows == null) {
            navident.setCellValue("Navident");
            etternavn.setCellValue("Etternavn");
            fornavn.setCellValue("Fornavn");
            mellomnavn.setCellValue("Mellomnavn");
            sektor.setCellValue("Sektor");
            orgId.setCellValue("Organisasjonsenhet ID");
            agressoId.setCellValue("Agresso ID");
            orgNavn.setCellValue("Organisasjonsenhet Navn");
            fom.setCellValue("Fra og med");
            jobbtelefon.setCellValue("Jobbtelefon");
            privatTelefon.setCellValue("Privat telefon");
            epost.setCellValue("Epost");
            if (kanSeSluttdato) {
                sluttdato.setCellValue("Sluttdato");
            }
        } else {
            navident.setCellValue(excelRows.navident());
            etternavn.setCellValue(excelRows.etternavn());
            fornavn.setCellValue(excelRows.fornavn());
            mellomnavn.setCellValue(excelRows.mellomnavn());
            sektor.setCellValue(excelRows.sektor());
            orgId.setCellValue(excelRows.orgId());
            agressoId.setCellValue(excelRows.agressoId());
            orgNavn.setCellValue(excelRows.orgNavn());
            fom.setCellValue(excelRows.fom());
            jobbtelefon.setCellValue(excelRows.jobbtelefon());
            privatTelefon.setCellValue(excelRows.privatTelefon());
            epost.setCellValue(excelRows.epost());
            sluttdato.setCellValue(excelRows.sluttdato());
        }
    }

    private ExcelRows createExcelRow(Ressurs ressurs, String orgEnhetId, Optional<String> agressoId, String orgEnhetNavn, LocalDate gyldigFom, boolean kanSeSluttdato) {
        return new ExcelRows(
            ressurs.state().navident(),
            ressurs.state().visningsnavn().etternavn(),
            ressurs.state().visningsnavn().fornavn(),
            ressurs.state().visningsnavn().mellomnavn().orElse(null),
            sektorMap.get(ressurs.state().sektor().name()),
            isNotNull(orgEnhetId) ? orgEnhetId : "UKJENT",
            agressoId.orElse(null),
            isNotNull(orgEnhetNavn) ? orgEnhetNavn : "UKJENT",
            isNull(gyldigFom) ? "UKJENT" : gyldigFom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            isNotNull(ressurs.state().jobbtelefon()) ? ressurs.state().jobbtelefon().nummer() : "",
            requireNonNullElse(ressurs.state().primaryTelefonnummer(), ""),
            ressurs.epost(),
            kanSeSluttdato && isNotNull(ressurs.state().sluttdato()) ? ressurs.state().sluttdato().toString() : null
        );
    }

    private record ExcelRows(
        String navident,
        String etternavn,
        String fornavn,
        String mellomnavn,
        String sektor,
        String orgId,
        String agressoId,
        String orgNavn,
        String fom,
        String jobbtelefon,
        String privatTelefon,
        String epost,
        String sluttdato
    ) {}
}
