package no.nav.nom.apps.api.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.nomproxy.NomProxyClient;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.dvh.dto.DvhLeder;
import no.nav.nom.apps.api.dvh.dto.DvhOrganisering;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Component
public class DvhOldTdvService {
    private final NomProxyClient nomProxyClient;
    private final AppProperties appProperties;

    public DvhOldTdvService(NomProxyClient nomProxyClient, AppProperties appProperties) {
        this.nomProxyClient = nomProxyClient;
        this.appProperties = appProperties;
    }

    public List<DvhRessurs> getTdvRessurser() {
        var jsonRessurser = dvhExchange(appProperties.clients().nomProxy().ansatteBucketsPath());

        var dvhRessurs = StreamSupport.stream(jsonRessurser.get("NOM_ANSATTE_BUCKETS_v2Response").spliterator(), false)
            .map(DvhRessurs::new)
            .toList();

        log.info("Antall ressurser hentet fra DVH: {}", dvhRessurs.size());
        return dvhRessurs;
    }

    public List<DvhLeder> getTdvLedere() {
        var jsonOrgenheter = dvhExchange(appProperties.clients().nomProxy().orgHierarkiPath());

        var ledere = StreamSupport.stream(jsonOrgenheter.get("NOM_HRORG_HIERARKI2_v2Response").spliterator(), false)
            .filter(this::determineEnhetActive)
            .filter(this::determineEnhetNotIgnored)
            .filter(this::validateLeder)
            .map(DvhLeder::new)
            .toList();

        log.info("Antall ledere hentet fra DVH: {}", ledere.size());
        return ledere;
    }

    public List<DvhOrganisering> getTdvOrganisering() {
        var jsonOrgenheter = dvhExchange(appProperties.clients().nomProxy().orgHierarkiPath());

        var organisering = StreamSupport.stream(jsonOrgenheter.get("NOM_HRORG_HIERARKI2_v2Response").spliterator(), false)
            .filter(this::determineEnhetActive)
            .filter(this::determineEnhetNotIgnored)
            .filter(this::validateOrganisering)
            .map(DvhOrganisering::new)
            .toList();

        log.info("Antall organiseringer hentet fra DVH: {}", organisering.size());
        return organisering;
    }

    private boolean validateLeder(JsonNode e) {
        final var validate = e.hasNonNull("LEDER_NAVIDENT") && e.hasNonNull("ORGNODE_KODE") && e.hasNonNull("ORGNODE_NIVA");
        if (!validate) {
            log.info("Ugyldig leder fra dvh, LEDER_NAVIDENT: {}, ORGNODE_KODE: {}", getAsText(e, "LEDER_NAVIDENT"), getAsText(e, "ORGNODE_KODE"));
        }
        return validate;
    }

    private boolean validateOrganisering(JsonNode e) {
        final var validate = e.hasNonNull("ORGNODE_KODE_OVER") && e.hasNonNull("ORGNODE_KODE")
            && e.hasNonNull("ORGNODE_NIVA_OVER") && e.hasNonNull("ORGNODE_NIVA");
        if (!validate) {
            log.info("Ugyldig organisering fra dvh, ORGNODE_KODE_OVER: {}, ORGNODE_KODE: {}", getAsText(e, "ORGNODE_KODE_OVER"), getAsText(e, "ORGNODE_KODE"));
        }
        return validate;
    }

    private boolean determineEnhetActive(JsonNode e) {
        return e.get("ORGNODE_STATUS").asText().equals("N");
    }

    private boolean determineEnhetNotIgnored(JsonNode jsonNode) {
        // 'deltar i rekruttering' og 'nav ventelønn' er kun for intern bruk i agresso.
        val orgnodeKode = jsonNode.get("ORGNODE_KODE").asText();
        val deltarIRekruttering = orgnodeKode.matches("0+");
        val navVenteloenn = orgnodeKode.equals("9999920");
        return !(deltarIRekruttering || navVenteloenn);
    }

    public List<DvhTelefon> getTdvTelefoner() {
        var jsonTlfNr = dvhExchange(appProperties.clients().nomProxy().tlfPath());

        var tlfRader = StreamSupport.stream(jsonTlfNr.get("NOM_TLF_v1Result").spliterator(), false)
            .filter(this::validateTlf)
            .map(DvhTelefon::new)
            .collect(Collectors.toList());

        log.info("Antall telefoner hentet fra DVH: {}", tlfRader.size());
        return tlfRader;
    }

    private boolean validateTlf(JsonNode jsonNode) {
        var isSecretNumber = !jsonNode.hasNonNull("issecretnumber") || (jsonNode.hasNonNull("issecretnumber") && !jsonNode.get("issecretnumber").asBoolean());
        var userRef1 = jsonNode.get("userreference1");

        return isSecretNumber && (userRef1.asText().startsWith("PT-") || userRef1.asText().startsWith("PK-"));
    }

    private JsonNode dvhExchange(String urlPath) {
        return nomProxyClient.getWebClient().get()
            .uri(urlPath)
            .retrieve()
            .bodyToMono(JsonNode.class)
            .block();
    }

    private String getAsText(JsonNode e, String field) {
        JsonNode jsonNode = e.get(field);
        return jsonNode == null ? "<null>" : jsonNode.asText();
    }
}
