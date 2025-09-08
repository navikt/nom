package no.nav.nom.apps.api.orgtilknytning.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.SwaggerConfig;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.OrgEnhetCommandService;
import no.nav.nom.apps.api.orgenhet.eventsourcing.producer.commands.KorrigerOrgEnhet;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.FjernOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Slf4j
@Tag(name = "Admin for korrigeringer")
@RestController
@RequestMapping("/api/admin/SlaaSammenOrgtilknytningerPaaDuplikatEnhet")
@PreAuthorize("hasAuthority('ADMIN')")
public class SlaaSammenOrgtilknytningerPaaDuplikatEnhet {

    private final OrgenhetService orgenhetService;
    private final OrgtilknytningService orgtilknytningService;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final OrgEnhetCommandService orgEnhetCommandService;

    public SlaaSammenOrgtilknytningerPaaDuplikatEnhet(
        OrgenhetService orggenhetService, OrgtilknytningService orgtilknytningService,
        OrgtilknytningCommandService orgtilknytningCommandService,
        OrgEnhetCommandService orgEnhetCommandService
    ) {
        this.orgenhetService = orggenhetService;
        this.orgtilknytningService = orgtilknytningService;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.orgEnhetCommandService = orgEnhetCommandService;
    }

    @Operation(
        description = "Når enhet skulle hatt annet navn (skrivefeil etc) i utgangspunktet kan man slå sammen OT på feilaktig enhet med korrekt enhet, " +
            "slik at ressursene sitter kun igjen med OT på korrekt enhet. Den korrekte enheten vil få startdato fra den første feilaktige enheten",
        security = @SecurityRequirement(name = SwaggerConfig.BEARER_TOKEN)
    )
    @PostMapping()
    public String SlaaSammenOTerForEnheter(@RequestParam String gammelFeilEnhetId,
                                           @RequestParam String nyKorrektEnhetId,
                                           @Parameter(description = "Skriv CONFIRM for å bekrefte!") @RequestParam(defaultValue = "DRY RUN") String confirm) {
        boolean commit = "CONFIRM".equals(confirm);

        var gammelEnhet = orgenhetService.getById(gammelFeilEnhetId).orElseThrow(() -> new IllegalArgumentException("Angitt gammel enhet finnes ikke"));
        var nyEnhet = orgenhetService.getById(nyKorrektEnhetId).orElseThrow(() -> new IllegalArgumentException("Angitt ny enhet finnes ikke"));

        if (nyEnhet.getGyldigFom().isBefore(gammelEnhet.getGyldigFom())) {
            throw new IllegalArgumentException("Ny enhet er gyldig før gammel enhet.");
        }

        var nyGyldigFom = gammelEnhet.getGyldigFom();

        StringBuffer result = new StringBuffer();
        if (commit) {
            result.append("Kjører med CONFIRM - alt endres").append("\n");
        } else {
            result.append("Kjører DRY RUN - ingenting endres").append("\n");
        }

        result.append("\n");
        result.append("Slår sammen OT'er i Gammel enhet (%s) og Ny enhet (%s) ".formatted(enhetToString(gammelEnhet), enhetToString(nyEnhet))).append("\n");
        result.append("Ny enhet vil få ny gyldigFom: ").append(nyGyldigFom).append("\n");
        result.append("\n");

        var orgtilknytningerForGammelEnhet = orgtilknytningService.getByEnhet(gammelEnhet.getId());
        var orgtilknytningerForNyEnhet = orgtilknytningService.getByEnhet(nyEnhet.getId());

        var ressurserForGammelEnhet = orgtilknytningerForGammelEnhet.stream()
            .map(OrgtilknytningState::navident)
            .toList();

        List<OrgtilknytningState> oterAaFjerne = new ArrayList<>();
        Map<OrgtilknytningState, LocalDate> oterAaEndreStartdatoPaa = new HashMap<>();
        List<OrgtilknytningState> gamleOterSomSkalOpprettesPaaNyEnhet = new ArrayList<>(); // Ressurs har ikke OT på Ny enhet her i utgangspunktet - så det er ikke noe å slå sammen med
        Map<OrgtilknytningState, OrgtilknytningState> oterSomIkkeHengerSammen = new HashMap<>();

        ressurserForGammelEnhet.forEach(navident -> {
            result.append("Ressurs: ").append(navident).append("\n");

            var otIGammelEnhet = orgtilknytningerForGammelEnhet.stream()
                .filter(ot -> ot.navident().equals(navident))
                .findFirst().get();

            var otINyEnhet = orgtilknytningerForNyEnhet.stream().
                filter(ot -> ot.navident().equals(navident))
                .findFirst();

            result.append("\t").append("OT på gammel enhet: ").append(otToString(otIGammelEnhet)).append("\n");

            otINyEnhet.ifPresentOrElse(
                nyOt -> { // Slå sammen
                    result.append("\t").append("OT på ny enhet: ").append(otToString(nyOt)).append("\n");
                    if (otHengerSammen(otIGammelEnhet, nyOt, result)) {
                        result.append("\t").append("Fjerner OT i gammel enhet, endrer startdato i ny OT").append("\n");
                        oterAaFjerne.add(otIGammelEnhet);
                        oterAaEndreStartdatoPaa.put(nyOt, otIGammelEnhet.gyldigFom());
                    } else {
                        oterSomIkkeHengerSammen.put(otIGammelEnhet, nyOt);
                    }
                },
                () -> { // Kun opprette OT på ny enhet
                    result.append("\t").append("Fjerner OT i gammel enhet, oppretter identisk i ny enhet").append("\n");
                    oterAaFjerne.add(otIGammelEnhet);
                    gamleOterSomSkalOpprettesPaaNyEnhet.add(otIGammelEnhet);
                }
            );
        });

        result.append("\n");
        result.append("EvtSrc commands:").append("\n");

        var metadata = getMetadata(gammelFeilEnhetId, nyKorrektEnhetId);
        endreOrgenhetGyldigFom(nyEnhet, nyGyldigFom, metadata, commit, result);
        oterAaEndreStartdatoPaa.forEach( (ot, gyldigFom) -> endreStartdato(ot, gyldigFom, metadata, commit, result));
        oterAaFjerne.forEach(ot -> fjernOrgtilknytning(ot, metadata, commit , result));
        gamleOterSomSkalOpprettesPaaNyEnhet.forEach(ot -> opprettOrgtilknytning(ot, nyEnhet, metadata, commit, result));

        result.append("\n");
        result.append("Totalt:").append("\n");
        result.append("\t").append("Antall OTer endret startdato: ").append(oterAaEndreStartdatoPaa.size()).append("\n");
        result.append("\t").append("Antall OTer fjernet: ").append(oterAaFjerne.size()).append("\n");
        result.append("\t").append("Antall OTer opprettet: ").append(gamleOterSomSkalOpprettesPaaNyEnhet.size()).append("\n");
        result.append("\t").append("Antall OTer som ikke kan slås sammen: ").append(oterSomIkkeHengerSammen.size()).append("\n");

        return result.toString();
    }

