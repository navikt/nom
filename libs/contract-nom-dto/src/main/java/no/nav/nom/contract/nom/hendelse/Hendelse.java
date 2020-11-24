package no.nav.nom.contract.nom.hendelse;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;

import java.time.LocalDateTime;

/**
 * #########################################
 * #####            DRAFT!!!            ####
 * #########################################
 */
// tag::class[]
@Value
public class Hendelse {
    String id;                      // b960900e-f58c-4d65-ad72-0eacceb5722c
    String reqId;                   // 2fabfeb1-6f6b-4a93-87a6-982fdfcc8074
    String type;                    // "no.nav.nom.hendelse:Enhet"
    JsonNode data;                  // { "fid": "UUID....", "navn": "NAV Gamle Oslo"... }
    Endringstype endringstype;      // OPPRETTET
    String opprettetAv;             // test.testesen@nav.no
    LocalDateTime opprettet;        // 2020-11-24T13:37:00Z
    String tidligereId;             // null

    public enum Endringstype {
        OPPRETET, ENDRET, KORRIGERT, OPPHOERT, ANNULLERT
    }
}
// end::class[]
