package no.nav.nom.apps.api.rest.dolly;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.AppProperties;
import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.ressurs.api.app.AvvikleRessursRequest;
import no.nav.nom.apps.api.ressurs.api.app.OpprettRessursRequest;
import no.nav.nom.apps.api.ressurs.api.app.RessursRestService;
import no.nav.nom.apps.api.ressurs.api.exceptions.DuplicateRessursException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.rest.dto.NavnDTO;
import no.nav.nom.contract.nom.AnsettelsesType;
import no.nav.nom.contract.nom.IdentType;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.RessursState;
import no.nav.nom.libs.state.ressurs.RessursStateRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Optional;

@Slf4j
@Service
public class DollyService {

    private final RessursStateRepository ressursStateRepository;
    private final RessursRestService ressursRestService;
    private final DollyAsyncOpprettRessurs dollyAsyncOpprettRessurs;
    private final AppProperties appProperties;

    public DollyService(RessursStateRepository ressursStateRepository,
                        RessursRestService ressursRestService, DollyAsyncOpprettRessurs dollyAsyncOpprettRessurs, AppProperties appProperties
    ) {
        this.ressursStateRepository = ressursStateRepository;
        this.ressursRestService = ressursRestService;
        this.dollyAsyncOpprettRessurs = dollyAsyncOpprettRessurs;
        this.appProperties = appProperties;
    }

    public RessursDTO getRessurs(String personident) {
        var ressurs = ressursStateRepository.findByPersonident(personident);
        return ressurs.map(this::from)
                .orElseThrow(() -> new RessursNotFoundException("Ressurs med angitt personident finnes ikke"));
    }

    public void opprettRessurs(DollyRessursRequest request) {
        var opprettReq = OpprettRessursRequest.builder()
            .personident(request.personident())
            .identType(IdentType.STANDARD)
            .visningsfornavn(request.fornavn())
            .visningsmellomnavn(request.mellomnavn())
            .visningsetternavn(request.etternavn())
            .ansettelsesType(AnsettelsesType.FAST)
            .startDato(request.startDato() != null ? request.startDato() : java.time.LocalDate.now())
            .sluttDato(request.sluttDato())
            .sektor(Sektor.NAV_STATLIG)
            .orgEnhetId(appProperties.dollyRessurs().orgenhetId())
            .fysiskBrukerstedNr(appProperties.dollyRessurs().fysiskBrukerstedNr())
            .tilknytningtype(Tilknytningtype.FAST)
            .build();

        var metaData = new InputMetadata(
            "DOLLY-REST-API",
            "opprettRessurs",
            request,
            new Origination(
                "Dolly",
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        final var existingRessursByNavident = ressursStateRepository.findByPersonident(request.personident());

        if (existingRessursByNavident.isPresent() && ressursRestService.isActive(existingRessursByNavident.get())) {
            throw new DuplicateRessursException(
                String.format("Det finnes allerede en aktiv ressurs med personident %s", request.personident())
            );
        }

        ressursRestService.validateOpprettRessursRequest(opprettReq);
        dollyAsyncOpprettRessurs.opprettRessurs(opprettReq, metaData);
    }

    public void avvikleRessurs(DollyRessursRequest request) {
        RessursState ressurs = ressursStateRepository.findByPersonident(request.personident())
            .orElseThrow(() -> new RessursNotFoundException("Fant ikke ressurs for angitt personident"));

        var avvikleReq = new AvvikleRessursRequest(request.sluttDato() == null ? java.time.LocalDate.now() : request.sluttDato());
        var metadata = new InputMetadata(
            "DOLLY-REST-API",
            "avvikleRessurs",
            request,
            new Origination(
                "Dolly",
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        ressursRestService.avvikleRessurs(ressurs.fid(), avvikleReq, metadata);
    }

    RessursDTO from(RessursState ressursState) {
        return RessursDTO.builder()
            .fid(ressursState.fid())
            .personident(ressursState.personident())
            .navident(ressursState.navident())
            .navn(NavnDTO.builder()
                .fornavn(ressursState.visningsnavn().fornavn())
                .mellomnavn(ressursState.visningsnavn().mellomnavn().orElse(null))
                .etternavn(ressursState.visningsnavn().etternavn())
                .build())
            .startDato(ressursState.startdato())
            .sluttDato(ressursState.sluttdato())
            .sektor(ressursState.sektor())
            .build();
    }
}
