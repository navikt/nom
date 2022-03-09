package no.nav.nom.apps.api.ressurs.implementations;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.repository.NavIdentService;
import no.nav.nom.apps.api.repository.hendelse.Hendelse;
import no.nav.nom.apps.api.repository.hendelse.HendelseRepository;
import no.nav.nom.apps.api.repository.hendelse.Hendelse.DataType;
import no.nav.nom.apps.api.ressurs.contract.RessursI;
import no.nav.nom.apps.api.ressurs.contract.IRessursService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessursInterface;
import no.nav.nom.apps.api.ressurs.implementations.postgresql.EventBasedRessurs;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RessursService implements IRessursService {

    private final ObjectMapper objectMapper;
    private final HendelseRepository hendelseRepository;
    private final NavIdentService navIdentService;
    private final DvhRessursInterface dvhRessursInterface;

    public Optional<RessursI> findByFid(UUID fid){
        return Optional.ofNullable(
                convert(hendelseRepository.findAllByFunksjonellId(fid))
        );
    }

    @Override
    public Optional<RessursI> findByNavIdent(String navIdent){
        return Optional.ofNullable(
                convert(hendelseRepository.findHendelseByJson(EventBasedRessurs.NAVIDENT_FIELD, navIdent))
        );
    }

    @Override
    public List<RessursI> matchByNavident(String navident) {
        return hendelseRepository
                .findHendelserByJsonMatch(EventBasedRessurs.NAVIDENT_FIELD, navident.toLowerCase())
                .stream().collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream().map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<RessursI> findByPersonIdent(String personIdent){
        return Optional.ofNullable(
                convert(hendelseRepository.findHendelseByJson(EventBasedRessurs.PERSONIDENT_FIELD, personIdent))
        );
    }

    public Map<String, RessursI> findByNavIdentBulk(List<String> navIdenter){
        return hendelseRepository.findHendelseByJsonBulk(EventBasedRessurs.NAVIDENT_FIELD, navIdenter).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(RessursI::getNavIdent, Function.identity()));
    }

    public Map<String, RessursI> findByPersonIdentBulk(List<String> personIdenter){
        return hendelseRepository.findHendelseByJsonBulk(EventBasedRessurs.PERSONIDENT_FIELD, personIdenter).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(RessursI::getPersonIdent, Function.identity()));
    }

    @Override
    public List<RessursI> getAll(){
        return hendelseRepository.findAllByDataType(DataType.RESSURS_V1).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RessursI opprettRessurs(String personIdent, String navIdent, String navIdentForbokstav) {
        return findByPersonIdent(personIdent).orElseGet(() -> {
            var data = objectMapper.createObjectNode();

            data.put(EventBasedRessurs.NAVIDENT_FIELD, navIdentService.generateNavIdent(navIdentForbokstav));
            data.put(EventBasedRessurs.PERSONIDENT_FIELD, personIdent);

            val hendelse = Hendelse.builder()
                    .hendelseID(UUID.randomUUID())
                    .funksjonellId(UUID.randomUUID())
                    .ordreId(UUID.randomUUID())
                    .tidligereId(null)
                    .dataType(DataType.RESSURS_V1)
                    .hendelseType(Hendelse.HendelseType.OPPRETTET)
                    .data(data)
                    .opprettetTidspunkt(LocalDateTime.now())
                    .build();

            return convert(List.of(hendelseRepository.save(hendelse)));
        });
    }

    public void createEndreTilNyttPersonnummerHendelse(UUID fid, String personIdent) {
        var data = objectMapper.createObjectNode();
        data.put(EventBasedRessurs.PERSONIDENT_FIELD, personIdent);

        val hendelse = Hendelse.builder()
                .hendelseID(UUID.randomUUID())
                .funksjonellId(fid)
                .ordreId(UUID.randomUUID())
                .tidligereId(null)
                .dataType(DataType.RESSURS_V1)
                .hendelseType(Hendelse.HendelseType.ENDRET)
                .data(data)
                .opprettetTidspunkt(LocalDateTime.now())
                .build();

        hendelseRepository.save(hendelse);
    }

    public void createEndreTilNyttPersonnummerHendelser(Map<UUID, String> personIdentByFid) {
        hendelseRepository.saveAll(
                personIdentByFid.entrySet().stream()
                        .map(h -> {
                            val data = objectMapper.createObjectNode();
                            data.put(EventBasedRessurs.PERSONIDENT_FIELD, h.getValue());

                            return Hendelse.builder()
                                    .hendelseID(UUID.randomUUID())
                                    .funksjonellId(h.getKey())
                                    .ordreId(UUID.randomUUID())
                                    .tidligereId(null)
                                    .dataType(DataType.RESSURS_V1)
                                    .hendelseType(Hendelse.HendelseType.ENDRET)
                                    .data(data)
                                    .opprettetTidspunkt(LocalDateTime.now())
                                    .build();
                        }).toList()
        );
    }

    public void createOpprettetHendelser(Map<String, String> personIdentByNavident) {
        val listToSave = personIdentByNavident.entrySet().stream()
                .map(h -> {
                    val data = objectMapper.createObjectNode();
                    data.put(EventBasedRessurs.NAVIDENT_FIELD, h.getKey());
                    data.put(EventBasedRessurs.PERSONIDENT_FIELD, h.getValue());
                    return Hendelse.builder()
                            .hendelseID(UUID.randomUUID())
                            .funksjonellId(UUID.randomUUID())
                            .ordreId(UUID.randomUUID())
                            .tidligereId(null)
                            .dataType(DataType.RESSURS_V1)
                            .hendelseType(Hendelse.HendelseType.OPPRETTET)
                            .data(data)
                            .opprettetTidspunkt(LocalDateTime.now())
                            .build();
                }).toList();

        hendelseRepository.saveAll(listToSave);
    }

    private RessursI convert(List<Hendelse> hendelser){
        return hendelser == null || hendelser.isEmpty() ? null : new EventBasedRessurs(hendelser);
    }

    @Override
    public Ressurs convertToGraphQlResponse(RessursI ressurs) {
        return Ressurs.builder()
                .nomId(ressurs.getId())
                .navIdent(ressurs.getNavIdent())
                .personIdent(ressurs.getPersonIdent())
                .ressurstyper(dvhRessursInterface.getRessurstypeGraphQlResponse(ressurs.getNavIdent()))
                .build();
    }
}
