package no.nav.nom.apps.api.remedy;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.stream.Collectors;

public class RemedyEnhetCsvReader {

    private final CSVReader csvReader;

    public RemedyEnhetCsvReader(Resource resource) {

        try {
            Reader reader = new InputStreamReader(resource.getInputStream());

            CSVParser parser = new CSVParserBuilder()
                .withSeparator(';')
                .withIgnoreQuotations(true)
                .build();

            csvReader = new CSVReaderBuilder(reader)
                .withSkipLines(1)
                .withCSVParser(parser)
                .build();
        } catch (IOException e) {
            throw new RemedyCsvException(e);
        }
    }

    public List<RemedyEnhet> readRows() {
        try {
            List<String[]> lines = csvReader.readAll();

            return lines.stream()
                .map(row -> mapRow(row))
                .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RemedyCsvException(e);
        } catch (CsvException e) {
            throw new RemedyCsvException(e);
        }
    }

    private RemedyEnhet mapRow(String[] values) {

        String nomId = (StringUtils.isBlank(values[10]) || "leggInn".equals(values[10])) ? null : values[10];

        return new RemedyEnhet(
            values[1],
            values[8],
            values[9],
            (nomId)
        );
    }
}
