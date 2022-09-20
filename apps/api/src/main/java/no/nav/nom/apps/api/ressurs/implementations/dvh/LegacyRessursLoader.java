package no.nav.nom.apps.api.ressurs.implementations.dvh;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.common.security.TokenRepository;
import no.nav.nom.apps.api.dvh.DvhRepository;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.StreamSupport;

@Slf4j
//@Component
@RequiredArgsConstructor
public class LegacyRessursLoader {

    private final String LEGACY_RESSURS_PATH = "/proxy/dvh/json/Hr/Nom/Nom_Ansatte";
    private final long incrementValue = 5000L;
    private long currentOffset = 0L;


    private final DvhRepository dvhRepository;
    private final AppProperties appProperties;
    private final TokenRepository tokenRepository;
    private final RestTemplate restTemplate;

    public void load(){
        List<LegacyRessurs> tempResult;

        do {
            try {
                log.info("Leser inn legacy ressurs fra sekvens {} til sekvens {}", currentOffset, currentOffset + incrementValue);
                tempResult = StreamSupport.stream(dvhExchange().get("Nom_AnsatteResponse").get("Nom_AnsatteResult").spliterator(), false)
                        .map(LegacyRessurs::new)
                        .toList();

                /*dvhRepository.getSluttdatoByPersonIdent().putAll(
                        tempResult.stream()
                                .filter(Predicate.not(LegacyRessurs::hasPermisjon))
                                .collect(Collectors.groupingBy(LegacyRessurs::getPersonIdent)).values().stream()
                                .map(l -> l.stream().max(Comparator.comparing(LegacyRessurs::getOffset)).get())
                                .filter(r -> r.getSluttdato() != null)
                                .collect(Collectors.toMap(LegacyRessurs::getPersonIdent, LegacyRessurs::getSluttdato))
                );*/

                currentOffset = tempResult.stream().map(LegacyRessurs::getOffset).max(Comparator.comparing(Long::valueOf)).orElse(currentOffset);
            }
            catch (Exception e){
                log.error("Lasting av legacyressurs feilet", e);
                return;
            }
        } while (tempResult.size() > 0);
    }


    private JsonNode dvhExchange() {
        val url = new StringBuilder(LEGACY_RESSURS_PATH)
                .append("?in_sekvensnummer_fra=")
                .append(currentOffset)
                .append("&in_sekvensnummer_til=")
                .append(currentOffset + incrementValue)
                .toString();

        val headers = new HttpHeaders();
        headers.setBearerAuth(tokenRepository.getSystemAadToken(appProperties.getProxy().getAadClientId()).serialize());

        return restTemplate.exchange(appProperties.getProxy().getBaseUrl() + url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class).getBody();
    }

    @Getter
    private class LegacyRessurs{

        private static final LocalDate D2099 = LocalDate.of(2099, 12, 31);


        private Long offset;
        private String personIdent;
        private String navIdent;
        private String agressoRessursType;
        private String remedyRessursType;
        private LocalDate remedySluttdato;
        private LocalDate agressoSluttdato;
        private Boolean remedyPermisjon;

        public LegacyRessurs(JsonNode jsonNode){
            offset = jsonNode.get("SEKVENSNR").asLong();
            personIdent = jsonNode.get("FNR").asText();
            navIdent = jsonNode.get("NAV_IDENT").asText();
            remedyPermisjon = jsonNode.hasNonNull("REMEDY_PERMISJON") && jsonNode.get("REMEDY_PERMISJON").asText().equals("JA");
            remedyRessursType = jsonNode.hasNonNull("ANSATT_TYPE") ? jsonNode.get("ANSATT_TYPE").asText() : null;
            remedySluttdato = jsonNode.hasNonNull("REMEDY_SLUTT_DATO") ? LocalDate.parse(jsonNode.get("REMEDY_SLUTT_DATO").asText()) : null;
            agressoRessursType = jsonNode.hasNonNull("ANSATT_RESSURS_KODE") ? jsonNode.get("ANSATT_RESSURS_KODE").asText() : null;
            agressoSluttdato = jsonNode.hasNonNull("AGRESSO_SLUTT_DATO") ? LocalDate.parse(jsonNode.get("AGRESSO_SLUTT_DATO").asText()) : null;
        }

        public enum RemedyRessursType {
            EKSTERN("Eksternt"),
            STATLIG("NAV Statlig"),
            KOMMUNAL("NAV Kommunalt"),
            ANNEN_STATLIG("Annen Statlig");

            public final String beskrivelse;

            RemedyRessursType(String beskrivelse) {
                this.beskrivelse = beskrivelse;
            }
        }

        public LocalDate getSluttdato(){
            if ("E".equals(agressoRessursType) ||
                    Arrays.asList(RemedyRessursType.EKSTERN.beskrivelse, RemedyRessursType.ANNEN_STATLIG.beskrivelse, RemedyRessursType.KOMMUNAL.beskrivelse).contains(remedyRessursType)) {
                return getDateOrNull(remedySluttdato);
            }

            return agressoSluttdato != null && !D2099.equals(agressoSluttdato)
                    ? agressoSluttdato : getDateOrNull(remedySluttdato);
        }

        public boolean hasPermisjon(){
            if(remedyPermisjon){
                if(!"E".equals(agressoRessursType)){
                    if (LegacyRessurs.RemedyRessursType.KOMMUNAL.beskrivelse.equals(remedyRessursType)) {
                        return true;
                    }
                    if(agressoSluttdato == null || LocalDate.of(2099, 12, 31).equals(agressoSluttdato)){
                        return true;
                    }
                }
            }
            return false;
        }

        private LocalDate getDateOrNull(final LocalDate dato) {
            return !D2099.equals(dato) ? dato : null;
        }
    }
}
