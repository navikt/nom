package no.nav.nom.apps.api.orgenhet.api.app;

import jakarta.servlet.http.HttpServletResponse;

import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.clients.kontaktinfo.Kontaktinfo;
import no.nav.nom.apps.api.clients.kontaktinfo.KontaktinfoClient;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KontaktpersonerExcelGenerator {

    private static final Map<String, String> sektorMap = Map.of(
        "NAV_STATLIG", "Statlig",
        "NAV_KOMMUNAL", "Kommunal",
        "EKSTERN", "Ekstern"
    );
    private final AuthResolver authResolver;
    private final KontaktinfoClient kontaktinfoClient;


    public KontaktpersonerExcelGenerator(
        AuthResolver authResolver,
        KontaktinfoClient kontaktinfoClient
    ) {
        this.authResolver = authResolver;
        this.kontaktinfoClient = kontaktinfoClient;
    }

    public void generate(HttpServletResponse response, OrgEnhet orgEnhet, List<Ressurs> ressurser) {
        var hasAdminRightsForEnhet = authResolver.kanOppretteRessurs(orgEnhet.id());

        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Kontaktpersoner - " + orgEnhet.navn());
            var rowcount = 0;

            addRow(sheet.createRow(rowcount++), null, null);

           if(!hasAdminRightsForEnhet) {
               throw new RuntimeException("User does not have access to view ressurser for this orgenhet");
           }

           /*
           var sortedRessurser = ressurser.stream()
               .sorted((r1, r2) -> r1.state().visningsnavn().etternavn().compareTo(r2.state().visningsnavn().etternavn()))
               .toList();
           */

           for (Ressurs ressurs : ressurser) {
               var kontaktinfo = kontaktinfoClient.getKontaktinfo(ressurs.state().navident());
               addRow(
                   sheet.createRow(rowcount++),
                   ressurs, kontaktinfo.isEmpty()? null : kontaktinfo.get(0)
               );
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
            throw new RuntimeException();
        }

    }
    private Kontaktinfo getPrioritertKontaktinfo(List<Kontaktinfo> kontaktinfo) {
        return kontaktinfo.stream().min((k1, k2) -> k1.prioritet().compareTo(k2.prioritet()))
            .orElse(null);
    }

    private static void addRow(Row row, Ressurs ressurs, Kontaktinfo kontaktinfo) {
        var medarbeiderNavident = row.createCell(0, CellType.STRING);
        var medarbeiderFornavn = row.createCell(1, CellType.STRING);
        var medarbeiderEtternavn = row.createCell(2, CellType.STRING);
        var kontaktpersonNavn = row.createCell(3, CellType.STRING);
        var kontaktpersonRelasjon = row.createCell(4, CellType.STRING);
        var kontaktpersonTelefonnr = row.createCell(5, CellType.STRING);
        var kontaktpersonProritet = row.createCell(6, CellType.STRING);

        if (ressurs == null) {
            medarbeiderNavident.setCellValue("Medarbeider-navident");
            medarbeiderFornavn.setCellValue("Medarbeider-fornavn");
            medarbeiderEtternavn.setCellValue("Medarbeider-etternavn");
            kontaktpersonNavn.setCellValue("Kontaktperson-navn");
            kontaktpersonRelasjon.setCellValue("Kontaktperson-relasjon");
            kontaktpersonTelefonnr.setCellValue("Kontaktperson-telefonnr");
            kontaktpersonProritet.setCellValue("Kontaktperson-prioritet");
        } else {
            medarbeiderNavident.setCellValue(ressurs.state().navident());
            medarbeiderFornavn.setCellValue(ressurs.state().visningsnavn().fornavn());
            medarbeiderEtternavn.setCellValue(ressurs.state().visningsnavn().etternavn());
            if (kontaktinfo != null) {
                kontaktpersonNavn.setCellValue(kontaktinfo.fornavn() + " " + kontaktinfo.etternavn());
                kontaktpersonRelasjon.setCellValue(kontaktinfo.relasjon());
                kontaktpersonTelefonnr.setCellValue(kontaktinfo.telefon());
                kontaktpersonProritet.setCellValue(kontaktinfo.prioritet());
            }
        }
    }
}
