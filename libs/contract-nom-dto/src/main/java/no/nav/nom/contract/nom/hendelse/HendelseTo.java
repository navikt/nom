package no.nav.nom.contract.nom.hendelse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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
public class HendelseTo {

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
    ObjectNode data;

    // Aksjonen som ble utført.
    // Eksempel: OPPRETT
    @NonNull
    Endringstype endringstype;

    // Tidspunkt hendelsen ble opprettet.
    // Eksempel: 2020-11-24T13:37:00Z
    @NonNull
    LocalDateTime opprettet;

    // Peker til en tidligere hendelsesid i tilfeller ved annullering.
    UUID tidligereId;

    public enum Endringstype {
        OPPRETTET, ENDRET, ANNULLERT
    }
}
// end::class[]
