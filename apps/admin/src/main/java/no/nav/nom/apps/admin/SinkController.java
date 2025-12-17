package no.nav.nom.apps.admin;

import no.nav.nom.apps.admin.dto.EventSearchParams;
import no.nav.nom.apps.admin.dto.deadletter.DeadLetterDTO;
import no.nav.nom.apps.admin.dto.hendelse.HendelseDTO;
import no.nav.nom.apps.admin.dto.orgenhet.OrgEnhetStateDTO;
import no.nav.nom.apps.admin.dto.ressurs.RessursHendelseKafkaDTO;
import no.nav.nom.apps.admin.dto.ressurs.RessursStateKafkaDTO;
import no.nav.nom.apps.admin.events.hendelser.HendelserApiStateRepository;
import no.nav.nom.apps.admin.events.orgenhet.api.OrgEnhetStateApiRepository;
import no.nav.nom.apps.admin.events.orgenhet.eventsource.OrgenhetEvtsrcRepository;
import no.nav.nom.apps.admin.events.orgtilknytning.deadletter.DeadLetterRepository;
import no.nav.nom.apps.admin.events.orgtilknytning.eventsource.OrgtilknytningEvtsrcRepository;
import no.nav.nom.apps.admin.events.ressurs.api.RessursApiStateRepository;
import no.nav.nom.apps.admin.events.ressurs.api.RessursHendelseApiRepository;
import no.nav.nom.apps.admin.events.ressurs.eventsource.RessursEvtsrcRepository;
import no.nav.nom.apps.admin.events.tilgang.TilgangEvtsrcRepository;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static no.nav.nom.apps.admin.dto.EventSearchParams.eventFilter;
import static no.nav.nom.apps.admin.utils.safeSubList;

@RestController
@RequestMapping("/sinks")
public class SinkController {

    private static final int NUMBER_OF_EVENTS = 500;
    
    private final RessursEvtsrcRepository ressursEvtsrcRepository;
    private final OrgenhetEvtsrcRepository orgenhetEvtsrcRepository;
    private final TilgangEvtsrcRepository tilgangEvtsrcRepository;
    private final RessursApiStateRepository ressursApiStateRepository;
    private final OrgEnhetStateApiRepository orgEnhetStateApiRepository;
    private final HendelserApiStateRepository hendelserApiStateRepository;
    private final OrgtilknytningEvtsrcRepository orgtilknytningEvtsrcRepository;
    private final RessursHendelseApiRepository ressursHendelseApiRepository;
    private final DeadLetterRepository deadLetterRepository;

    public SinkController(
        RessursEvtsrcRepository ressursEvtsrcRepository,
        OrgenhetEvtsrcRepository orgenhetEvtsrcRepository,
        TilgangEvtsrcRepository tilgangEvtsrcRepository,
        RessursApiStateRepository ressursApiStateRepository,
        OrgEnhetStateApiRepository orgEnhetStateApiRepository,
        HendelserApiStateRepository hendelserApiStateRepository,
        OrgtilknytningEvtsrcRepository orgtilknytningEvtsrcRepository,
        RessursHendelseApiRepository ressursHendelseApiRepository,
        DeadLetterRepository deadLetterRepository
    ) {
        this.ressursEvtsrcRepository = ressursEvtsrcRepository;
        this.orgenhetEvtsrcRepository = orgenhetEvtsrcRepository;
        this.tilgangEvtsrcRepository = tilgangEvtsrcRepository;
        this.ressursApiStateRepository = ressursApiStateRepository;
        this.orgEnhetStateApiRepository = orgEnhetStateApiRepository;
        this.hendelserApiStateRepository = hendelserApiStateRepository;
        this.orgtilknytningEvtsrcRepository = orgtilknytningEvtsrcRepository;
        this.ressursHendelseApiRepository = ressursHendelseApiRepository;
        this.deadLetterRepository = deadLetterRepository;
    }


