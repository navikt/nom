package no.nav.nom.apps.api.hendelse.contract;

import com.fasterxml.jackson.databind.JsonNode;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;

import java.util.Map;
import java.util.UUID;

public interface IHendelseService {
    IHendelse convert(GenericStorageElement genericStorageElement);
    GenericStorageElement convert(IHendelse hendelse);
    IHendelse createOpprettHendelse(JsonNode data, HendelseMetadata hendelseMetadata, HendelseDataType hendelseDataType);
    IHendelse createOpprettHendelse(Map<String, Object> data, HendelseMetadata hendelseMetadata, HendelseDataType hendelseDataType);
    IHendelse createEndreHendelse(UUID fid, JsonNode data, HendelseMetadata hendelseMetadata, HendelseDataType hendelseDataType);
    IHendelse createEndreHendelse(UUID fid, Map<String, Object> data, HendelseMetadata hendelseMetadata, HendelseDataType hendelseDataType);
}
