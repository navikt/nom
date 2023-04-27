package no.nav.nom.apps.api.orgenhet;

import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreNomNivaa;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.EndreOrgEnhetsType;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.contract.nom.NomNivaa;
import no.nav.nom.contract.nom.OrgEnhetsType;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgEnhetState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class OrgEnhetTypeAndNivaaResolver {

    private final Logger logger = LoggerFactory.getLogger(OrgEnhetTypeAndNivaaResolver.class);

    private final DvhRepository dvhRepository;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final OrgEnhetCommandService orgEnhetCommandService;

    //Unntakstilfelle, orgenheter som mappes til DIR men skal ikke være det. TODO: fiks
    private final List<String> excludeNonDirAgressoId;

    public OrgEnhetTypeAndNivaaResolver(
        DvhRepository dvhRepository,
        OrgEnhetStateRepository orgEnhetStateRepository,
        OrgEnhetCommandService orgEnhetCommandService
    ) {
        this.dvhRepository = dvhRepository;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
        this.orgEnhetCommandService = orgEnhetCommandService;

        excludeNonDirAgressoId = List.of("7");
    }

    public void resolve() {
        logger.info("Starts resolving orgenhet type and nom nivaa");

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
                var isExcluded = wrapper.orgEnhetState.agressoId().map(agressoId -> excludeNonDirAgressoId.contains(agressoId)).orElse(false);

                // hvis antallet på underordnede er over 50%
                if ((float) dirCount / children.size() >= 0.5 && !isExcluded) {
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

        //Etter typer er satt, gå gjennom igjen og sett nom nivå
        wrapperList
            .forEach(wrapper ->  {
                resolveNomNivaa(wrapper);
            });

        logger.info("Done resolving orgenhet type and nom nivaa");
    }

    private void resolveNomNivaa(OrgEnhetStateWrapper wrappedOrgEnhet) {
        var current = wrappedOrgEnhet.orgEnhetState.nomNivaa();
        var resolved = NomNivaaMapper.resolve(wrappedOrgEnhet.orgEnhetsType, wrappedOrgEnhet.orgNivaa);

        if(!current.equals(resolved)){
            logger.debug("OrgEnhet {} nom nivaa settes til {}", wrappedOrgEnhet.orgEnhetState.uniqueAgressoId(), resolved.orElse(null));
            endreNomNivaa(wrappedOrgEnhet.orgEnhetState.fid(), resolved.orElse(null));
        }
    }

    private void endreOrgEnhetsType(String fid, OrgEnhetsType type) {
        final var endreType = new EndreOrgEnhetsType(
            Optional.ofNullable(type),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );

        orgEnhetCommandService.endreOrgEnhetsType(fid, endreType);
    }

    private void endreNomNivaa(String fid, NomNivaa nomNivaa) {
        final var endreNomNivaa = new EndreNomNivaa(
            Optional.ofNullable(nomNivaa),
            new InputMetadata("nom-api BatchJob", "Datavarehus", null, new Origination("nom-api", "nom-api"))
        );

        orgEnhetCommandService.endreNomNivaa(fid, endreNomNivaa);
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
            var isActive = orgEnhetState.gyldigTom().map(gyldigTom -> gyldigTom.isAfter(LocalDate.now())).orElse(true);

            if (isDir & isActive) {
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

        logger.debug("Kunne ikke finne overordnet for orgenhet {}", orgEnhetState.id());
        return Optional.empty();
    }

}
