package no.nav.nom.apps.api.remedy;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RemedyEnhetCsvReaderTest {

    @Test
    public void should_parse_csv(){
        Resource dataFile = new ClassPathResource("remedy/data.csv");

        RemedyEnhetCsvReader reader = new RemedyEnhetCsvReader(dataFile);
        List<RemedyEnhet> list = reader.readRows();

        var testSample1 = list.stream().filter(s -> s.remedyEnhetId().equals("2027")).findFirst();
        assertTrue(testSample1.isPresent());
        assertEquals("ORGNIV3", testSample1.get().orgNivaa());
        assertEquals("202700", testSample1.get().agressoId());
        assertNull(testSample1.get().nomId());

        var testSample2 = list.stream().filter(s -> s.remedyEnhetId().equals("4415")).findFirst();
        assertTrue(testSample2.isPresent());
        assertEquals("ORGNIV3", testSample2.get().orgNivaa());
        assertEquals("443150", testSample2.get().agressoId());
        assertEquals("my788g", testSample2.get().nomId());
    }

}
