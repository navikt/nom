package no.nav.nom.apps.api.ressurs.api.app;

import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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

    public RessursExcelGenerator(OrgtilknytningStateRepository orgtilknytningStateRepository,
                                 OrgEnhetStateRepository orgEnhetStateRepository,
                                 RessursStateRepository ressursStateRepository
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    public void generate(HttpServletResponse response, List<Ressurs> ressurser, List<String> navidenter) {
        var dolMap =  orgtilknytningStateRepository.findByNavidentIn(navidenter).stream()
            .filter(OrgtilknytningState::erDagligOppfolging)
            .collect(Collectors.groupingBy(
                OrgtilknytningState::navident,
                Collectors.collectingAndThen(Collectors.toList(), dol -> dol.get(0))));
        var orgEnhetNavnMap = orgEnhetStateRepository.findByIdIn(
                dolMap.values().stream()
                    .map(OrgtilknytningState::orgEnhetId)
                    .collect(Collectors.toSet()))
            .stream().collect(
                Collectors.groupingBy(
                    OrgEnhetState::id,
                    Collectors.collectingAndThen(Collectors.toList(), orgEnehetState -> orgEnehetState.get(0).navn())
                )
            );

        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet("Ressurser");
            var rowcount = 0;

            addRow(sheet.createRow(rowcount++), null, null, null);

            for (Ressurs ressurs : ressurser) {
                var orgEnhetId = dolMap.get(ressurs.state().navident()).orgEnhetId();

                addRow(
                    sheet.createRow(rowcount++),
                    ressurs,
                    String.format("%s (%s)", orgEnhetNavnMap.get(orgEnhetId), orgEnhetId),
                    dolMap.get(ressurs.state().navident()).gyldigFom()
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

    private static void addRow(Row row, Ressurs ressurs, String orgEnhetNavn, LocalDate gyldigFom) {
        var navident = row.createCell(0, CellType.STRING);
        var navn = row.createCell(1, CellType.STRING);
        var sektor = row.createCell(2, CellType.STRING);
        var orgEnhet = row.createCell(3, CellType.STRING);
        var fom = row.createCell(4, CellType.STRING);
        var tlf = row.createCell(5, CellType.STRING);


        if (ressurs == null) {
            navident.setCellValue("Nav-ident");
            navn.setCellValue("Navn");
            sektor.setCellValue("Sektor");
            orgEnhet.setCellValue("Organisasjonsenhet");
            fom.setCellValue("Fra og med");
            tlf.setCellValue("Epost");
        } else {
            navident.setCellValue(ressurs.state().navident());
            navn.setCellValue(ressurs.state().visningsnavn().toString());
            sektor.setCellValue(sektorMap.get(ressurs.state().sektor().name()));
            orgEnhet.setCellValue(orgEnhetNavn);
            fom.setCellValue(gyldigFom.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")));
            tlf.setCellValue(ressurs.epost());
        }
    }
}
