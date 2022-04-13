package no.nav.nom.apps.api.hendelse.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseType;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.hendelse.contract.IHendelseService;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HendelseService implements IHendelseService {

    private final ObjectMapper objectMapper;

    @Override
    public IHendelse convert(GenericStorageElement genericStorageElement) {
        return objectMapper.convertValue(genericStorageElement.getDocument(), HendelseV1.class);
    }

    @Override
    public IHendelse createOpprettHendelse(JsonNode data, HendelseMetadata hendelseMetadata, HendelseDataType hendelseDataType) {
         return HendelseV1.builder()
                .id(UUID.randomUUID())
                .fid(UUID.randomUUID())
                .tidligereId(null)
                .hendelseType(HendelseType.OPPRETTET)
                .hendelseDataType(hendelseDataType)
                .hendelseData(data)
                 .hendelseMetadata(hendelseMetadata)
                .build();
    }

    @Override
    public IHendelse createEndreHendelse(UUID fid, JsonNode data, HendelseMetadata hendelseMetadata, HendelseDataType hendelseDataType) {
        return  HendelseV1.builder()
                .id(UUID.randomUUID())
                .fid(fid)
                .tidligereId(null)
                .hendelseType(HendelseType.ENDRET)
                .hendelseDataType(hendelseDataType)
                .hendelseData(data)
                .hendelseMetadata(hendelseMetadata)
                .build();
    }

    @Override
    public GenericStorageElement convert(IHendelse hendelse){
        return GenericStorageElement.builder()
                .id(hendelse.getId())
                .dataType(GenericStorageElement.DataType.HENDELSE_V1)
                .document(objectMapper.convertValue(hendelse, JsonNode.class))
                .storedDate(LocalDateTime.now())
                .build();
    }
}
