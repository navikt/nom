package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.common.NomProxyRestClient;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhDuplikatAgressoIdAnalyse {
    private final NomProxyRestClient nomProxyRestClient;
    private final OrgenhetService orgenhetService;
    private HashSet<String> enhetIdForEnheterIOrganisering = new HashSet<>();

    public String analyserDvhOrgenheterOgStateEnheter() {
        enhetIdForEnheterIOrganisering.clear();
        enhetIdForEnheterIOrganisering.addAll(orgenhetService.getAllEnheterMedOrganisering().stream().map(IOrgenhet::getId).collect(Collectors.toSet()));

        log.info("Laster orgenhet v2 fra dvh");
        var response = nomProxyRestClient.getEnheterResponse();
        val parsedDvhEnheter = StreamSupport.stream(response.spliterator(), false)
                .map(DvhOrgenhetV2::new)
                // Validation: Orgenhetstatus should only be P and N
                .filter(e -> {
                    return e.getOrgenhetStatus().equals("P") || e.getOrgenhetStatus().equals("N");
                })
                // Validation: Enhet is invalid if its parked and never had stillinger.
                .filter(e -> {
                    if (e.getOrgNiv().equals("ORGENHET")) {
                        if (e.getOrgenhetStatus().equals("P")) {
                            return e.getStillingMaksDato() != null;
                        }
                    }
                    return true;
                }).filter(e -> {
                    if (e.getOrgNiv().equals("ORGENHET")) {
                        return !e.getOrgenhetStatus().equals("P") || !e.getGyldigFom().isAfter(e.getGyldigTom().orElse(LocalDate.MAX));
                    }
                    return true;
                }).toList();

        log.info("Orgenhet v2 last fra dvh. Parsed antall enheter:{}", parsedDvhEnheter.size());

        val dvhGroupedByAgressoId = parsedDvhEnheter.stream()
                // Grupperer enheter på kombinasjonen agressoId + navn + agresso orgnivå
                .collect(Collectors.groupingBy(DvhOrgenhetV2::getAgressoId));

        val dvhGroupedByAgressoIdAndNivaa = parsedDvhEnheter.stream()
            // Grupperer enheter på kombinasjonen agressoId + navn + agresso orgnivå
            .collect(Collectors.groupingBy(dvhOrgenhetV2 -> dvhOrgenhetV2.getAgressoId() + "-" + dvhOrgenhetV2.getOrgNiv()));

        String dvhDuplikaterPaaAgressoId = dvhDuplikaterPaaAgressoId(dvhGroupedByAgressoId, "AgressoId");
        String dvhDuplikaterPaaAgressoIdOgNivaa = dvhDuplikaterPaaAgressoId(dvhGroupedByAgressoIdAndNivaa, "AgressoId og Nivå");

        var stateOrgEnheterByAgressoId = orgenhetService.getAllEnheter().stream()
            .collect(Collectors.groupingBy(IOrgenhet::getAgressoId));

        var stateOrgEnheterByAgressoIdAndNivaa = orgenhetService.getAllEnheter().stream()
            .collect(Collectors.groupingBy( o -> o.getAgressoId() + " - " + o.getOrgNiv()));

        String stateDuplikaterPaaAgressoId =stateDuplikaterPaaAgressoId(stateOrgEnheterByAgressoId, "AgressoId");
        String stateDuplikaterPaaAgressoIdAndNivaa =stateDuplikaterPaaAgressoId(stateOrgEnheterByAgressoIdAndNivaa, "AgressoId og Nivå");

        return dvhDuplikaterPaaAgressoId + "\n"
            + dvhDuplikaterPaaAgressoIdOgNivaa + "\n"
            + stateDuplikaterPaaAgressoId + "\n"
            + stateDuplikaterPaaAgressoIdAndNivaa;
    }

    private String dvhDuplikaterPaaAgressoId(Map<String, List<DvhOrgenhetV2>> parsedEnheter, String grouping) {
        StringBuffer result = new StringBuffer();
        result.append("DVH Duplikater på ").append(grouping).append("\n");
        parsedEnheter.entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .forEach(e -> {
                result.append(grouping).append(": ").append(e.getKey()).append("\n");
                e.getValue().stream()
                    .sorted(Comparator.comparing(DvhOrgenhetV2::getGyldigFom).reversed())
                    .forEach(dvhOrgenhetV2 ->
                        result.append("   ")
                            .append(dvhOrgenhetV2.getAgressoId()).append(";")
                            .append(dvhOrgenhetV2.getNavn()).append(";")
                            .append(dvhOrgenhetV2.getOrgNiv()).append(";")
                            .append(dvhOrgenhetV2.getGyldigFom()).append(";")
                            .append(dvhOrgenhetV2.getGyldigTom().isPresent() ? dvhOrgenhetV2.getGyldigTom() : "").append(";")
                            .append(dvhOrgenhetV2.getRawData())
                            .append("\n"));
            });
        return result.toString();
    }

    private String stateDuplikaterPaaAgressoId(Map<String, List<IOrgenhet>> stateEnheter, String grouping) {
        StringBuffer result = new StringBuffer();
        result.append("STATE Duplikater på ").append(grouping).append("\n");
        stateEnheter.entrySet().stream()
            .filter(e -> e.getValue().size() > 1)
            .forEach(e -> {
                result.append(grouping).append(": ").append(e.getKey()).append("\n");
                e.getValue().stream()
                    .sorted(Comparator.comparing(IOrgenhet::getGyldigFom).reversed())
                    .forEach(oe ->
                        result
                            .append(oe.getAgressoId()).append(";")
                            .append(oe.getNavn()).append(";")
                            .append(oe.getOrgNiv()).append(";")
                            .append(oe.getId()).append(";")
                            .append(enhetIdForEnheterIOrganisering.contains(oe.getId()) ? "Y" : "N").append(";")
                            .append(oe.getGyldigFom()).append(";")
                            .append(oe.getGyldigTom().isPresent() ? oe.getGyldigTom() : "")
                            .append("\n"));
            });
        return result.toString();
    }
}