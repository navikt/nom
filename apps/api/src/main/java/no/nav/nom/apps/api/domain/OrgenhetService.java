package no.nav.nom.apps.api.domain;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.val;
import no.nav.nom.apps.api.repository.hendelse.Hendelse;
import no.nav.nom.apps.api.repository.hendelse.Hendelse.DataType;
import no.nav.nom.apps.api.repository.hendelse.HendelseRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static no.nav.nom.apps.api.domain.Orgenhet.AGRESSO_ID_FIELD;
import static no.nav.nom.apps.api.domain.Orgenhet.NAVN_FIELD;

@Service
@RequiredArgsConstructor
public class OrgenhetService {

    private final HendelseRepository hendelseRepository;
    private final ObjectMapper objectMapper;

    public Optional<Orgenhet> findById(UUID uuid) {
        return Optional.ofNullable(
                convert(hendelseRepository.findAllByFunksjonellId(uuid))
        );
    }

    public Optional<Orgenhet> findByAgressoId(String agressoId) {
        return Optional.ofNullable(
                convert(hendelseRepository.findHendelseByJson(AGRESSO_ID_FIELD, agressoId))
        );
    }

    public List<Orgenhet> matchBy(String field, String fieldValue) {
        return hendelseRepository
                .findHendelserByJsonMatch(field, fieldValue.toLowerCase())
                .stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public Map<String, Orgenhet> findByAgressoIdBulk(List<String> agressoIder) {
        return hendelseRepository.findHendelseByJsonBulk(AGRESSO_ID_FIELD, agressoIder).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values().stream()
                .map(this::convert)
                .collect(Collectors.toUnmodifiableMap(Orgenhet::getAgressoId, Function.identity()));
    }

    public List<Orgenhet> getAll(){
        return hendelseRepository.findAllByDataType(DataType.ORGENHET_V1).stream()
                .collect(Collectors.groupingBy(Hendelse::getFunksjonellId))
                .values()
                .stream()
                .map(this::convert)
                .collect(Collectors.toList());
    }

    public void createEndreHendelse(UUID fid, Map<String, String> endringer) {
        var data = objectMapper.createObjectNode();
        endringer.forEach(data::put);


        val hendelse = Hendelse.builder()
                .hendelseID(UUID.randomUUID())
                .funksjonellId(fid)
                .ordreId(UUID.randomUUID())
                .tidligereId(null)
                .dataType(DataType.ORGENHET_V1)
                .hendelseType(Hendelse.HendelseType.ENDRET)
                .data(data)
                .opprettetTidspunkt(LocalDateTime.now())
                .build();

        hendelseRepository.save(hendelse);
    }

    public void createEndreHendelser(Map<UUID, Map<String, String>> endringerByFid) {
        hendelseRepository.saveAll(
                endringerByFid.entrySet().stream()
                .map(h -> {
                    var data = objectMapper.createObjectNode();
                    h.getValue().forEach(data::put);

                    return Hendelse.builder()
                            .hendelseID(UUID.randomUUID())
                            .funksjonellId(h.getKey())
                            .ordreId(UUID.randomUUID())
                            .tidligereId(null)
                            .dataType(DataType.ORGENHET_V1)
                            .hendelseType(Hendelse.HendelseType.ENDRET)
                            .data(data)
                            .opprettetTidspunkt(LocalDateTime.now())
                            .build();

                }).toList()
        );
    }

    public void createOpprettHendelse(String agressoId, String navn) {
        var data = objectMapper.createObjectNode();
        data.put(AGRESSO_ID_FIELD, agressoId);
        data.put(NAVN_FIELD, navn);

        val hendelse = Hendelse.builder()
                .hendelseID(UUID.randomUUID())
                .funksjonellId(UUID.randomUUID())
                .ordreId(UUID.randomUUID())
                .tidligereId(null)
                .dataType(DataType.ORGENHET_V1)
                .hendelseType(Hendelse.HendelseType.OPPRETTET)
                .data(data)
                .opprettetTidspunkt(LocalDateTime.now())
                .build();

        hendelseRepository.save(hendelse);
    }

    public void createOpprettHendelser(Map<String, String> navnByAgressoId) {
        hendelseRepository.saveAll(
                navnByAgressoId.entrySet().stream()
                .map(h -> {
                    var data = objectMapper.createObjectNode();
                    data.put(AGRESSO_ID_FIELD, h.getKey());
                    data.put(NAVN_FIELD, h.getValue());

                    return Hendelse.builder()
                            .hendelseID(UUID.randomUUID())
                            .funksjonellId(UUID.randomUUID())
                            .ordreId(UUID.randomUUID())
                            .tidligereId(null)
                            .dataType(DataType.ORGENHET_V1)
                            .hendelseType(Hendelse.HendelseType.OPPRETTET)
                            .data(data)
                            .opprettetTidspunkt(LocalDateTime.now())
                            .build();
                }).toList()
        );
    }

    private Orgenhet convert(List<Hendelse> hendelser){
        return hendelser == null || hendelser.isEmpty() ? null : new Orgenhet(hendelser);
    }
}