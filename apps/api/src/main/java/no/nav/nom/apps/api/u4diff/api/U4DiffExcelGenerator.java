package no.nav.nom.apps.api.u4diff.api;

import jakarta.servlet.http.HttpServletResponse;

import no.nav.nom.apps.api.u4diff.U4Diff;
import no.nav.nom.contract.nom.Sektor;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
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

    public enum ExportType {
        SEKTOR_DIFF_PAA_RESSURS,
        IDENTER_IKKE_I_U4,
        ULIK_ENHET,
        ULIK_START_DATO,
        ULIK_SLUTT_DATO,
        ALLE_DIFFERANSER,
        IKKE_KLASSIFISERTE_DIFFERANSER
    }

    public void generate(HttpServletResponse response, List<U4Diff> u4diff, ExportType type) {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("U4Diff");
            var rowcount = 0;

            // HEADER
            switch (type) {
                case IDENTER_IKKE_I_U4, IKKE_KLASSIFISERTE_DIFFERANSER, ALLE_DIFFERANSER -> addRowGeneric(sheet.createRow(rowcount++), null);
                case SEKTOR_DIFF_PAA_RESSURS -> addRowSektorDiff(sheet.createRow(rowcount++), null);
                case ULIK_ENHET -> addRowUlikEnhet(sheet.createRow(rowcount++), null);
                case ULIK_START_DATO, ULIK_SLUTT_DATO -> addRowUlikDato(sheet.createRow(rowcount++), null);
            }

            // RADER
            for (var diff : u4diff) {
                switch (type) {
                    case IDENTER_IKKE_I_U4, IKKE_KLASSIFISERTE_DIFFERANSER, ALLE_DIFFERANSER -> addRowGeneric(sheet.createRow(rowcount++), diff);
                    case SEKTOR_DIFF_PAA_RESSURS -> addRowSektorDiff(sheet.createRow(rowcount++), diff);
                    case ULIK_ENHET -> addRowUlikEnhet(sheet.createRow(rowcount++), diff);
                    case ULIK_START_DATO, ULIK_SLUTT_DATO -> addRowUlikDato(sheet.createRow(rowcount++), diff);
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
        } catch (Exception e) {
            log.error("Kunne ikke generere excel", e);
            throw new RuntimeException("Kunne ikke generere excel", e);
        }
    }

    private static void addRowGeneric(Row row, U4Diff u4diff) {
        int col = 0;
        id(col++, row, u4diff);
        type(col++, row, u4diff);
        kilde(col++, row, u4diff);
        navident(col++, row, u4diff);
        nomDol(col++, row, u4diff);
        nomGyldigFom(col++, row, u4diff);
        nomGyldigTom(col++, row, u4diff);
        nomOrgenhetId(col++, row, u4diff);
        nomOrgenhetNavn(col++, row, u4diff);
        nomOtSektor(col++, row, u4diff);
        nomSektor(col++, row, u4diff);
        u4Dol(col++, row, u4diff);
        u4GyldigFom(col++, row, u4diff);
        u4GyldigTom(col++, row, u4diff);
        u4OrgenhetId(col++, row, u4diff);
        u4OrgenhetNavn(col++, row, u4diff);
        u4SektorPaaStilling(col++, row, u4diff);
        u4SektorPaaRessurs(col++, row, u4diff);
        u4RessursAnsesttelsesTypeBesk(col++, row, u4diff);
        u4RessursType(col++, row, u4diff);
        u4StillingPeriode(col++, row, u4diff);
        nomOrgtilknytning(col++, row, u4diff);
    }

    private static void addRowSektorDiff(Row row, U4Diff u4diff) {
        int col = 0;
        navident(col++, row, u4diff);
        nomSektor(col++, row, u4diff);
        u4SektorPaaRessurs(col++, row, u4diff);
        u4RessursAnsesttelsesTypeBesk(col++, row, u4diff);
        u4RessursType(col++, row, u4diff);
    }

    private static void addRowUlikEnhet(Row row, U4Diff u4diff) {
        int col = 0;
        navident(col++, row, u4diff);
        nomDol(col++, row, u4diff);
        nomGyldigFom(col++, row, u4diff);
        nomGyldigTom(col++, row, u4diff);
        nomOrgenhetId(col++, row, u4diff);
        nomOrgenhetNavn(col++, row, u4diff);
        u4Dol(col++, row, u4diff);
        u4GyldigFom(col++, row, u4diff);
        u4GyldigTom(col++, row, u4diff);
        u4OrgenhetId(col++, row, u4diff);
        u4OrgenhetNavn(col++, row, u4diff);
        u4StillingPeriode(col++, row, u4diff);
        nomOrgtilknytning(col++, row, u4diff);
    }

    private void addRowUlikDato(Row row, U4Diff u4diff) {
        int col = 0;
        navident(col++, row, u4diff);
        nomOrgenhetId(col++, row, u4diff);
        nomOrgenhetNavn(col++, row, u4diff);
        nomGyldigFom(col++, row, u4diff);
        nomGyldigTom(col++, row, u4diff);
        u4GyldigFom(col++, row, u4diff);
        u4GyldigTom(col++, row, u4diff);
        u4StillingPeriode(col++, row, u4diff);
        nomOrgtilknytning(col++, row, u4diff);
    }

    private static void id(int col, Row row, U4Diff u4diff) {
        var id = row.createCell(col, CellType.NUMERIC);
        if (u4diff == null) { id.setCellValue("ID"); } else { id.setCellValue(u4diff.getId()); }
    }

    private static void type(int col, Row row, U4Diff u4diff) {
        var type = row.createCell(col, CellType.STRING);
        if (u4diff == null) { type.setCellValue("Diff Type"); } else { type.setCellValue(enumToText(u4diff.getType())); }
    }

    private static void kilde(int col, Row row, U4Diff u4diff) {
        var type = row.createCell(col, CellType.STRING);
        if (u4diff == null) { type.setCellValue("Diff kilde"); } else { type.setCellValue(enumToText(u4diff.getKilde())); }
    }

    private static void navident(int col, Row row, U4Diff u4diff) {
        var navident = row.createCell(col, CellType.STRING);
        if (u4diff == null) { navident.setCellValue("Navident"); } else { navident.setCellValue(u4diff.getNavident()); }
    }

    private static void nomDol(int col, Row row, U4Diff u4diff) {
        var nomDol = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomDol.setCellValue("NOM-DOL"); } else { nomDol.setCellValue(isNull(u4diff.getNomDol()) ? null : u4diff.getNomDol().toString()); }
    }

    private static void nomGyldigFom(int col, Row row, U4Diff u4diff) {
        var nomGyldigFom = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomGyldigFom.setCellValue("NOM GyldigFom"); } else { nomGyldigFom.setCellValue(getDateAsString(u4diff.getNomGyldigFom())); }
    }

    private static void nomGyldigTom(int col, Row row, U4Diff u4diff) {
        var nomGyldigTom = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomGyldigTom.setCellValue("NOM GyldigTom"); } else { nomGyldigTom.setCellValue(getDateAsString(u4diff.getNomGyldigTom())); }
    }

    private static void nomOrgenhetId(int col, Row row, U4Diff u4diff) {
        var nomOrgenhetId = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomOrgenhetId.setCellValue("NOM OrgenhetId"); } else { nomOrgenhetId.setCellValue(u4diff.getNomOrgenhetId()); }
    }

    private static void nomOrgenhetNavn(int col, Row row, U4Diff u4diff) {
        var nomOrgenhetNavn = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomOrgenhetNavn.setCellValue("NOM OrgenhetNavn"); } else { nomOrgenhetNavn.setCellValue(u4diff.getNomOrgenhetNavn()); }
    }

    private static void nomOtSektor(int col, Row row, U4Diff u4diff) {
        var nomOtSektor = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomOtSektor.setCellValue("NOM OT Sektor"); } else { nomOtSektor.setCellValue(enumToText(u4diff.getNomOtSektor())); }
    }

    private static void nomSektor(int col, Row row, U4Diff u4diff) {
        var nomSektor = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomSektor.setCellValue("NOM Ressurs Sektor"); } else { nomSektor.setCellValue(enumToText(u4diff.getNomSektor())); }
    }

    private static void u4Dol(int col, Row row, U4Diff u4diff) {
        var u4Dol = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4Dol.setCellValue("U4-DOL"); } else { u4Dol.setCellValue(isNull(u4diff.getU4Dol()) ? null : u4diff.getU4Dol().toString()); }
    }

    private static void u4GyldigFom(int col, Row row, U4Diff u4diff) {
        var u4GyldigFom = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4GyldigFom.setCellValue("U4 GyldigFom"); } else { u4GyldigFom.setCellValue(getDateAsString(u4diff.getU4GyldigFom())); }
    }

    private static void u4GyldigTom(int col, Row row, U4Diff u4diff) {
        var u4GyldigTom = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4GyldigTom.setCellValue("U4 GyldigTom"); } else { u4GyldigTom.setCellValue(getDateAsString(u4diff.getU4GyldigTom())); }
    }

    private static void u4OrgenhetId(int col, Row row, U4Diff u4diff) {
        var u4OrgenhetId = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4OrgenhetId.setCellValue("U4 OrgenhetId"); } else { u4OrgenhetId.setCellValue(u4diff.getU4OrgenhetId()); }
    }

    private static void u4OrgenhetNavn(int col, Row row, U4Diff u4diff) {
        var u4OrgenhetNavn = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4OrgenhetNavn.setCellValue("U4 OrgenhetNavn"); } else { u4OrgenhetNavn.setCellValue(u4diff.getU4OrgenhetNavn()); }
    }

    private static void u4SektorPaaRessurs(int col, Row row, U4Diff u4diff) {
        var u4SektorPaaRessurs = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4SektorPaaRessurs.setCellValue("U4 Ressurs Sektor"); } else { u4SektorPaaRessurs.setCellValue(enumToText(u4diff.getU4SektorPaaRessurs())); }
    }

    private static void u4SektorPaaStilling(int col, Row row, U4Diff u4diff) {
        var u4SektorPaaStilling = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4SektorPaaStilling.setCellValue("U4 Sektor paa stilling"); } else { u4SektorPaaStilling.setCellValue(enumToText(u4diff.getU4SektorPaaStilling())); }
    }

    private static void u4RessursAnsesttelsesTypeBesk(int col, Row row, U4Diff u4diff) {
        var u4RessursAnsesttelsesTypeBesk = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4RessursAnsesttelsesTypeBesk.setCellValue("U4 Ressurs ansettelsestype besk."); } else { u4RessursAnsesttelsesTypeBesk.setCellValue(u4diff.getU4RessursAnsettelsestypeBesk()); }
    }

    private static void u4RessursType(int col, Row row, U4Diff u4diff) {
        var u4RessursType = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4RessursType.setCellValue("U4 Ressurs type"); } else { u4RessursType.setCellValue(u4diff.getU4RessursType()); }
    }

    private static void u4StillingPeriode(int col, Row row, U4Diff u4diff) {
        var u4StillingPeriode = row.createCell(col, CellType.STRING);
        if (u4diff == null) { u4StillingPeriode.setCellValue("U4 Stilling Periode"); } else { u4StillingPeriode.setCellValue(u4diff.getU4StillingPeriode()); }
    }

    private static void nomOrgtilknytning(int col, Row row, U4Diff u4diff) {
        var nomOrgtilknytning = row.createCell(col, CellType.STRING);
        if (u4diff == null) { nomOrgtilknytning.setCellValue("NOM Orgtilknytning"); } else { nomOrgtilknytning.setCellValue(u4diff.getNomOrgtilknytning()); }
    }

    private static String getDateAsString(LocalDate date) {
        return isNull(date) ? null : date.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));
    }

    private static <E extends Enum<E>> String enumToText(E enumValue) {
        if (isNull(enumValue)) return null;
        if (enumValue instanceof Sektor) {
            return sektorMap.get(enumValue.name());
        } else {
            return enumValue.name();
        }
    }
}
