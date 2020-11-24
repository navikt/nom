package no.nav.nom.contract.nom.hendelse;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * #########################################
 * #####            DRAFT!!!            ####
 * #########################################
 */
// tag::class[]
@Value
@Builder
public class Hendelse {

    // Unik id per hendelse. Opprettes av backend.
    @NonNull
    UUID id;

    // Unik id for en forretningshendelse, f.eks nedleggelse av enhet kan generere flere hendelser. Opprettes av client.
    @NonNull
    UUID reqId;

    // Hvilke type data som ligger i feltet data.
    // Eksempel: no.nav.nom.hendelse:Enhet
    @NonNull
    String type;

    // Data i JSON format.
    // Eksempel: { "fid": "UUID....", "navn": "NAV Gamle Oslo"... }
    JsonNode data;

    // Aksjonen som ble utført.
    // Eksempel: OPPRETT
    @NonNull
    Endringstype endringstype;

    // Subject fra JWT token.
    // Eksempel: test.testesen@nav.no
    @NonNull
    String opprettetAv;

    // Tidspunkt hendelsen ble opprettet.
    // Eksempel: 2020-11-24T13:37:00Z
    @NonNull
    LocalDateTime opprettet;

    // Peker til en tidligere hendelsesid i tilfeller ved korrigering og annullering.
    UUID tidligereId;

    public enum Endringstype {
        OPPRETET, ENDRET, KORRIGERT, OPPHOERT, ANNULLERT
    }
}
// end::class[]
