package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.OrgEnhetTypeEndretEventV1;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.springframework.stereotype.Component;

@Component
public class OrgEnhetNomNivaaHandler {

    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;

    public OrgEnhetNomNivaaHandler(
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetCommandService orgEnhetCommandService
    ) {
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
    }

    public void updateNomNivaaWhenOrgEnhetsTypeIsSet(OrgEnhetTypeEndretEventV1 orgEnhetEvent) {
        var maybeOrgEnhet = orgEnhetStateRepository.findByFid(orgEnhetEvent.getFid());
        var maybeAgressoNiva = maybeOrgEnhet.flatMap(OrgEnhetState::agressoOrgNivaa);
        var maybeOrgEnhetType = orgEnhetEvent.getData().orgEnhetsType();

        if (maybeAgressoNiva.isPresent() && maybeOrgEnhetType.isPresent()) {

            var resolvedNomNivaa = NomNivaaMapper.resolve(maybeOrgEnhetType.get(), maybeAgressoNiva.get());

            if (!resolvedNomNivaa.equals(maybeOrgEnhet.get().nomNivaa())) {
                var endreNomNivaa = new EndreNomNivaa(
                    NomNivaaMapper.resolve(maybeOrgEnhetType.get(), maybeAgressoNiva.get()),
                    new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
                );

                orgEnhetCommandService.endreNomNivaa(maybeOrgEnhet.get().fid(), endreNomNivaa);
            }
        }
    }
}
