package no.nav.nom.apps.api.clients.dvhords;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DvhOrdsService {
    private final DvhOrdsClient dvhOrdsClient;
    private AppProperties.DvhOrds dvhOrdsProperties;


    public DvhOrdsService(DvhOrdsClient dvhOrdsClient, AppProperties appProperties) {
        dvhOrdsProperties = appProperties.clients().dvhOrds();
        this.dvhOrdsClient = dvhOrdsClient;
    }

    public JsonNode getEksterneRessurserJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.eksterneRessurserPath());
    }

    public JsonNode getEksterneRessurserJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getEksterneRessurserJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.eksterneRessurserPath(), numRowsToFetch);
    }

    public JsonNode getEksterneRessurserJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.eksterneRessurserPath(), ordsFilter);
    }
}
