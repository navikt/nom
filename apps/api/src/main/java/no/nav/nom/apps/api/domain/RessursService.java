package no.nav.nom.apps.api.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.repository.Hendelse;
import no.nav.nom.apps.api.repository.HendelseRepository;
import no.nav.nom.apps.api.repository.Hendelse.DataType;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RessursService {

    private final ObjectMapper objectMapper;
    private final HendelseRepository hendelseRepository;

    public Optional<Ressurs> findByFid(UUID fid){
        return Optional.ofNullable(
                convert(hendelseRepository.findAllByFunksjonellId(fid))
        );
    }

    public Optional<Ressurs> findByNavIdent(String navIdent){
        return Optional.ofNullable(
                convert(hendelseRepository.findHendelseByJson(Ressurs.NAVIDENT_FIELD, navIdent))
        );
    }

    public Optional<Ressurs> findByPersonIdent(String personIdent){
        return Optional.ofNullable(
                convert(hendelseRepository.findHendelseByJson(Ressurs.PERSONIDENT_FIELD, personIdent))
        );
    }

    public Map<String, Ressurs> findByNavIdentBulk(List<String> navIdenter){
        return hendelseRepository.findHendelseByJsonBulk(Ressurs.NAVIDENT_FIELD, navIdenter).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(Ressurs::getNavIdent, Function.identity()));
    }

    public Map<String, Ressurs> findByPersonIdentBulk(List<String> personIdenter){
        return hendelseRepository.findHendelseByJsonBulk(Ressurs.PERSONIDENT_FIELD, personIdenter).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(Ressurs::getPersonIdent, Function.identity()));
    }

    public List<Ressurs> getAll(){
        return hendelseRepository.findAllByDataType(DataType.RESSURS_V1).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public void createEndreTilNyttPersonnummerHendelse(UUID fid, String personIdent) {
        var data = objectMapper.createObjectNode();
        data.put(Ressurs.PERSONIDENT_FIELD, personIdent);

        val hendelse = Hendelse.builder()
                .hendelseID(UUID.randomUUID())
                .funksjonellId(fid)
                .foresporselId(UUID.randomUUID())
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
                            data.put(Ressurs.PERSONIDENT_FIELD, h.getValue());

                            return Hendelse.builder()
                                    .hendelseID(UUID.randomUUID())
                                    .funksjonellId(h.getKey())
                                    .foresporselId(UUID.randomUUID())
                                    .tidligereId(null)
                                    .dataType(DataType.RESSURS_V1)
                                    .hendelseType(Hendelse.HendelseType.ENDRET)
                                    .data(data)
                                    .opprettetTidspunkt(LocalDateTime.now())
                                    .build();
                        }).toList()
        );
    }

    public void createOpprettetHendelse(String navIdent, String personIdent) {
        var data = objectMapper.createObjectNode();
        data.put(Ressurs.NAVIDENT_FIELD, navIdent);
        data.put(Ressurs.PERSONIDENT_FIELD, personIdent);

        val hendelse = Hendelse.builder()
                .hendelseID(UUID.randomUUID())
                .funksjonellId(UUID.randomUUID())
                .foresporselId(UUID.randomUUID())
                .tidligereId(null)
                .dataType(DataType.RESSURS_V1)
                .hendelseType(Hendelse.HendelseType.OPPRETTET)
                .data(data)
                .opprettetTidspunkt(LocalDateTime.now())
                .build();

        hendelseRepository.save(hendelse);
    }

    public void createOpprettetHendelser(Map<String, String> personIdentByNavident) {
        val listToSave = personIdentByNavident.entrySet().stream()
                .map(h -> {
                    val data = objectMapper.createObjectNode();
                    data.put(Ressurs.NAVIDENT_FIELD, h.getKey());
                    data.put(Ressurs.PERSONIDENT_FIELD, h.getValue());
                    return Hendelse.builder()
                            .hendelseID(UUID.randomUUID())
                            .funksjonellId(UUID.randomUUID())
                            .foresporselId(UUID.randomUUID())
                            .tidligereId(null)
                            .dataType(DataType.RESSURS_V1)
                            .hendelseType(Hendelse.HendelseType.OPPRETTET)
                            .data(data)
                            .opprettetTidspunkt(LocalDateTime.now())
                            .build();
                }).toList();

        hendelseRepository.saveAll(listToSave);
    }

    private Ressurs convert(List<Hendelse> hendelser){
        return hendelser == null || hendelser.isEmpty() ? null : new Ressurs(hendelser);
    }
}
