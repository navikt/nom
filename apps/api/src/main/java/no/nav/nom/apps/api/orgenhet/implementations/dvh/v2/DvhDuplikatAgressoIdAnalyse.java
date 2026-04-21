package no.nav.nom.apps.api.orgenhet.implementations.dvh.v2;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import no.nav.nom.apps.api.clients.dvhords.DvhOrdsService;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Objects.nonNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class DvhDuplikatAgressoIdAnalyse {
    private final OrgenhetService orgenhetService;
    private final HashSet<String> enhetIdForEnheterIOrganisering = new HashSet<>();
    private final DvhOrdsService dvhOrdsService;

    public String analyserDvhOrgenheterOgStateEnheter(String year) {
        enhetIdForEnheterIOrganisering.clear();
        enhetIdForEnheterIOrganisering.addAll(orgenhetService.getAktiveEnheterIOrganisasjonen().stream().map(IOrgenhet::getId).collect(Collectors.toSet()));

        log.info("Laster orgenhet v2 fra dvh");
        var dvhOrgenheter = dvhOrdsService.getOrgenheter();

        val parsedDvhEnheter = dvhOrgenheter.stream()
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
                        return !e.getOrgenhetStatus().equals("P") || !e.getGyldigFom().isAfter(Objects.requireNonNullElse(e.getGyldigTom(), LocalDate.MAX));
                    }
                    return true;
                }).toList();

        log.info("Orgenhet v2 last fra dvh. Parsed antall enheter:{}", parsedDvhEnheter.size());

        var filtrerteDvhEnheter = parsedDvhEnheter;
        var filtrerteStateEnheter = orgenhetService.getAllEnheter();
        if (year != null && !year.isEmpty()) {
            log.info("Filtrerer på år:{}", year);
            var agresoiderMedGyldigFomIFilterAaret = parsedDvhEnheter.stream()
                    .filter(e -> e.getGyldigFom().getYear() == Integer.valueOf(year))
                    .map(DvhOrgenhetV2::getAgressoId)
                    .toList();

            filtrerteDvhEnheter = parsedDvhEnheter.stream()
                    .filter(e -> agresoiderMedGyldigFomIFilterAaret.contains(e.getAgressoId()))
                    .toList();

            filtrerteStateEnheter = filtrerteStateEnheter.stream()
                    .filter(e -> agresoiderMedGyldigFomIFilterAaret.contains(e.getAgressoId()))
                    .toList();
        }

        val dvhGroupedByAgressoId = filtrerteDvhEnheter.stream()
                // Grupperer enheter på kombinasjonen agressoId + navn + agresso orgnivå
                .collect(Collectors.groupingBy(DvhOrgenhetV2::getAgressoId));

        val dvhGroupedByAgressoIdAndNivaa = filtrerteDvhEnheter.stream()
            // Grupperer enheter på kombinasjonen agressoId + navn + agresso orgnivå
            .collect(Collectors.groupingBy(dvhOrgenhetV2 -> dvhOrgenhetV2.getAgressoId() + "-" + dvhOrgenhetV2.getOrgNiv()));

        String dvhDuplikaterPaaAgressoId = dvhDuplikaterPaaAgressoId(dvhGroupedByAgressoId, "AgressoId");
        String dvhDuplikaterPaaAgressoIdOgNivaa = dvhDuplikaterPaaAgressoId(dvhGroupedByAgressoIdAndNivaa, "AgressoId og Nivå");

        log.info("Ferdig med å gruppere og liste filtrerte DVH enheter:{}", filtrerteDvhEnheter.size());

        var stateOrgEnheterByAgressoId = filtrerteStateEnheter.stream()
            .filter(e -> !(nonNull(e.getGyldigTom()) && e.getGyldigFom().isEqual(e.getGyldigTom()))) // Filter ut de som slutter samme dag som de starter
            .collect(Collectors.groupingBy(IOrgenhet::getAgressoId));

        var stateOrgEnheterByAgressoIdAndNivaa = filtrerteStateEnheter.stream()
            .filter(e -> !(nonNull(e.getGyldigTom()) && e.getGyldigFom().isEqual(e.getGyldigTom()))) // Filter ut de som slutter samme dag som de starter
            .collect(Collectors.groupingBy( o -> o.getAgressoId() + " - " + o.getOrgNiv()));

        var stateOrgEnheterByAgressoIdAndNivaaOgGyldigFom = filtrerteStateEnheter.stream()
            .filter(e -> !(nonNull(e.getGyldigTom()) && e.getGyldigFom().isEqual(e.getGyldigTom()))) // Filter ut de som slutter samme dag som de starter
            .collect(Collectors.groupingBy( o -> o.getAgressoId() + " - " + o.getOrgNiv() + " - " + o.getGyldigFom()));

        String stateDuplikaterPaaAgressoId = stateDuplikaterPaaAgressoId(stateOrgEnheterByAgressoId, "AgressoId");
        String stateDuplikaterPaaAgressoIdAndNivaa = stateDuplikaterPaaAgressoId(stateOrgEnheterByAgressoIdAndNivaa, "AgressoId og Nivå");
        String stateDuplikaterPaaAgressoIdAndNivaaOgGyldigFom = stateDuplikaterPaaAgressoId(stateOrgEnheterByAgressoIdAndNivaaOgGyldigFom, "AgressoId og Nivå og GyldigFom (Navnebytte?)");

        log.info("Ferdig med å gruppere og liste filtrerte State enheter:{}", parsedDvhEnheter.size());

        return dvhDuplikaterPaaAgressoId + "\n"
            + dvhDuplikaterPaaAgressoIdOgNivaa + "\n"
            + stateDuplikaterPaaAgressoId + "\n"
            + stateDuplikaterPaaAgressoIdAndNivaa + "\n"
            + stateDuplikaterPaaAgressoIdAndNivaaOgGyldigFom;
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
                            .append(nonNull(dvhOrgenhetV2.getGyldigTom()) ? dvhOrgenhetV2.getGyldigTom() : "").append(";")
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
                        result.append("   ")
                            .append(oe.getAgressoId()).append(";")
                            .append(oe.getNavn()).append(";")
                            .append(oe.getOrgNiv()).append(";")
                            .append(oe.getId()).append(";")
                            .append(enhetIdForEnheterIOrganisering.contains(oe.getId()) ? "Y" : "N").append(";")
                            .append(oe.getGyldigFom()).append(";")
                            .append(nonNull(oe.getGyldigTom()) ? oe.getGyldigTom() : "")
                            .append("\n"));
            });
        return result.toString();
    }
}