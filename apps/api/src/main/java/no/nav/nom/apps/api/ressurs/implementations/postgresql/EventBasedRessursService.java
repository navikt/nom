package no.nav.nom.apps.api.ressurs.implementations.postgresql;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadataSerialize;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.hendelse.contract.IHendelseService;
import no.nav.nom.apps.api.postgresql.NavIdentService;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventBasedRessursService {

    private final IHendelseService hendelseService;
    private final RessursHendelseRepository repository;
    private final ObjectMapper objectMapper;
    private final NavIdentService navIdentService;

    public Optional<RessursI> findByFid(UUID fid){
        return Optional.ofNullable(
                convert(repository.findAllByFunksjonellId(fid).stream().map(hendelseService::convert).toList())
        );
    }

    public Optional<RessursI> findByNavIdent(String navIdent){
        return Optional.ofNullable(
                convert(repository.findHendelseByJson(EventBasedRessurs.NAVIDENT_FIELD, navIdent).stream().map(hendelseService::convert).toList())
        );
    }

    public List<RessursI> matchByNavident(String navident) {
        return repository.findHendelserByJsonMatch(EventBasedRessurs.NAVIDENT_FIELD, navident.toLowerCase())
                .stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values().stream().map(this::convert)
                .collect(Collectors.toList());
    }

    public Optional<RessursI> findByPersonIdent(String personIdent){
        return Optional.ofNullable(
                convert(repository.findHendelseByJson(EventBasedRessurs.PERSONIDENT_FIELD, personIdent).stream().map(hendelseService::convert).toList())
        );
    }

    public Map<String, RessursI> findByNavIdentBulk(List<String> navIdenter){
        return repository.findHendelseByJsonBulk(EventBasedRessurs.NAVIDENT_FIELD, navIdenter).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(RessursI::getNavIdent, Function.identity()));
    }

    public Map<String, RessursI> findByPersonIdentBulk(List<String> personIdenter){
        return repository.findHendelseByJsonBulk(EventBasedRessurs.PERSONIDENT_FIELD, personIdenter).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(RessursI::getPersonIdent, Function.identity()));
    }

    public List<RessursI> getAll(){
        return repository.findAllByDataType(HendelseDataType.RESSURS_V1.getDataType()).stream()
                .map(hendelseService::convert)
                .collect(Collectors.groupingBy(IHendelse::getFid))
                .values()
                .stream()
                .map(EventBasedRessurs::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public RessursI opprettRessurs(String personIdent, String navIdent, String navIdentForbokstav, HendelseMetadataSerialize hendelseMetadata) {
        return findByPersonIdent(personIdent).orElseGet(() -> {
            var data = objectMapper.createObjectNode();

            data.put(EventBasedRessurs.NAVIDENT_FIELD, navIdent != null ? navIdent : navIdentService.generateNavIdent(navIdentForbokstav));
            data.put(EventBasedRessurs.PERSONIDENT_FIELD, personIdent);

            val event = hendelseService.createOpprettHendelse(data, hendelseMetadata, HendelseDataType.RESSURS_V1);
            repository.save(hendelseService.convert(event));

            return findByFid(event.getFid()).orElse(null);
        });
    }

    private RessursI convert(List<IHendelse> hendelser){
        return hendelser == null || hendelser.isEmpty() ? null : new EventBasedRessurs(hendelser);
    }
}
