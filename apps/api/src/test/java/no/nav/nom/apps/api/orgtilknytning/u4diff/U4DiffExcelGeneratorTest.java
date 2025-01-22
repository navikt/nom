package no.nav.nom.apps.api.orgtilknytning.u4diff;

import no.nav.nom.apps.api.orgtilknytning.api.admin.U4DiffExcelGenerator;
import no.nav.nom.contract.nom.Sektor;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;

import jakarta.servlet.http.HttpServletResponse;

import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.DelegatingServletOutputStream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class U4DiffExcelGeneratorTest {

    @InjectMocks
    U4DiffExcelGenerator u4DiffExcelGenerator;

    @Test
    @DisplayName("Excel report should be generated correctly")
    void testGenerateExcel() throws Exception {
        var u4diff = List.of(generateU4Diff());

        // Mock HttpServletResponse
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(outputStream));

        // Generate Excel
        u4DiffExcelGenerator.generate(response, u4diff);

        // Verify the content of the generated Excel file
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            var headerRow = sheet.getRow(0);
            assertEquals("ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Navident", headerRow.getCell(1).getStringCellValue());

            var dataRow = sheet.getRow(1);
            assertEquals(10000, (int) dataRow.getCell(0).getNumericCellValue());
            assertEquals("X123456", dataRow.getCell(1).getStringCellValue());
            assertEquals("", dataRow.getCell(13).getStringCellValue());
            assertEquals("", dataRow.getCell(15).getStringCellValue());
        }
    }

    @Test
    @DisplayName("Excel report should be generated correctly even with all null values")
    void testGenerateExcelWithNullValues() throws Exception {
        var u4diff = List.of(generateU4DiffAllNull());

        // Mock HttpServletResponse
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(outputStream));

        // Generate Excel
        u4DiffExcelGenerator.generate(response, u4diff);

        // Verify the content of the generated Excel file
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            var headerRow = sheet.getRow(0);
            assertEquals("ID", headerRow.getCell(0).getStringCellValue());
            assertEquals("Navident", headerRow.getCell(1).getStringCellValue());

            var dataRow = sheet.getRow(1);
            assertEquals(10000, (int) dataRow.getCell(0).getNumericCellValue());
            assertEquals("", dataRow.getCell(1).getStringCellValue());
            assertEquals("", dataRow.getCell(2).getStringCellValue());
            assertEquals("", dataRow.getCell(7).getStringCellValue());
            assertEquals("", dataRow.getCell(13).getStringCellValue());
            assertEquals("", dataRow.getCell(15).getStringCellValue());
        }
    }

    private U4Diff generateU4Diff() {
        return U4Diff.builder()
            .id(10000)
            .navident("X123456")
            .nomDol(true)
            .nomGyldigFom(LocalDate.now().minusMonths(1))
            .nomGyldigTom(LocalDate.now().plusMonths(1))
            .nomOrgenhetId("xy123z")
            .nomOrgenhetNavn("NAV")
            .nomOtSektor(Sektor.NAV_STATLIG)
            .nomSektor(Sektor.NAV_STATLIG)
            .type(U4DiffType.MANGLENDE_STATLIG_I_U4)
            .u4Dol(null)
            .u4GyldigFom(null)
            .u4GyldigTom(null)
            .u4OrgenhetId(null)
            .u4OrgenhetNavn(null)
            .u4Sektor(null)
            .u4StillingPeriode(null)
            .nomOrgtilknytning("{ \"navident\": \"X123456\", \"navn\": \"NAV\", \"sektor\": \"NAV_STATLIG\" }")
            .build();
    }

    private U4Diff generateU4DiffAllNull() {
        return U4Diff.builder()
            .id(10000)
            .navident(null)
            .nomDol(null)
            .nomGyldigFom(null)
            .nomGyldigTom(null)
            .nomOrgenhetId(null)
            .nomOrgenhetNavn(null)
            .nomOtSektor(null)
            .nomSektor(null)
            .type(null)
            .u4Dol(null)
            .u4GyldigFom(null)
            .u4GyldigTom(null)
            .u4OrgenhetId(null)
            .u4OrgenhetNavn(null)
            .u4Sektor(null)
            .u4StillingPeriode(null)
            .nomOrgtilknytning(null)
            .build();
    }
}