    private void endreOrgenhetGyldigFom(IOrgenhet nyEnhet, LocalDate nyGyldigFom, InputMetadata metadata, boolean commit, StringBuffer result) {
        var endreEnhetCmd = new KorrigerOrgEnhet(
            nyEnhet.getId(),
            nyEnhet.getNavn(),
            nyGyldigFom,
            nyEnhet.getGyldigTom(),
            nyEnhet.getOverordnetOrgEnhetFid(),
            null,
            metadata
        );

        result.append("\t").append("Endre Orgenhet startdato (fid: %s): ".formatted(nyEnhet.getFid())).append(endreEnhetCmd).append("\n");
        if (commit) {
            orgEnhetCommandService.korrigerOrgEnhet(nyEnhet.getFid(), endreEnhetCmd);
        }
    }

    private void endreStartdato(OrgtilknytningState ot, LocalDate gyldigFom, InputMetadata metadata, boolean commit, StringBuffer result) {
        var endreOtDatoCmd = new EndreOrgtilknytningDato(
            ot.navident(),
            ot.orgEnhetId(),
            gyldigFom,
            ot.gyldigTom(),
            metadata,
            null
        );

        result.append("\t").append("Endre OT startdato (fid: %s): ".formatted(ot.fid())).append(endreOtDatoCmd).append("\n");
        if (commit) {
            orgtilknytningCommandService.endreOrgtilknytningDato(ot.fid(), endreOtDatoCmd);
        }
    }

