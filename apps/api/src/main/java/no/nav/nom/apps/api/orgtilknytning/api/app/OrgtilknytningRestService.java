package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class OrgtilknytningRestService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;

    public OrgtilknytningRestService(
        OrgtilknytningStateRepository orgtilknytningStateRepository, OrgtilknytningCommandService orgtilknytningCommandService,
        RessursStateRepository ressursStateRepository,
        NomMastered nomMastered
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.ressursStateRepository = ressursStateRepository;
        this.nomMastered = nomMastered;
    }

    public List<OrgtilknytningState> getOrgtilknytninger(Optional<String> navident) {
        return navident.map(
                presentNavident -> orgtilknytningStateRepository.findByNavident(presentNavident).stream().toList()
            )
            .orElseGet(
                () -> orgtilknytningStateRepository.getAll().stream().toList()
            );
    }

    public void updateOrgtilknyting(String fid, EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest) {
        endreOrgtilknytningDatoRequest.gyldigTom()
            .ifPresent(sluttDato -> {
                if (sluttDato.isBefore(endreOrgtilknytningDatoRequest.gyldigFom())) {
                    throw new IllegalArgumentException("Sluttdato kan ikke være før startdato");
                }
            });

        var orgtilknytning = orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));

        ressursStateRepository.findByNavident(orgtilknytning.navident())
            .ifPresentOrElse(
                tilknyttetRessurs -> {
                    if (!tilknyttetRessurs.sektor().equals(Sektor.EKSTERN)) {
                        throw InvalidSektorException.actionNotAllowedForSektor(tilknyttetRessurs.sektor());
                    }
                },
                () -> {
                    throw RessursNotFoundException.navident(orgtilknytning.navident());
                }
            );

        orgtilknytningCommandService.endreOrgtilknytningDato(
            fid,
            new EndreOrgtilknytningDato(
                orgtilknytning.navident(),
                orgtilknytning.orgEnhetId(),
                endreOrgtilknytningDatoRequest.gyldigFom(),
                endreOrgtilknytningDatoRequest.gyldigTom(),
                new InputMetadata(
                    "REST",
                    "updateOrgtilknyting",
                    Map.of(
                        "navident", orgtilknytning.navident(),
                        "orgEnhetId", orgtilknytning.orgEnhetId()
                    ),
                    new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())
                ),
                null
            )
        );

        nomMastered.markRessursAsMasteredInNom(orgtilknytning.navident());
    }
}
