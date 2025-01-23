package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.authorize.AuthResolver;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class RessursExcelGenerator {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    private static final Map<String, String> sektorMap = Map.of(
        "NAV_STATLIG", "Statlig",
        "NAV_KOMMUNAL", "Kommunal",
        "EKSTERN", "Ekstern"
    );
    private final AuthResolver authResolver;

    public RessursExcelGenerator(OrgtilknytningStateRepository orgtilknytningStateRepository,
                                 OrgEnhetStateRepository orgEnhetStateRepository,
                                 AuthResolver authResolver
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.authResolver = authResolver;
    }

    public void generate(HttpServletResponse response, List<Ressurs> ressurser, List<String> navidenter, String topOrgEnhetId) {
        var kanSeSluttdato = authResolver.kanOppretteRessurs(topOrgEnhetId);
        var otNavn = orgEnhetStateRepository.findById(topOrgEnhetId).get().navn();
        var otMap = orgtilknytningStateRepository.findByNavidentIn(navidenter).stream()
            .filter(ot -> ot.orgEnhetId().equals(topOrgEnhetId))
            .collect(Collectors.groupingBy(
                OrgtilknytningState::navident,
                Collectors.collectingAndThen(
                    Collectors.toList(),
                    dol -> Collections.max(dol, Comparator.comparing(OrgtilknytningState::gyldigFom))
                )
            ));

        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Ressurser");
            var rowcount = 0;

            addRow(sheet.createRow(rowcount++), null, null, null, kanSeSluttdato);

            for (Ressurs ressurs : ressurser) {
                addRow(
                    sheet.createRow(rowcount++),
                    ressurs,
                    String.format("%s (%s)", otNavn, topOrgEnhetId),
                    otMap.get(ressurs.state().navident()).gyldigFom(),
                    kanSeSluttdato
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

    private static void addRow(Row row, Ressurs ressurs, String orgEnhetNavn, LocalDate gyldigFom, boolean kanSeSluttdato) {
        var navident = row.createCell(0, CellType.STRING);
        var navn = row.createCell(1, CellType.STRING);
        var sektor = row.createCell(2, CellType.STRING);
        var orgEnhet = row.createCell(3, CellType.STRING);
        var epost = row.createCell(4, CellType.STRING);
        var fom = row.createCell(5, CellType.STRING);
        var sluttdato = row.createCell(6, CellType.STRING);


        if (ressurs == null) {
            navident.setCellValue("Navident");
            navn.setCellValue("Navn");
            sektor.setCellValue("Sektor");
            orgEnhet.setCellValue("Organisasjonsenhet");
            epost.setCellValue("Epost");
            fom.setCellValue("Fra og med");
            if (kanSeSluttdato) sluttdato.setCellValue("Sluttdato");
        } else {
            navident.setCellValue(ressurs.state().navident());
            navn.setCellValue(ressurs.state().visningsnavn().toString());
            sektor.setCellValue(sektorMap.get(ressurs.state().sektor().name()));
            orgEnhet.setCellValue(orgEnhetNavn);
            epost.setCellValue(ressurs.epost());
            fom.setCellValue(gyldigFom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            if (kanSeSluttdato) {
                ressurs.state().sluttdato().ifPresentOrElse(
                    gyldigTom -> sluttdato.setCellValue(gyldigTom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))),
                    () -> sluttdato.setCellValue("")
                );
            }
        }
    }
}
