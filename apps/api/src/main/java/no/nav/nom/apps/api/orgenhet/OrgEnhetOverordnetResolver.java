package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrgEnhetOverordnetResolver {
    private final Logger logger = LoggerFactory.getLogger(OrgEnhetOverordnetResolver.class);
    private final DvhRepository dvhRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;

    public OrgEnhetOverordnetResolver(DvhRepository dvhRepository, OrgEnhetStateRepository orgEnhetStateRepository) {
        this.dvhRepository = dvhRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
    }

    public Optional<OrgEnhetState> findOverordnetOrgEnhet(OrgEnhetState orgEnhetState) {
        // Ikke returner overordnet/organisering for enheter som er opprettet og avsluttet samme dag
        if (orgEnhetState.endedOnStartDate()) { return Optional.empty(); }

        final var agressoId = orgEnhetState.agressoId();
        final var orgNiv = orgEnhetState.agressoOrgNivaa();
        final var navn = orgEnhetState.navn();

        if (agressoId.isPresent() && orgNiv.isPresent() && !navn.isBlank()) {
            var overordnet = dvhRepository.getOrganiseringer(agressoId.get(), orgNiv.get(), navn)
                .stream()
                .filter(o ->
                    o.getAgressoIdUnder().equals(agressoId.get())
                        && o.getOrgNivUnder().equals(orgNiv.get())
                        && o.getNavnUnder().equals(navn))
                .findFirst();

            var uniqueId = overordnet.map(o ->
                String.join("-", o.getAgressoIdOver(), o.getNavnOver(), o.getOrgNivOver())
            );

            if (uniqueId.isPresent()) {
                return orgEnhetStateRepository.findNewestUniqueAgressoId(uniqueId.get());
            }
        }

        logger.debug("Kunne ikke finne overordnet for orgenhet {}", orgEnhetState.id());
        return Optional.empty();
    }

    public Optional<OrgEnhetState> findOverordnetOrgEnhet(String fid) {
        return orgEnhetStateRepository.findByFid(fid)
            .flatMap(this::findOverordnetOrgEnhet);
    }
}
