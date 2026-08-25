package no.nav.nom.libs.utils.common.excel;

import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFTable;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static java.util.Objects.nonNull;

@Slf4j
@Component
public class ExcelGenerator {

    public void generateWorkbook(HttpServletResponse response, String sheetName, List<String> headers, List<Map<String, Object>> listOfMappedObjects) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            var sheet = workbook.createSheet(sheetName);

            int colIdx = 0;
            int rowIdx = 0;
            Row headerRow = sheet.createRow(rowIdx++);
            for (String header : headers) {
                Cell headerCell = headerRow.createCell(colIdx++, CellType.STRING);
                headerCell.setCellValue(header);
            }

            for (Map<String, Object> mappedObject : listOfMappedObjects) {
                Row row = sheet.createRow(rowIdx++);
                colIdx = 0;
                for (String header : headers) {
                    Cell cell = row.createCell(colIdx++);
                    var value = mappedObject.get(header);
                    cell.setCellValue(nonNull(value) ? value.toString() : "");
                }
            }

            XSSFSheet xssfSheet = (XSSFSheet) sheet;
            int lastRow = xssfSheet.getLastRowNum();
            int lastColumn = xssfSheet.getRow(0).getLastCellNum() - 1;

            AreaReference areaRef = new AreaReference(
                new CellReference(0, 0),
                new CellReference(lastRow, lastColumn),
                xssfSheet.getWorkbook().getSpreadsheetVersion()
            );

            XSSFTable table = xssfSheet.createTable(areaRef);
            // Excel tillater ikke mellomrom i verken name eller displayName (OOXML-spec)
            String safeTableName = sheetName.replaceAll("[^a-zA-Z0-9_]", "_") + "Table";
            table.setName(safeTableName);
            table.setDisplayName(safeTableName);

            table.getCTTable().addNewTableStyleInfo().setName("TableStyleMedium4");
            table.setStyleName("TableStyleMedium4");
            table.getCTTable().setTotalsRowShown(false);
            table.getCTTable().getTableStyleInfo().setShowRowStripes(true);
            table.getCTTable().getTableStyleInfo().setShowColumnStripes(false);
            // autoFilter må ha ref-attributt, ellers reparerer Excel filen
            table.getCTTable().addNewAutoFilter().setRef(areaRef.formatAsString());

            for (int col = 0; col <= lastColumn; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(response.getOutputStream());
        } catch (IOException e) {
            throw new IOException("Failed to generate Excel file", e);
        } catch (Exception e) {
            log.error("Annen feil ved generering av Excel fil", e);
            throw e;
        }
    }
}

