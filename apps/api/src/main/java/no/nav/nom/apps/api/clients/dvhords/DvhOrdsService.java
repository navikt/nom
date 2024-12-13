package no.nav.nom.apps.api.clients.dvhords;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.orgenhet.implementations.dvh.v2.DvhOrgenhetV2;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.StreamSupport;

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
    public List<DvhOrgenhetV2> getOrgenheter() {
        return StreamSupport.stream(getOrgenheterJson().get("items").spliterator(), false)
            .map(DvhOrgenhetV2::fromOrds)
            .toList();
    }

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
    public List<DvhLeder> getLedere() {
        var jsonOrgenheter = getOrganiseringOgLedereJson();

        var ledere = StreamSupport.stream(jsonOrgenheter.get("items").spliterator(), false)
            .filter(this::determineEnhetActive)
            .filter(this::determineEnhetNotIgnored)
            .filter(this::validateLeder)
            .map(DvhLeder::fromOrds)
            .toList();

        log.info("Antall ledere hentet fra DVH: {}", ledere.size());
        return ledere;
    }

    public List<DvhOrganisering> getOrganiseringer() {
        var jsonOrgenheter = getOrganiseringOgLedereJson();

        var organiseringer = StreamSupport.stream(jsonOrgenheter.get("items").spliterator(), false)
            .filter(this::determineEnhetActive)
            .filter(this::determineEnhetNotIgnored)
            .filter(this::validateOrganisering)
            .map(DvhOrganisering::fromOrds)
            .toList();

        log.info("Antall organiseringer hentet fra DVH: {}", organiseringer.size());
        return organiseringer;
    }

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
    public List<DvhRessurs> getRessurser() {
        return StreamSupport.stream(getRessurserJson().get("items").spliterator(), false)
                .map(DvhRessurs::fromOrdsRessurs)
                .toList();
    }

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


    private boolean validateLeder(JsonNode e) {
        final var validate = e.hasNonNull("leder_navident") && e.hasNonNull("orgnode_kode") && e.hasNonNull("orgnode_niva");
        if (!validate) {
            log.info("Ugyldig leder fra dvh, LEDER_NAVIDENT: {}, ORGNODE_KODE: {}", getAsText(e, "leder_navident"), getAsText(e, "orgnode_kode"));
        }
        return validate;
    }

    private boolean validateOrganisering(JsonNode e) {
        final var validate = e.hasNonNull("orgnode_kode_over") && e.hasNonNull("orgnode_kode")
            && e.hasNonNull("orgnode_niva_over") && e.hasNonNull("orgnode_niva");
        if (!validate) {
            log.info("Ugyldig organisering fra dvh, ORGNODE_KODE_OVER: {}, ORGNODE_KODE: {}", getAsText(e, "orgnode_kode_over"), getAsText(e, "orgnode_kode"));
        }
        return validate;
    }

    private boolean determineEnhetActive(JsonNode e) {
        return e.get("orgnode_status").asText().equals("N");
    }

    private boolean determineEnhetNotIgnored(JsonNode jsonNode) {
        // 'deltar i rekruttering' og 'nav ventelønn' er kun for intern bruk i agresso.
        val orgnodeKode = jsonNode.get("orgnode_kode").asText();
        val deltarIRekruttering = orgnodeKode.matches("0+");
        val navVenteloenn = orgnodeKode.equals("9999920");
        return !(deltarIRekruttering || navVenteloenn);
    }

    private String getAsText(JsonNode e, String field) {
        JsonNode jsonNode = e.get(field);
        return jsonNode == null ? "<null>" : jsonNode.asText();
    }
}
