package no.nav.nom.apps.api.orgenhet.api.admin;

import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.AvvikleOrgEnhet;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import no.nav.nom.libs.state.organisering.OrganiseringStateRepository;
import no.nav.nom.libs.state.orgenhet.OrgEnhetStateRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.stream.Collectors;

import static java.util.Objects.isNull;

@Service
@RequiredArgsConstructor
public class SetteGyldigTomPaaOrgEnheterUtenOrganisering {
    private final OrgEnhetCommandService orgEnhetCommandService;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;

    public String settGyldigTomPaaOrgEnheterUtenOrganisering(boolean dryRun) {
        var orgEnheterUtenGyldigTom = orgEnhetStateRepository.getAll()
            .stream()
            .filter(orgEnhetState -> isNull(orgEnhetState.gyldigTom()))
            .filter(orgEnhetState -> !orgEnhetState.gyldigFom().isAfter(LocalDate.now()))
            .collect(Collectors.toMap(OrgEnhetState::id, orgEnhetState -> orgEnhetState));

        var organiseringer = organiseringStateRepository.getHistoricOrganisering();

        var orgEnheterToSetGyldigTom = orgEnheterUtenGyldigTom.entrySet()
            .stream()
            .filter(stringOrgEnhetStateEntry -> !organiseringer.containsKey(stringOrgEnhetStateEntry.getKey()))
            .toList();

        StringBuilder antallAvsluttet = new StringBuilder();
        antallAvsluttet.append(String.format("Antall orgEnhet funnet uten tilhørende organisering objekt: %d", orgEnheterToSetGyldigTom.size())).append("\n");
        antallAvsluttet.append("fid;orgEnhetId;orgEnhetNavn;gyldigFom;gyldigTom\n");

        orgEnheterToSetGyldigTom.forEach(orgEnhetStateMap -> {
            var orgEnhetState = orgEnhetStateMap.getValue();
            antallAvsluttet.append(String.format(
                "%s;%s;%s;%s;%s\n",
                orgEnhetState.fid(),
                orgEnhetState.id(),
                orgEnhetState.navn(),
                orgEnhetState.gyldigFom(),
                orgEnhetState.gyldigTom()
            ));
            if (!dryRun) {
                orgEnhetCommandService.avvikleOrgEnhet(
                    orgEnhetState.fid(),
                    new AvvikleOrgEnhet(
                        orgEnhetState.id(),
                        LocalDate.now().minusDays(1),
                        new InputMetadata(
                            "/api/admin/orgenhet/gyldig-tom-historiske-uten-organisering",
                            "gyldigTomHistoriskeUtenOrganisering",
                            orgEnhetState.fid(),
                            new Origination(
                                SecurityUtil.getAuthenticatedNavIdent(),
                                "Admin swagger/rest"
                            )
                        )
                    )
                );
            }
        });
        return antallAvsluttet.toString();
    }
}