    private void fjernOrgtilknytning(OrgtilknytningState ot, InputMetadata metadata, boolean commit, StringBuffer result) {
        var fjerneOtCmd = new FjernOrgtilknytning(
            ot.navident(),
            ot.orgEnhetId(),
            ot.gyldigFom(),
            ot.gyldigTom(),
            metadata,
            null
        );

        result.append("\t").append("Fjerner OT (fid: %s): ".formatted(ot.fid())).append(fjerneOtCmd).append("\n");
        if (commit) {
            orgtilknytningCommandService.fjernOrgtilknytning(ot.fid(), fjerneOtCmd);
        }
    }

    private void opprettOrgtilknytning(OrgtilknytningState ot, IOrgenhet nyEnhet, InputMetadata metadata, boolean commit, StringBuffer result) {
        var opprettOtCmd = new OpprettOrgtilknytning(
            ot.navident(),
            ot.orgEnhetId(),
            ot.sektor(),
            ot.gyldigFom(),
            null,
            metadata,
            null
        );

        String fid;
        result.append("\t").append("Opprett OT fra 'gammelOT' (fid: %s): ".formatted(ot.fid())).append(opprettOtCmd).append("\n");
        if (commit) {
            fid = orgtilknytningCommandService.opprettOrgtilknytning(opprettOtCmd);
        } else {
            fid = "DRY RUN fid";
        }

        if (nonNull(ot.gyldigTom())) {
            var avvikleOtDatoCmd = new AvvikleOrgtilknytning(
                ot.navident(),
                nyEnhet.getId(),
                ot.gyldigTom(),
                metadata,
                null
            );
            result.append("\t").append("Avvikler ny OT (fid: %s): ".formatted(fid)).append(avvikleOtDatoCmd).append("\n");
            if (commit) {
                orgtilknytningCommandService.avvikleOrgtilknytning(fid, avvikleOtDatoCmd);
            }
        }

    }

    private boolean otHengerSammen(OrgtilknytningState gammelOt, OrgtilknytningState nyOt, StringBuffer result) {
        if (isNull(gammelOt.gyldigTom())) {
            result.append("\t").append("Gammel OT har ingen gyldig tom - kan ikke slå sammen").append("\n");
            return false;
        }
        if (!gammelOt.gyldigTom().plusDays(1).isEqual(nyOt.gyldigFom())) {
            result.append("\t").append("Gammel og ny OT henger ikke sammen på dato - kan ikke slå sammen").append("\n");
            return false;
        }
        if (!Objects.equals(gammelOt.orgtilknytningType(), nyOt.orgtilknytningType())) {
            result.append("\t").append("Gammel og ny OT har ulik tilknytningstype - kan ikke slå sammen").append("\n");
            return false;
        }
        if (!Objects.equals(gammelOt.sektor(), nyOt.sektor())) {
            result.append("\t").append("Gammel og ny OT har ulik sektor - kan ikke slå sammen").append("\n");
            return false;
        }
        return true;
    }


    private InputMetadata getMetadata(String gammelFeilEnhetId, String nyKorrektEnhetId) {
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode node = mapper.createObjectNode();
        node.put("Gammel enhetId", gammelFeilEnhetId);
        node.put("Ny enhetId", nyKorrektEnhetId);

        return new InputMetadata(
            "Swagger",
            "SlaaSammenOrgtilknytningerPaaDuplikatEnhet",
            node,
            new Origination("nom-api", "nom-api")
        );
    }

    private String enhetToString(IOrgenhet enhet) {
        return "%s %s - Gyldig [%s - %s]".formatted(
            enhet.getId(),
            enhet.getNavn(),
            enhet.getGyldigFom(),
            enhet.getGyldigTom()
        );
    }

    private String otToString(OrgtilknytningState ot) {
        return "%s %s %s Gyldig [%s - %s]".formatted(
            ot.fid(),
            ot.orgtilknytningType(),
            ot.sektor(),
            ot.gyldigFom(),
            ot.gyldigTom()
        );
    }
}
