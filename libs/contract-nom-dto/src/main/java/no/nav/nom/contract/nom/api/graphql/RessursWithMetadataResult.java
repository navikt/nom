package no.nav.nom.contract.nom.api.graphql;

import java.time.LocalDateTime;

public class RessursWithMetadataResult {

    private String id;
    private Ressurs ressurs;
    private  String opprettetAv;
    private LocalDateTime opprettetDato;
    private String endretAv;
    private LocalDateTime endretDato;
    private String endringEvent;
    private ResultCode code;


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
