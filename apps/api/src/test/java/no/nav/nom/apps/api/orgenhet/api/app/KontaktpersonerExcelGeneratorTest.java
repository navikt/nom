package no.nav.nom.apps.api.orgenhet.api.app;

import jakarta.servlet.http.HttpServletResponse;

import no.nav.nom.apps.api.clients.kontaktinfo.Kontaktinfo;
import no.nav.nom.apps.api.clients.kontaktinfo.KontaktinfoClient;
import no.nav.nom.apps.api.clients.kontaktinfo.RessursKontaktinfo;
import no.nav.nom.apps.api.orgenhet.api.app.dto.OrgEnhet;
import no.nav.nom.apps.api.ressurs.api.dto.Ressurs;
import no.nav.nom.apps.api.rest.RestTestBase;
import no.nav.nom.contract.nom.TelefonType;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.DelegatingServletOutputStream;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.TestData.ressursState;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class KontaktpersonerExcelGeneratorTest extends RestTestBase {

    @Autowired
    KontaktpersonerExcelGenerator kontaktpersonerExcelGenerator;

    @MockitoBean
    KontaktinfoClient kontaktinfoClient;

    @Test
    @DisplayName("Kontakt info excel report should be generated correctly")
    void testGenerateKontaktinfoExcel() throws Exception {
        // Mock HttpServletResponse
        HttpServletResponse response = mock(HttpServletResponse.class);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        when(response.getOutputStream()).thenReturn(new DelegatingServletOutputStream(outputStream));

        // prepare data
        String ressursNavident1 = "X123456";
        String ressursNavident2 = "X223456";
        String ressursNavident3 = "X323456";

        Ressurs ressurs1 = new Ressurs(ressursState().toBuilder()
            .navident(ressursNavident1)
            .visningsnavn(new Visningsnavn("R_Fornavn1", Optional.empty(), "Etternavn1"))
            .build(),
            List.of(new Ressurs.Telefon("1234", TelefonType.NAV_KONTOR_TELEFON)), "epost1@epost.com", true);
        Ressurs ressurs2 = new Ressurs(ressursState().toBuilder()
            .navident(ressursNavident2)
            .visningsnavn(new Visningsnavn("R_Fornavn3", Optional.empty(), "Etternavn2"))
            .build(),
            List.of(new Ressurs.Telefon("4567", TelefonType.NAV_KONTOR_TELEFON)), "epost2@epost.com", true);
        Ressurs ressurs3 = new Ressurs(ressursState().toBuilder()
            .navident(ressursNavident3)
            .visningsnavn(new Visningsnavn("R_Fornavn2", Optional.empty(), "Etternavn3"))
            .build(),
            List.of(new Ressurs.Telefon("7890", TelefonType.PRIVAT_TELEFON)), "epost3@epost.com", false);
        var ressurser = List.of(ressurs1, ressurs2, ressurs3);

        RessursKontaktinfo kontaktinfo1 = RessursKontaktinfo.builder()
            .ressursNavident(ressurs1.state().navident())
            .kontaktinfo(List.of(
                    Kontaktinfo.builder().kontaktId("id1_1").fornavn("K_Fornavn1_1").etternavn("Etternavn1_1").telefon("1234").prioritet(1).build(),
                    Kontaktinfo.builder().kontaktId("id1_2").fornavn("K_Fornavn1_2").etternavn("Etternavn1_2").telefon("123456").prioritet(2).build())
            ).build();

        RessursKontaktinfo kontaktinfo2 = RessursKontaktinfo.builder()
            .ressursNavident(ressurs2.state().navident())
            .kontaktinfo(List.of()).build();
        RessursKontaktinfo kontaktinfo3 = RessursKontaktinfo.builder()
            .ressursNavident(ressurs3.state().navident())
            .kontaktinfo(List.of(
                Kontaktinfo.builder().kontaktId("id3").fornavn("K_Fornavn3").etternavn("Etternavn3").telefon("8901").prioritet(1).build())
            ).build();


        // note: resources will be sorted based on the fornavn+etternavn, before retrieving the contact info
        List<RessursKontaktinfo> kontaktinfoForRessurser = List.of(kontaktinfo1, kontaktinfo3, kontaktinfo2);
        when(kontaktinfoClient.getKontaktinfoForRessurser(anyList(), any())).thenReturn(kontaktinfoForRessurser);

        OrgEnhet orgEnhet = OrgEnhet.builder().id("1").fid("F123").navn("Test Enhet").build();

        // act
        kontaktpersonerExcelGenerator.generate(response, orgEnhet, ressurser);

        // Verify the content of the generated Excel file
        try (Workbook workbook = new XSSFWorkbook(new ByteArrayInputStream(outputStream.toByteArray()))) {
            var sheet = workbook.getSheetAt(0);
            var headerRow = sheet.getRow(0);
            assertEquals("Medarbeider-navident", headerRow.getCell(0).getStringCellValue());
            assertEquals("Kontaktperson-navn", headerRow.getCell(3).getStringCellValue());

            var dataRow2 = sheet.getRow(1);
            assertEquals(ressursNavident1, dataRow2.getCell(0).getStringCellValue());
            assertEquals(kontaktinfo1.kontaktinfo().getFirst().fornavn() + " " + kontaktinfo1.kontaktinfo().getFirst().etternavn(),
                dataRow2.getCell(3).getStringCellValue());

            var dataRow3 = sheet.getRow(2);
            assertEquals("", dataRow3.getCell(0).getStringCellValue());

            // test total ressurs count and is sorted
            var dataRow4 = sheet.getRow(3);
            assertEquals(ressursNavident3, dataRow4.getCell(0).getStringCellValue());

            // make sure that resurs are added even if contact info is not available
            var dataRow5 = sheet.getRow(4);
            assertEquals(ressursNavident2, dataRow5.getCell(0).getStringCellValue());

            // assert that there total 5 rows in the excel
            assertNull(sheet.getRow(5));
        }
    }
}
