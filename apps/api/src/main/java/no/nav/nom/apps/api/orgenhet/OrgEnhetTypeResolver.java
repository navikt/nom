package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class OrgEnhetTypeResolver {

    private final Logger logger = LoggerFactory.getLogger(OrgEnhetTypeResolver.class);

    private final DvhRepository dvhRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;

    public OrgEnhetTypeResolver(
        DvhRepository dvhRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetCommandService orgEnhetCommandService
    ) {
        this.dvhRepository = dvhRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;
    }

    public void mapOrgEnhetType() {
        var orgEnhetStateList = orgEnhetStateRepository.getAll();

        // Sett DIR hvis agressoOrgenhetType er Dir og lag en wrapper instanse
        var wrapperList = orgEnhetStateList.
            stream()
            .map(this::mapFrom)
            .toList();

        wrapperList
            .stream()
            .filter(n -> n.orgNivaa != null && !n.orgNivaa.matches("ORGNIV0|ORGENHET")) // Fjern topp og bunn nivåene
            .sorted((w1, w2) -> w2.orgNivaa.compareTo(w1.orgNivaa))
            .forEach(wrapper -> {
                var children = wrapperList.stream().filter(c -> c.isUnderordnetTo(wrapper.orgEnhetState)).toList();
                var dirCount = children.stream().filter(c -> OrgEnhetsType.DIR.equals(c.orgEnhetsType)).count();
                var isAlreadyDirInState = wrapper.orgEnhetState.orgEnhetsType().map(type -> type.equals(OrgEnhetsType.DIR)).orElse(false);

                // hvis antallet på underordnede er over 50%
                if ((float) dirCount / children.size() >= 0.5) {
                    wrapper.orgEnhetsType = OrgEnhetsType.DIR;

                    if (!isAlreadyDirInState) {
                        logger.debug("OrgEnhet type settes til DIR for {} pga underordnede som er DIR", wrapper.orgEnhetState.uniqueAgressoId());
                        endreOrgEnhetsType(wrapper.orgEnhetState.fid(), OrgEnhetsType.DIR);
                    }
                } else {
                    if (isAlreadyDirInState) {
                        logger.debug("Fjerner OrgEnhet type DIR for {} pga underordnede", wrapper.orgEnhetState.uniqueAgressoId());
                        endreOrgEnhetsType(wrapper.orgEnhetState.fid(), null);
                    }
                }
            });
    }

    private void endreOrgEnhetsType(String fid, OrgEnhetsType type) {
        final var endreType = new EndreOrgEnhetsType(
            Optional.ofNullable(type),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );

        orgEnhetCommandService.endreOrgEnhetsType(fid, endreType);
    }

    static class OrgEnhetStateWrapper {
        final OrgEnhetState orgEnhetState;
        OrgEnhetState overordnetOrgEnhetState;
        String orgNivaa;
        OrgEnhetsType orgEnhetsType;

        OrgEnhetStateWrapper(OrgEnhetState orgEnhetState) {
            this.orgEnhetState = orgEnhetState;
        }

        boolean isUnderordnetTo(OrgEnhetState overordnet){
            return overordnet.equals(overordnetOrgEnhetState);
        }
    }

    private OrgEnhetStateWrapper mapFrom(OrgEnhetState orgEnhetState) {
        var overordent = findOverordnetOrgEnhet(orgEnhetState);
        var wrapper = new OrgEnhetStateWrapper(orgEnhetState);
        wrapper.overordnetOrgEnhetState = overordent.orElse(null);
        wrapper.orgNivaa = orgEnhetState.agressoOrgNivaa().orElse(null);

        if (orgEnhetState.agressoOrgNivaa().map(niva -> niva.equals("ORGENHET")).orElse(false)) {
            var isDir = orgEnhetState.agressoOrgenhetType().map(type -> type.equals("Dir")).orElse(false);
            var isAlreadyDirInState = orgEnhetState.orgEnhetsType().map(type -> type.equals(OrgEnhetsType.DIR)).orElse(false);

            if (isDir) {
                wrapper.orgEnhetsType = OrgEnhetsType.DIR;

                if (!isAlreadyDirInState) {
                    logger.debug("OrgEnhet type settes til DIR for {}", wrapper.orgEnhetState.uniqueAgressoId());
                    endreOrgEnhetsType(wrapper.orgEnhetState.fid(), OrgEnhetsType.DIR);
                }
            } else {
                if (isAlreadyDirInState) {
                    logger.debug("Fjerner OrgEnhet type DIR for {}", wrapper.orgEnhetState.uniqueAgressoId());
                    endreOrgEnhetsType(wrapper.orgEnhetState.fid(), null);
                }
            }
        }

        return wrapper;
    }

    private Optional<OrgEnhetState> findOverordnetOrgEnhet(OrgEnhetState orgEnhetState) {
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

        logger.warn("Kunne ikke finne overordnet for orgenhet {}", orgEnhetState.id());
        return Optional.empty();
    }

}
