package no.nav.nom.apps.api.orgenhet.api.app;

import jakarta.servlet.http.HttpServletResponse;

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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KontaktpersonerExcelGenerator {
    private final static Logger log = LoggerFactory.getLogger(KontaktpersonerExcelGenerator.class);

    private final KontaktinfoClient kontaktinfoClient;

    public KontaktpersonerExcelGenerator(
        KontaktinfoClient kontaktinfoClient
    ) {
        this.kontaktinfoClient = kontaktinfoClient;
    }

    public void generate(HttpServletResponse response, OrgEnhet orgEnhet, List<Ressurs> sortedRessurser) {
        log.info("Getting content inf for ressurser: {}", sortedRessurser);
        List<RessursKontaktinfo> kontaktinfoForRessurser =  List.of();

        // try to get the contact info for the ressurser
        try {
            kontaktinfoForRessurser = kontaktinfoClient
                .getKontaktinfoForRessurser(sortedRessurser.stream().map(r -> r.state().navident()).toList(),
                    orgEnhet.id());
            log.info("Received kontaktinfo for this orgenhet: {}-{}", orgEnhet.id(), kontaktinfoForRessurser);
        } catch (Exception e) {
            if (e instanceof AccessDeniedException) throw e;
            else {
                log.error("Failed to get kontaktinfo for orgenhet {}", orgEnhet.id(), e);
                throw new RuntimeException(e.getMessage());
            }
        }

        // generate excel
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Kontaktpersoner - " + orgEnhet.navn());
            var rowcount = 0;

            // index -1 is for the header row
            addRow(sheet.createRow(rowcount++), null, null, -1);

            log.info("Generating excel for orgenhet {} with ressurser {}, and contantinfo {}", orgEnhet.id(), sortedRessurser, kontaktinfoForRessurser);
            for (RessursKontaktinfo info : kontaktinfoForRessurser) {
                Ressurs ressurs = getRessursForGittNavident(sortedRessurser, info.ressursNavident());

                // index determines the row number for ressurs.
                // If there are multiple contact info for a ressurs, we will have multiple rows for that ressurs.
                // However, medarbeider info will be displayed only once for that ressurs.
                int index = 0;
                if(info.kontaktinfo().isEmpty()){
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

    private Ressurs getRessursForGittNavident(List<Ressurs> ressurser, String navident) {
        return ressurser.stream().filter(r -> r.state().navident().equals(navident)).findFirst().orElse(null);
    }

    private Kontaktinfo getPrioritertKontaktinfo(List<Kontaktinfo> kontaktinfo) {
        return kontaktinfo.stream().min((k1, k2) -> k1.prioritet().compareTo(k2.prioritet()))
            .orElse(null);
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
}
