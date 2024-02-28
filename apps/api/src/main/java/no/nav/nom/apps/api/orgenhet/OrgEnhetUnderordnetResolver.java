package no.nav.nom.apps.api.orgenhet;

import io.getunleash.Unleash;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.organisering.eventsourcing.state.OrganiseringStateRepository;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static no.nav.nom.apps.api.organisering.OrganiseringConfig.UNLEASH_USE_NEW_ORGANISASJON_SOURCE;


@Component
public class OrgEnhetUnderordnetResolver {
    private final Logger logger = LoggerFactory.getLogger(OrgEnhetUnderordnetResolver.class);
    private final DvhRepository dvhRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrganiseringStateRepository organiseringStateRepository;
    private final Unleash unleash;

    public OrgEnhetUnderordnetResolver(
        DvhRepository dvhRepository, OrgEnhetStateRepository orgEnhetStateRepository,
        OrganiseringStateRepository organiseringStateRepository, Unleash unleash
    ) {
        this.dvhRepository = dvhRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.organiseringStateRepository = organiseringStateRepository;
        this.unleash = unleash;
    }

    public List<OrgEnhetState> findUnderordnedeOrgEnheter(OrgEnhetState orgEnhetState) {
        // Ikke returner overordnet/organisering for enheter som er opprettet og avsluttet samme dag
        if (orgEnhetState.endedOnStartDate()) { return Collections.emptyList(); }

        if (unleash.isEnabled(UNLEASH_USE_NEW_ORGANISASJON_SOURCE)) {
            return organiseringStateRepository.getOrganiseringerContainingId(orgEnhetState.id()).stream()
                .filter(organiseringState -> organiseringState.idOver().equals(orgEnhetState.id()))
                .peek(organiseringState -> {
                    if (organiseringState.gyldigTom().toString().equals("2024-02-25")) {
                        logger.warn("Fant en utgåtte organisering med id: {}", organiseringState);
                    }
                })
                .map(organiseringState -> orgEnhetStateRepository.findById(organiseringState.id()))
                .filter(matchedOrgEnhetState -> matchedOrgEnhetState.isPresent() && matchedOrgEnhetState.get().isActive()) // sjekker også orgnhet er aktiv!
                .peek(matchedOrgEnhetState -> {
                    if (matchedOrgEnhetState.get().id().equals("py458c")) {
                        logger.warn("Fant en rart orgEnhetState : {}", matchedOrgEnhetState);
                    }
                })
                .flatMap(Optional::stream)
                .distinct()
                .toList();
        } else {

            final var agressoId = orgEnhetState.agressoId();
            final var orgNiv = orgEnhetState.agressoOrgNivaa();
            final var navn = orgEnhetState.navn();

            if (agressoId.isPresent() && orgNiv.isPresent() && !navn.isBlank()) {
                return dvhRepository.getOrganiseringer(agressoId.get(), orgNiv.get(), navn)
                    .stream()
                    .filter(o ->
                        o.getAgressoIdOver().equals(agressoId.get())
                            && o.getOrgNivOver().equals(orgNiv.get())
                            && o.getNavnOver().equals(navn))
                    .map(o -> String.join("-", o.getAgressoIdUnder(), o.getNavnUnder(), o.getOrgNivUnder()))
                    .map(uniqueId -> orgEnhetStateRepository.findNewestUniqueAgressoId(uniqueId).orElse(null))
                    .filter(Objects::nonNull)
                    .toList();
            }

            logger.debug("Kunne ikke finne underordnede for orgenhet {}", orgEnhetState.id());
            return Collections.emptyList();
        }
    }

    public List<OrgEnhetState> findUnderordnedeOrgEnheter(String fid) {
        var orgEnhetState = orgEnhetStateRepository.findByFid(fid);

        return orgEnhetState.isPresent() ? findUnderordnedeOrgEnheter(orgEnhetState.get()) : Collections.emptyList();
    }
}