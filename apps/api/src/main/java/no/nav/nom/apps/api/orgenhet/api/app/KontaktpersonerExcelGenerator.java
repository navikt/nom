package no.nav.nom.apps.api.orgenhet.api.app;

import jakarta.servlet.http.HttpServletResponse;

import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.clients.kontaktinfo.Kontaktinfo;
import no.nav.nom.apps.api.clients.kontaktinfo.KontaktinfoClient;
import no.nav.nom.apps.api.clients.kontaktinfo.RessursKontaktinfo;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KontaktpersonerExcelGenerator {
    private final static Logger log = LoggerFactory.getLogger(KontaktpersonerExcelGenerator.class);

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

    public void generate(HttpServletResponse response, OrgEnhet orgEnhet, List<Ressurs> unsortedRessurser) {
        var hasAdminRightsForEnhet = authResolver.kanOppretteRessurs(orgEnhet.id());

        List<Ressurs> sortedRessurser = unsortedRessurser.stream().sorted((r1, r2) -> {
            var n1 = r1.state().visningsnavn();
            var n2 = r2.state().visningsnavn();
            return n1.fornavn().compareTo(n2.fornavn()) == 0 ? n1.etternavn().compareTo(n2.etternavn()) : n1.fornavn().compareTo(n2.fornavn());
        }).toList();

        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Kontaktpersoner - " + orgEnhet.navn());
            var rowcount = 0;

            addRow(sheet.createRow(rowcount++), null, null);

           if(!hasAdminRightsForEnhet) {
               var errorMessage = "User does not have access to view ressurser for this orgenhet";
               log.error(errorMessage);
               throw new RuntimeException(errorMessage);
           }

            log.info("Generating excel for orgenhet {} with {} ressurser", orgEnhet.id(), sortedRessurser.size());
            List<RessursKontaktinfo> kontaktinfoForRessurser = kontaktinfoClient.getKontaktinfoForRessurser(sortedRessurser.stream().map(r -> r.state().navident()).toList());
            for (RessursKontaktinfo info : kontaktinfoForRessurser) {
                Ressurs ressurs = getRessursForGittNavident(sortedRessurser, info.ressursNavident());
                addRow(
                        sheet.createRow(rowcount++),
                        ressurs, info.kontaktinfo().isEmpty()? null : info.kontaktinfo().getFirst()
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
            log.error("Failed to generate excel file", e);
            throw new RuntimeException(e.getMessage());
        }

    }

    private Ressurs getRessursForGittNavident(List<Ressurs> ressurser, String navident) {
        return ressurser.stream().filter(r -> r.state().navident().equals(navident)).findFirst().orElse(null);
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
