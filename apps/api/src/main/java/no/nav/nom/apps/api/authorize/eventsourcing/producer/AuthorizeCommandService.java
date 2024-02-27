package no.nav.nom.apps.api.authorize.eventsourcing.producer;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.AvsluttDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.FornyDelegertAuthorization;
import no.nav.nom.apps.api.authorize.eventsourcing.producer.commands.OpprettDelegertAuthorization;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.contract.nom.eventsource.AuthorizationEvent;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationAvsluttetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationFornyetEventV1;
import no.nav.nom.contract.nom.eventsource.DelegertAuthorizationOpprettetEventV1;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class AuthorizeCommandService {
    private final AuthorizeEventProducer eventProducer;

    public AuthorizeCommandService(AuthorizeEventProducer eventProducer) {
        this.eventProducer = eventProducer;
    }

    public String opprettDelegertAuthorization(OpprettDelegertAuthorization opprettDelegertAuthorization) {
        String fid = PrefixedUlidCreator.createAuthorizationUlid();
        opprettAuthorization(fid, opprettDelegertAuthorization);
        return fid;
    }

    public String fornyDelegertAuthorization(String fid, FornyDelegertAuthorization fornyDelegertAuthorization) {
        fornyAuthorization(fid, fornyDelegertAuthorization);
        return fid;
    }

    public String avsluttDelegertAuthorization(String fid, AvsluttDelegertAuthorization avsluttDelegertAuthorization) {
        avsluttAuthorization(fid, avsluttDelegertAuthorization);
        return fid;
    }

    private CompletableFuture<SendResult<String, AuthorizationEvent>> opprettAuthorization(
        String fid,
        OpprettDelegertAuthorization opprettDelegertAuthorization
    ) {
        return eventProducer.sendMessage(new DelegertAuthorizationOpprettetEventV1(
            fid,
            new DelegertAuthorizationOpprettetEventV1.DelegertAuthorizationOpprettetEventData(
                opprettDelegertAuthorization.til(),
                opprettDelegertAuthorization.av(),
                opprettDelegertAuthorization.forOrgenhet(),
                opprettDelegertAuthorization.gyldigFom(),
                opprettDelegertAuthorization.gyldigTom()
            ),
            opprettDelegertAuthorization.inputMetadata()
        ));
    }

    private CompletableFuture<SendResult<String, AuthorizationEvent>> fornyAuthorization(String fid, FornyDelegertAuthorization fornyDelegertAuthorization) {
        return eventProducer.sendMessage(new DelegertAuthorizationFornyetEventV1(
            fid,
            new DelegertAuthorizationFornyetEventV1.DelegertAuthorizationFornyetEventData(
                fornyDelegertAuthorization.til(),
                fornyDelegertAuthorization.av(),
                fornyDelegertAuthorization.forOrgenhet(),
                fornyDelegertAuthorization.gyldigFom(),
                fornyDelegertAuthorization.opprinneligGyldigTom(),
                fornyDelegertAuthorization.nyGyldigTom()
            ),
            fornyDelegertAuthorization.inputMetadata()
        ));
    }

    private CompletableFuture<SendResult<String, AuthorizationEvent>> avsluttAuthorization(String fid, AvsluttDelegertAuthorization avsluttDelegertAuthorization) {
        return eventProducer.sendMessage(new DelegertAuthorizationAvsluttetEventV1(
            fid,
            new DelegertAuthorizationAvsluttetEventV1.DelegertAuthorizationAvsluttetEventData(
                avsluttDelegertAuthorization.til(),
                avsluttDelegertAuthorization.av(),
                avsluttDelegertAuthorization.forOrgenhet(),
                avsluttDelegertAuthorization.gyldigFom(),
                avsluttDelegertAuthorization.opprinneligGyldigTom(),
                avsluttDelegertAuthorization.nyGyldigTom()
            ),
            avsluttDelegertAuthorization.inputMetadata()
        ));
    }
}

