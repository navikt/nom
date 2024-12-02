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

    // Enheter
    public JsonNode getOrgenheterJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.enhetPath());
    }

    public JsonNode getOrgenheterJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getOrgenheterJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.enhetPath(), numRowsToFetch);
    }

    public JsonNode getOrgenheterJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.enhetPath(), ordsFilter);
    }

    // Hierarki/Organisering og ledere
    public JsonNode getOrganiseringOgLedereJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.orgHierarkiPath());
    }

    public JsonNode getOrganiseringOgLedereJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getOrganiseringOgLedereJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.orgHierarkiPath(), numRowsToFetch);
    }

    public JsonNode getOrganiseringOgLedereJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.orgHierarkiPath(), ordsFilter);
    }

    // Ansatte buckets/Ressurser
    public JsonNode getRessurserJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.ansatteBucketsPath());
    }

    public JsonNode getRessurserJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getRessurserJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.ansatteBucketsPath(), numRowsToFetch);
    }

    public JsonNode getRessurserJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.ansatteBucketsPath(), ordsFilter);
    }

    // RessursStillinger/Statlige ressurser tilknytninger
    public JsonNode getStatligeTilknytningerJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.ressursStillingerPath());
    }

    public JsonNode getStatligeTilknytningerJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getStatligeTilknytningerJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.ressursStillingerPath(), numRowsToFetch);
    }

    public JsonNode getStatligeTilknytningerJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.ressursStillingerPath(), ordsFilter);
    }

    // Kommunale ressurser tilknytninger
    public JsonNode getKommunaleTilknytningerJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.kommunaleRessurserPath());
    }

    public JsonNode getKommunaleTilknytningerJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getKommunaleTilknytningerJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.kommunaleRessurserPath(), numRowsToFetch);
    }

    public JsonNode getKommunaleTilknytningerJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.kommunaleRessurserPath(), ordsFilter);
    }

    // Eksterne ressurser tilknytninger
    public JsonNode getEksterneTilknytningerJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.eksterneRessurserPath());
    }

    public JsonNode getEksterneTilknytningerJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getEksterneTilknytningerJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.eksterneRessurserPath(), numRowsToFetch);
    }

    public JsonNode getEksterneTilknytningerJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.eksterneRessurserPath(), ordsFilter);
    }

    // Telefoner
    public JsonNode getTlfJson() {
        return dvhOrdsClient.dvhGetJsonData(dvhOrdsProperties.tlfPath());
    }

    public JsonNode getTlfJson(int numRowsToFetch) {
        if (numRowsToFetch < 1) {
            return getTlfJson();
        }
        return dvhOrdsClient.dvhGetJsonDataLimitRows(dvhOrdsProperties.tlfPath(), numRowsToFetch);
    }

    public JsonNode getTlfJson(String ordsFilter) {
        return dvhOrdsClient.dvhGetJsonDataOrdsFilter(dvhOrdsProperties.tlfPath(), ordsFilter);
    }
}