    @GetMapping(value = "/ressursEvents")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('READONLY')")
    public List<RessursEvent> getRessursEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<RessursEvent> events = getAllMatchingRessursEvents(eventSearchParams);
        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/orgenhetEvents")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('READONLY')")
    public List<OrgEnhetEvent> getOrgEnhetEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<OrgEnhetEvent> events = orgenhetEvtsrcRepository.getOrgenhetEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/orgtilknytningEvents")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('READONLY')")
    public List<OrgtilknytningEvent> getOrgtilknytningEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<OrgtilknytningEvent> events = getAllMatchingOrgtilknytningEvents(eventSearchParams);

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/tilgangEvents")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('READONLY')")
    public List<AuthorizationEvent> getTilgangEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<AuthorizationEvent> events = tilgangEvtsrcRepository.getTilgangEvtsrc().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/apiRessursEvents")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('READONLY')")
    public List<RessursStateKafkaDTO> getEksternRessursEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<RessursStateKafkaDTO> events = ressursApiStateRepository.getRessursStateTopic().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/apiOrgenhetEvents")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('READONLY')")
    public List<OrgEnhetStateDTO> getApiOrgenhetEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<OrgEnhetStateDTO> events = orgEnhetStateApiRepository.getOrgtilknytningApiState().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/apiHendelserEvents")
    public List<HendelseDTO> getApiHendelserEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<HendelseDTO> events = hendelserApiStateRepository.getHendelserApiState().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/apiRessursHendelseEvents")
    public List<RessursHendelseKafkaDTO> getApiRessursHendelseEvents(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<RessursHendelseKafkaDTO> events = ressursHendelseApiRepository.getRessursHendelser().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    @GetMapping(value = "/remedyDeadletter")
    @PreAuthorize("hasAuthority('ADMIN') or hasAuthority('READONLY')")
    public List<DeadLetterDTO> getRemedyDeadletter(@Valid EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        List<DeadLetterDTO> events = deadLetterRepository.getDeadLetters().stream().filter(eventFilter(eventSearchParams)).toList();

        return safeSubList(events.reversed(), fromOffset, fromOffset + NUMBER_OF_EVENTS);
    }

    private List<RessursEvent> getAllMatchingRessursEvents(EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        String regexNavident = "[A-Za-z][1-9]\\d{5}";

        Pattern pattern = Pattern.compile(regexNavident);
        Matcher matcher = pattern.matcher(eventSearchParams.getFilterText());
        Set<String> eventFids = new HashSet<>();
        List<RessursEvent> events;
        if (matcher.find()) {
            eventFids.addAll(ressursEvtsrcRepository.getNavidentToFidMap().get(matcher.group()));
        }

        if (!eventFids.isEmpty()) {
            events = new ArrayList<>();
            eventFids.forEach(fid -> events.addAll(ressursEvtsrcRepository.getRessursEvtsrc().stream()
                .filter(event -> event.getFid().equals(fid))
                .toList()));
        } else {
            events = ressursEvtsrcRepository.getRessursEvtsrc().stream()
                .filter(eventFilter(new EventSearchParams(fromOffset, eventSearchParams.getFilterText())))
                .toList();
        }
        return events;
    }

    private List<OrgtilknytningEvent> getAllMatchingOrgtilknytningEvents(EventSearchParams eventSearchParams) {
        int fromOffset = eventSearchParams.getFromOffset();
        String regexNavident = "[A-Za-z][1-9]\\d{5}";

        Pattern pattern = Pattern.compile(regexNavident);
        Matcher matcher = pattern.matcher(eventSearchParams.getFilterText());
        Set<String> eventFids = new HashSet<>();
        List<OrgtilknytningEvent> events;
        if (matcher.find()) {
            var oter = orgtilknytningEvtsrcRepository.getNavidentToFidMap().get(matcher.group());
            if (oter != null) {
                eventFids.addAll(orgtilknytningEvtsrcRepository.getNavidentToFidMap().get(matcher.group()));
            }
        }

        if (!eventFids.isEmpty()) {
            events = new ArrayList<>();
            eventFids.forEach(fid -> events.addAll(orgtilknytningEvtsrcRepository.getOrgtilknytningEvtsrc().stream()
                .filter(event -> event.getFid().equals(fid))
                .toList()));
        } else {
            events = orgtilknytningEvtsrcRepository.getOrgtilknytningEvtsrc().stream()
                .filter(eventFilter(new EventSearchParams(fromOffset, eventSearchParams.getFilterText())))
                .toList();
        }
        return events;
    }
}
