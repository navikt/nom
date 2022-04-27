package no.nav.nom.apps.api.hendelse.implementation;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import no.nav.nom.apps.api.hendelse.contract.HendelseDataType;
import no.nav.nom.apps.api.hendelse.contract.HendelseMetadata;
import no.nav.nom.apps.api.hendelse.contract.HendelseType;
import no.nav.nom.apps.api.hendelse.contract.IHendelse;
import no.nav.nom.apps.api.postgresql.GenericStorageElement;

import java.util.UUID;

@Value
@Builder
@RequiredArgsConstructor
public class HendelseV1 implements IHendelse {

    GenericStorageElement.DataType dataType = GenericStorageElement.DataType.HENDELSE_V1;

    UUID id;
    UUID fid;
    UUID tidligereId;
    HendelseType hendelseType;
    HendelseDataType hendelseDataType;
    JsonNode hendelseData;
    HendelseMetadata hendelseMetadata;


    @Override
    public UUID getId() {
        return id;
    }

    @Override
    public UUID getFid() {
        return fid;
    }

    public JsonNode getHendelseData() {
        return hendelseData;
    }

    @Override
    public UUID getTidligereId() {
        return tidligereId;
    }

    @Override
    public HendelseType getHendelseType() {
        return hendelseType;
    }

    @Override
    public HendelseMetadata getHendelseMetadata() {
        return hendelseMetadata;
    }

    @Override
    public HendelseDataType getHendelseDataType() {
        return hendelseDataType;
    }
}
