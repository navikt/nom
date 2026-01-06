package no.nav.nom.contract.nom.api.graphql;

import java.time.LocalDateTime;

public class RessursWithMetadataResult {

    public final String id;
    public final Ressurs ressurs;
    public final  String opprettetAv;
    public final LocalDateTime opprettetDato;
    public final String endretAv;
    public final LocalDateTime endretDato;
    public final String endringEvent;
    public final ResultCode code;


    public RessursWithMetadataResult(String id, Ressurs ressurs, String opprettetAv, LocalDateTime opprettetDato, String endretAv, LocalDateTime endretDato, String endringEvent) {
        this.id = id;
        this.ressurs = ressurs;
        this.opprettetAv = opprettetAv;
        this.opprettetDato = opprettetDato;
        this.endretAv = endretAv;
        this.endretDato = endretDato;
        this.endringEvent = endringEvent;
        this.code = ressurs == null ? ResultCode.NOT_FOUND : ResultCode.OK;
    }
}
