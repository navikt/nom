package no.nav.nom.contract.nom.api.graphql;

import java.time.LocalDateTime;

public class RessursWithMedatadaResult  {

    private String id;
    private Ressurs ressurs;
    private  String opprettetAv;
    private LocalDateTime opprettetDato;
    private ResultCode code;

    public RessursWithMedatadaResult(String id, Ressurs ressurs, String opprettetAv, LocalDateTime opprettetDato) {
        this.id = id;
        this.ressurs = ressurs;
        this.opprettetAv = opprettetAv;
        this.opprettetDato = opprettetDato;
        this.code = ressurs == null ? ResultCode.NOT_FOUND : ResultCode.OK;
    }
}
