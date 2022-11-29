package no.nav.nom.apps.api.hendelse.contract;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.UUID;

public interface IHendelse {
    UUID getId();

    UUID getFid();

    JsonNode getHendelseData();

    UUID getTidligereId();

    HendelseType getHendelseType();

    HendelseMetadata getHendelseMetadata();

    HendelseDataType getHendelseDataType();
}