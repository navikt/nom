package no.nav.nom.apps.api.ressurs.api.app;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.orgtilknytning.dvh.PeriodeUtil;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.contract.nom.state.RessursState;
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
import java.util.stream.Collectors;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNullElse;

@Slf4j
@Component
public class RessurserByFysiskbrukerstedExcelGenerator {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursContactDetailsResolver ressursContactDetailsResolver;

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
    }

    public void generate(HttpServletResponse response, List<RessursState> ressurser) {
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

        List<ExcelRows> excelRows = new ArrayList<>();
        var eposter = ressursContactDetailsResolver.fetchEmails(navidenter);

        for (RessursState ressurs : ressurser) {
            var entry = otMap.get(ressurs.navident());
            if (entry == null || entry.isEmpty()) {
                log.error("Ressurs {} har ingen OTer.", ressurs.navident());
                excelRows.add(createExcelRow(
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
                    excelRows.add(createExcelRow(ressurs, orgEnhetEntry.getKey(), orgEnhetEntry.getValue().agressoId(), orgEnhetEntry.getValue().navn(), orgEnhetEntry.getValue().tilgangsenhetId(), gyldigFom, eposter.get(ressurs.navident()).orElse("UKJENT")));
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

            addRow(sheet.createRow(rowcount++), null);

            for (ExcelRows excelRow : excelRows) {
                addRow(
                    sheet.createRow(rowcount++),
                    excelRow
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

    private static void addRow(Row row, ExcelRows excelRows) {
        var navident = row.createCell(0, CellType.STRING);
        var etternavn = row.createCell(1, CellType.STRING);
        var fornavn = row.createCell(2, CellType.STRING);
        var mellomnavn = row.createCell(3, CellType.STRING);
        var sektor = row.createCell(4, CellType.STRING);
        var orgId = row.createCell(5, CellType.STRING);
        var agressoId = row.createCell(6, CellType.STRING);
        var orgNavn = row.createCell(7, CellType.STRING);
        var tilgangsenhet = row.createCell(8, CellType.STRING);
        var fom = row.createCell(9, CellType.STRING);
        var jobbtelefon = row.createCell(10, CellType.STRING);
        var privatTelefon = row.createCell(11, CellType.STRING);
        var epost = row.createCell(12, CellType.STRING);


        if (excelRows == null) {
            navident.setCellValue("Navident");
            etternavn.setCellValue("Etternavn");
            fornavn.setCellValue("Fornavn");
            mellomnavn.setCellValue("Mellomnavn");
            sektor.setCellValue("Sektor");
            orgId.setCellValue("Organisasjonsenhet ID");
            agressoId.setCellValue("Agresso ID");
            orgNavn.setCellValue("Organisasjonsenhet Navn");
            tilgangsenhet.setCellValue("Tilgangsenhet");
            fom.setCellValue("Fra og med");
            jobbtelefon.setCellValue("Jobbtelefon");
            privatTelefon.setCellValue("Privat telefon");
            epost.setCellValue("Epost");
        } else {
            navident.setCellValue(excelRows.navident());
            etternavn.setCellValue(excelRows.etternavn());
            fornavn.setCellValue(excelRows.fornavn());
            mellomnavn.setCellValue(excelRows.mellomnavn());
            sektor.setCellValue(excelRows.sektor());
            orgId.setCellValue(excelRows.orgId());
            tilgangsenhet.setCellValue(excelRows.tilgangsenhet());
            agressoId.setCellValue(excelRows.agressoId());
            orgNavn.setCellValue(excelRows.orgNavn());
            fom.setCellValue(excelRows.fom());
            jobbtelefon.setCellValue(excelRows.jobbtelefon());
            privatTelefon.setCellValue(excelRows.privatTelefon());
            epost.setCellValue(excelRows.epost());
        }
    }

    private ExcelRows createExcelRow(RessursState ressurs, String orgEnhetId, String agressoId, String orgEnhetNavn, String tilgangsenhet, LocalDate gyldigFom, String epost) {
        return new ExcelRows(
            ressurs.navident(),
            ressurs.visningsnavn().etternavn(),
            ressurs.visningsnavn().fornavn(),
            ressurs.visningsnavn().mellomnavn().orElse(null),
            sektorMap.get(ressurs.sektor().name()),
            Objects.requireNonNullElse(orgEnhetId, "UKJENT"),
            Objects.requireNonNullElse(agressoId, "UKJENT"),
            Objects.requireNonNullElse(orgEnhetNavn, "UKJENT"),
            Objects.requireNonNullElse(tilgangsenhet, "UKJENT"),
            isNull(gyldigFom) ? "UKJENT" : gyldigFom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
            nonNull(ressurs.jobbtelefon()) ? ressurs.jobbtelefon().nummer() : "",
            requireNonNullElse(ressurs.primaryTelefonnummer(), ""),
            epost
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
        String tilgangsenhet,
        String fom,
        String jobbtelefon,
        String privatTelefon,
        String epost
    ) {}
}
