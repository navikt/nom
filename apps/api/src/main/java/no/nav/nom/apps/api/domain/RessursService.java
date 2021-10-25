package no.nav.nom.apps.api.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.repository.NavIdentService;
import no.nav.nom.apps.api.repository.hendelse.Hendelse;
import no.nav.nom.apps.api.repository.hendelse.HendelseRepository;
import no.nav.nom.apps.api.repository.hendelse.Hendelse.DataType;
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
public class RessursService {

    private final ObjectMapper objectMapper;
    private final HendelseRepository hendelseRepository;
    private final NavIdentService navIdentService;

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

    public List<Ressurs> matchByNavident(String navident) {
        return hendelseRepository
                .findHendelserByJsonMatch(Ressurs.NAVIDENT_FIELD, navident.toLowerCase())
                .stream().collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream().map(this::convert)
                .collect(Collectors.toList());
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
                            data.put(Ressurs.PERSONIDENT_FIELD, h.getValue());

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

    @Transactional
    public Ressurs createOpprettetHendelse(String personIdent, String navIdentBokstav) {
        return findByPersonIdent(personIdent).orElseGet(() -> {
            var data = objectMapper.createObjectNode();

            data.put(Ressurs.NAVIDENT_FIELD, navIdentService.generateNavIdent(navIdentBokstav));
            data.put(Ressurs.PERSONIDENT_FIELD, personIdent);

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

    public void createOpprettetHendelser(Map<String, String> personIdentByNavident) {
        val listToSave = personIdentByNavident.entrySet().stream()
                .map(h -> {
                    val data = objectMapper.createObjectNode();
                    data.put(Ressurs.NAVIDENT_FIELD, h.getKey());
                    data.put(Ressurs.PERSONIDENT_FIELD, h.getValue());
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

    private Ressurs convert(List<Hendelse> hendelser){
        return hendelser == null || hendelser.isEmpty() ? null : new Ressurs(hendelser);
    }
}
