package no.nav.nom.apps.streams.kafkaapi.hendelser;

import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.Event;
import no.nav.nom.contract.nom.eventsource.OrgEnhetAvvikletEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetEvent;
import no.nav.nom.contract.nom.eventsource.OrgEnhetKorrigertEventV1;
import no.nav.nom.contract.nom.eventsource.OrgEnhetOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrganiseringEvent;
import no.nav.nom.contract.nom.eventsource.OrganiseringOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDagligOppfolgingEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningDatoEndretEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningEvent;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningFjernetEventV1;
import no.nav.nom.contract.nom.eventsource.OrgtilknytningOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursEvent;
import no.nav.nom.contract.nom.eventsource.RessursOpprettetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonaliaOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursPersonidentOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSektorOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursSluttetEventV1;
import no.nav.nom.contract.nom.eventsource.RessursStartDatoOppdatertV1;
import no.nav.nom.contract.nom.eventsource.RessursTekniskSlettetEventV1;
import no.nav.nom.contract.nom.eventsource.Visningsnavn;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class HendelserTestUtils {
    static OrgEnhetEvent getOrgEnhetOpprettetEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrgEnhetOpprettetEventV1(
            fid,
            new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetEventData(
                id,
                "TestName",
                LocalDate.of(2022, 12, 1),
                "orgEnhet_123",
                "ressurs_123",
                "1234"
            ),
            eventMetadata,
            new OrgEnhetOpprettetEventV1.OrgEnhetOpprettetAdditionalAgressoData(
                "AgressoId_123",
                "AgressoNavn",
                "AgressoOrgNivaa_1",
                "AgressoOrgEnhetType",
                "AgressoId_123-AgressoNavn-AgressoOrgNivaa_1"
            )
        );
    }

    static OrgEnhetEvent getOrgEnhetKorrigertEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrgEnhetKorrigertEventV1(
            fid,
            new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertEventData(
                id,
                "TestName",
                LocalDate.of(2022, 12, 1),
                LocalDate.now().plusDays(10),
                "orgEnhet_123",
                "ressurs_123"
            ),
            eventMetadata,
            new OrgEnhetKorrigertEventV1.OrgEnhetKorrigertAdditionalAgressoData(
                "AgressoId_123",
                "AgressoNavn",
                "AgressoOrgNivaa_1",
                "AgressoOrgEnhetType",
                "AgressoId_123-AgressoNavn-AgressoOrgNivaa_1"
            )
        );
    }

    static OrgEnhetEvent getOrgEnhetAvvikletEvent(String orgEnhetFid, String orgEnhetId, Event.Metadata eventMetadata) {
        return new OrgEnhetAvvikletEventV1(
            orgEnhetFid,
            new OrgEnhetAvvikletEventV1.OrgEnhetAvvikletEventData(
                orgEnhetId,
                LocalDate.now().minusDays(2)
            ),
            eventMetadata
        );
    }

    static OrganiseringEvent getOpprettetOrganiseringEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrganiseringOpprettetEventV1(
            fid,
            new OrganiseringOpprettetEventV1.OrganiseringOpprettetEventData(
                id,
                "navn",
                "idOver",
                "navnOver",
                LocalDate.of(2021, 1, 1),
                null
            ),
            eventMetadata
        );
    }

    static OrganiseringEvent getEndretOrganiseringEvent(String fid, String id, Event.Metadata eventMetadata) {
        return new OrganiseringEndretEventV1(
            fid,
            new OrganiseringEndretEventV1.OrganiseringEndretEventData(
                id,
                "navn",
                "idOver",
                "navnOver",
                LocalDate.of(2021, 1, 1),
                null
            ),
            eventMetadata
        );
    }

    static OrganiseringEvent getAvsluttetOrganiseringEvent(
        String fid,
        String orgEnhetId,
        Event.Metadata eventMetadata
    ) {
        return new OrganiseringAvsluttetEventV1(
            fid,
            new OrganiseringAvsluttetEventV1.OrganiseringAvsluttetEventData(
                orgEnhetId,
                LocalDate.now().minusDays(2)
            ),
            eventMetadata
        );
    }

    static RessursEvent getRessursOpprettetEvent(String fid, String navident, Event.Metadata eventMetadata) {
        final RessursOpprettetEventV1.RessursOpprettetEventData eventData = new RessursOpprettetEventV1.RessursOpprettetEventData(
            "010101123456",
            Sektor.NAV_STATLIG,
            new Visningsnavn(
                "fornavn",
                Optional.empty(),
                "etternavn"
            ),
            null,
            List.of("fysiskBrukersted1", "fysiskBrukersted2"),
            "fysiskBrukersted1",
            IdentType.STANDARD,
            null
        );

        final RessursOpprettetEventV1.RessursOpprettetAdditionalData additionalData = new RessursOpprettetEventV1.RessursOpprettetAdditionalData(
            navident,
            null,
            null,
            null,
            LocalDate.now(),
            null,
            null
        );

        return new RessursOpprettetEventV1(
            fid,
            eventData,
            eventMetadata,
            additionalData
        );
    }

    static RessursEvent getRessursPersonaliaOppdatert(String fid, Event.Metadata eventMetadata) {
        final RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData eventData = new RessursPersonaliaOppdatertV1.PersonaliaOppdatertEventData(
            new Visningsnavn(
                "fornavn",
                Optional.empty(),
                "etternavn"
            )
        );

        return new RessursPersonaliaOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursPersonidentOppdatert(String fid, Event.Metadata eventMetadata) {
        final RessursPersonidentOppdatertV1.PersonidentOppdatertEventData eventData = new RessursPersonidentOppdatertV1.PersonidentOppdatertEventData(
            "010101123456"
        );

        return new RessursPersonidentOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursSektorOppdatert(String fid, Event.Metadata eventMetadata) {
        final RessursSektorOppdatertV1.SektorOppdatertEventData eventData = new RessursSektorOppdatertV1.SektorOppdatertEventData(
            Sektor.NAV_STATLIG
        );

        return new RessursSektorOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursStartDatoOppdatert(String fid, Event.Metadata eventMetadata) {
        final RessursStartDatoOppdatertV1.StartDatoOppdatertEventData eventData = new RessursStartDatoOppdatertV1.StartDatoOppdatertEventData(
            LocalDate.now().minusDays(50)
        );

        return new RessursStartDatoOppdatertV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursSluttetEvent(String fid, Event.Metadata eventMetadata) {
        final RessursSluttetEventV1.RessursSluttetEventData eventData = new RessursSluttetEventV1.RessursSluttetEventData(
            LocalDate.now().minusDays(50)
        );

        return new RessursSluttetEventV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static RessursEvent getRessursTekniskSlettetEvent(String fid, Event.Metadata eventMetadata) {
        final RessursTekniskSlettetEventV1.RessursTekniskSlettetEventData eventData = new RessursTekniskSlettetEventV1.RessursTekniskSlettetEventData(
        );

        return new RessursTekniskSlettetEventV1(
            fid,
            eventData,
            eventMetadata
        );
    }

    static OrgtilknytningEvent getOrgtilknytningOpprettetEvent(String fid, String navident, Event.Metadata eventMetadata, String masseflyttingFid) {
        final OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData eventData = new OrgtilknytningOpprettetEventV1.OrgtilknytningOpprettetEventData(
            navident,
            "orgEnhetId",
            Sektor.NAV_KOMMUNAL,
            LocalDate.now(),
            masseflyttingFid
        );

        final OrgtilknytningEvent.AdditionalAgressoData additionalData = new OrgtilknytningEvent.AdditionalAgressoData(
            "hrresStillingRessursEnhet",
            "hrresStillingerPerioderFormatert"
        );

        return new OrgtilknytningOpprettetEventV1(
            fid,
            eventData,
            eventMetadata,
            additionalData
        );
    }

    static OrgtilknytningEvent getOrgtilknytningFjernetEvent(String fid, String navident, Event.Metadata eventMetadata) {
        return new OrgtilknytningFjernetEventV1(
            fid,
            new OrgtilknytningFjernetEventV1.OrgtilknytningFjernetEventData(
                navident,
                "orgEnhetId",
                LocalDate.now(),
                LocalDate.now()
            ),
            eventMetadata,
            new OrgtilknytningEvent.AdditionalAgressoData(
                "hrresStillingRessursEnhet",
                "hrresStillingerPerioderFormatert"
            )
        );
    }

    static OrgtilknytningEvent getOrgtilknytningAvsluttetEvent(String fid, String navident, Event.Metadata eventMetadata) {
        return new OrgtilknytningAvsluttetEventV1(
            fid,
            new OrgtilknytningAvsluttetEventV1.OrgtilknytningAvsluttetEventData(
                navident,
                "orgEnhetId",
                LocalDate.now()
            ),
            eventMetadata,
            new OrgtilknytningEvent.AdditionalAgressoData(
                "hrresStillingRessursEnhet",
                "hrresStillingerPerioderFormatert"
            )
        );
    }

    static OrgtilknytningEvent getOrgtilknytningDatoEndretEvent(String fid, String navident, Event.Metadata eventMetadata) {
        return new OrgtilknytningDatoEndretEventV1(
            fid,
            new OrgtilknytningDatoEndretEventV1.OrgtilknytningDatoEndretEventData(
                navident,
                "orgEnhetId",
                LocalDate.now(),
                Optional.of(LocalDate.now())
            ),
            eventMetadata,
            new OrgtilknytningEvent.AdditionalAgressoData(
                "hrresStillingRessursEnhet",
                "hrresStillingerPerioderFormatert"
            )
        );
    }

    static OrgtilknytningEvent getOrgtilknytningDagligOppfolgingEndretEvent(String fid, String navident, Event.Metadata eventMetadata) {
        return new OrgtilknytningDagligOppfolgingEndretEventV1(
            fid,
            new OrgtilknytningDagligOppfolgingEndretEventV1.OrgtilknytningDagligOppfolgingEndretEventData(
                navident,
                "orgEnhetId",
                LocalDate.now(),
                LocalDate.now(),
                true
            ),
            eventMetadata,
            new OrgtilknytningEvent.AdditionalAgressoData(
                "hrresStillingRessursEnhet",
                "hrresStillingerPerioderFormatert"
            )
        );
    }

}
