package no.nav.nom.apps.api.orgtilknytning.api.admin;

import jakarta.servlet.http.HttpServletResponse;

import no.nav.nom.apps.api.u4diff.U4Diff;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Component
public class U4DiffExcelGenerator {

    private final static Logger log = LoggerFactory.getLogger(U4DiffExcelGenerator.class);

    private static final Map<String, String> sektorMap = Map.of(
        "NAV_STATLIG", "Statlig",
        "NAV_KOMMUNAL", "Kommunal",
        "EKSTERN", "Ekstern"
    );

    public void generate(HttpServletResponse response, List<U4Diff> u4diff) {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("U4Diff");
            var rowcount = 0;

            addRow(sheet.createRow(rowcount++), null);

            for (var diff : u4diff) {
                addRow(sheet.createRow(rowcount++), diff);
            }

            var headerRow = sheet.getRow(0);
            var font = workbook.createFont();
            var style = workbook.createCellStyle();
            font.setBold(true);
            style.setFont(font);
            headerRow.cellIterator().forEachRemaining(cell -> cell.setCellStyle(style));
            sheet.createFreezePane(0, 1);

            workbook.write(response.getOutputStream());
        } catch (Exception e) {
            log.error("Kunne ikke generere excel", e);
            throw new RuntimeException("Kunne ikke generere excel", e);
        }

    }

    private static void addRow(Row row, U4Diff u4diff) {
        var id = row.createCell(0, CellType.NUMERIC);
        var navident = row.createCell(1, CellType.STRING);
        var nomDol = row.createCell(2, CellType.STRING);
        var nomGyldigFom = row.createCell(3, CellType.STRING);
        var nomGyldigTom = row.createCell(4, CellType.STRING);
        var nomOrgenhetId = row.createCell(5, CellType.STRING);
        var nomOrgenhetNavn = row.createCell(6, CellType.STRING);
        var nomOtSektor = row.createCell(7, CellType.STRING);
        var nomSektor = row.createCell(8, CellType.STRING);
        var type = row.createCell(9, CellType.STRING);
        var u4Dol = row.createCell(10, CellType.STRING);
        var u4GyldigFom = row.createCell(11, CellType.STRING);
        var u4GyldigTom = row.createCell(12, CellType.STRING);
        var u4OrgenhetId = row.createCell(13, CellType.STRING);
        var u4OrgenhetNavn = row.createCell(14, CellType.STRING);
        var u4Sektor = row.createCell(15, CellType.STRING);
        var u4StillingPeriode = row.createCell(16, CellType.STRING);
        var nomOrgtilknytning = row.createCell(17, CellType.STRING);

        if (u4diff == null) {
            id.setCellValue("ID");
            navident.setCellValue("Navident");
            nomDol.setCellValue("NOM-DOL");
            nomGyldigFom.setCellValue("NOM GyldigFom");
            nomGyldigTom.setCellValue("NOM GyldigTom");
            nomOrgenhetId.setCellValue("NOM OrgenhetId");
            nomOrgenhetNavn.setCellValue("NOM OrgenhetNavn");
            nomOtSektor.setCellValue("NOM OT Sektor");
            nomSektor.setCellValue("NOM Ressurs Sektor");
            type.setCellValue("Type");
            u4Dol.setCellValue("U4-DOL");
            u4GyldigFom.setCellValue("U4 GyldigFom");
            u4GyldigTom.setCellValue("U4 GyldigTom");
            u4OrgenhetId.setCellValue("U4 OrgenhetId");
            u4OrgenhetNavn.setCellValue("U4 OrgenhetNavn");
            u4Sektor.setCellValue("U4 Ressurs Sektor");
            u4StillingPeriode.setCellValue("U4 Stilling Periode");
            nomOrgtilknytning.setCellValue("NOM Orgtilknytning");
        } else {
            id.setCellValue(u4diff.getId());
            navident.setCellValue(u4diff.getNavident());
            nomDol.setCellValue(isNull(u4diff.getNomDol()) ? null : u4diff.getNomDol().toString());
            nomGyldigFom.setCellValue(isNull(u4diff.getNomGyldigFom()) ? null : u4diff.getNomGyldigFom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            nomGyldigTom.setCellValue(isNull(u4diff.getNomGyldigTom()) ? null : u4diff.getNomGyldigTom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            nomOrgenhetId.setCellValue(u4diff.getNomOrgenhetId());
            nomOrgenhetNavn.setCellValue(u4diff.getNomOrgenhetNavn());
            nomOtSektor.setCellValue(isNull(u4diff.getNomOtSektor()) ? null : sektorMap.get(u4diff.getNomOtSektor().name()));
            nomSektor.setCellValue(isNull(u4diff.getNomSektor()) ? null : sektorMap.get(u4diff.getNomSektor().name()));
            type.setCellValue(isNull(u4diff.getType()) ? null : u4diff.getType().name());
            u4Dol.setCellValue(isNull(u4diff.getU4Dol()) ? null : u4diff.getU4Dol().toString());
            u4GyldigFom.setCellValue(isNull(u4diff.getU4GyldigFom()) ? null : u4diff.getU4GyldigFom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            u4GyldigTom.setCellValue(isNull(u4diff.getU4GyldigTom()) ? null : u4diff.getU4GyldigTom().format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            u4OrgenhetId.setCellValue(u4diff.getU4OrgenhetId());
            u4OrgenhetNavn.setCellValue(u4diff.getU4OrgenhetNavn());
            u4Sektor.setCellValue(isNull(u4diff.getU4Sektor()) ? null : sektorMap.get(u4diff.getU4Sektor().name()));
            u4StillingPeriode.setCellValue(u4diff.getU4StillingPeriode());
            nomOrgtilknytning.setCellValue(u4diff.getNomOrgtilknytning());
        }
    }
}
