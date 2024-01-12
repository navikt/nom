package no.nav.nom.apps.api.orgtilknytning.api.app;

import no.nav.nom.apps.api.graphql.common.SecurityUtil;
import no.nav.nom.apps.api.orgenhet.eventsourcing.state.OrgEnhetStateRepository;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.InvalidOrgtilknytningTypeAndSektorCombinationException;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningPeriodeOverlapException;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.AvvikleOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningDato;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreOrgtilknytningType;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.OpprettOrgtilknytning;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.state.OrgtilknytningStateRepository;
import no.nav.nom.apps.api.orgtilknytning.api.exceptions.OrgtilknytningNotFoundException;
import no.nav.nom.apps.api.ressurs.api.exceptions.InvalidSektorException;
import no.nav.nom.apps.api.ressurs.api.exceptions.RessursNotFoundException;
import no.nav.nom.apps.api.ressurs.eventsourcing.state.RessursStateRepository;
import no.nav.nom.apps.api.ressurs.masterdata.NomMastered;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static no.nav.nom.apps.api.common.DateUtils.isPeriodsOverlapping;
import static no.nav.nom.apps.api.common.DateUtils.validateChronologicalDates;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Service
public class OrgtilknytningRestService {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final OrgEnhetStateRepository orgEnhetStateRepository;
    private final RessursStateRepository ressursStateRepository;
    private final NomMastered nomMastered;

    public OrgtilknytningRestService(
        OrgtilknytningStateRepository orgtilknytningStateRepository, OrgtilknytningCommandService orgtilknytningCommandService,
        OrgEnhetStateRepository orgEnhetStateRepository, RessursStateRepository ressursStateRepository,
        NomMastered nomMastered
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.orgEnhetStateRepository = orgEnhetStateRepository;
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

    public OrgtilknytningState getOrgtilknytning(String fid) {
        return orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));
    }

    public void updateOrgtilknyting(String fid, EndreOrgtilknytningDatoRequest endreOrgtilknytningDatoRequest) {
        validateChronologicalDates(endreOrgtilknytningDatoRequest.gyldigFom(), endreOrgtilknytningDatoRequest.gyldigTom().orElse(null));

        var orgtilknytning = orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));

        ressursStateRepository.findByNavident(orgtilknytning.navident())
            .ifPresentOrElse(
                tilknyttetRessurs -> {
                    if (!tilknyttetRessurs.sektor().equals(Sektor.EKSTERN)) {
                        throw InvalidSektorException.actionNotAllowedForSektor(tilknyttetRessurs.sektor());
                    }

                    validateEksternOrgtilknytningPeriode(endreOrgtilknytningDatoRequest.gyldigTom());

                    // If gyldigTom is set to before today, verify there are other active tilknytninger
                    if (LocalDate.now().isAfter(endreOrgtilknytningDatoRequest.gyldigTom().orElse(LocalDate.MIN))) {
                        var activeOrgtilknytningExists = getOrgtilknytninger(Optional.ofNullable(tilknyttetRessurs.navident())).stream()
                            .anyMatch(orgtilknytningState -> {
                                if (orgtilknytningState.fid().equals(fid)) {
                                    return false;
                                }

                                return orgtilknytningState.gyldigTom().orElse(LocalDate.MAX).isAfter(LocalDate.now());
                            });

                        if (!activeOrgtilknytningExists) {
                            throw new IllegalArgumentException(
                                "Du kan ikke avslutte denne tilknytningen før du har lagt til en ny tilknytning  som gjelder fra denne tilknytningens sluttdato. Medarbeideren må til enhver tid være tilknyttet en enhet.");
                        }
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

    public void updateOrgtilknytningType(String fid, EndreTilknytningtypeRequest endreTilknytningtypeRequest) {

        var orgtilknytning = orgtilknytningStateRepository.findByFid(fid)
            .orElseThrow(() -> OrgtilknytningNotFoundException.fid(fid));

        var tilknyttetRessurs = ressursStateRepository.findByNavident(orgtilknytning.navident())
            .orElseThrow(() -> RessursNotFoundException.navident(orgtilknytning.navident()));

        if (tilknyttetRessurs.sektor() != Sektor.EKSTERN) {
            throw InvalidSektorException.actionNotAllowedForSektor(tilknyttetRessurs.sektor());
        }

        endreTilknytningtypeRequest.tilknytningtype().ifPresent(
            orgtilknytningType -> validateOrgtilknytningTypeSektorCombination(orgtilknytningType, tilknyttetRessurs.sektor())
        );

        orgtilknytningCommandService.endreOrgtilknytningType(
            orgtilknytning.fid(),
            new EndreOrgtilknytningType(
                endreTilknytningtypeRequest.tilknytningtype(),
                new InputMetadata(
                    "REST",
                    "updateOrgtilknytningType",
                    Map.of(
                        "navident", orgtilknytning.navident(),
                        "orgEnhetId", orgtilknytning.orgEnhetId()
                    ),
                    new Origination(SecurityUtil.getAuthenticatedNavIdent(), SecurityUtil.getAuthenticatedAzpName())
                )
            )
        );

        nomMastered.markRessursAsMasteredInNom(orgtilknytning.navident());
    }

    private void validateOrgtilknytningTypeSektorCombination(Tilknytningtype tilknytningtype, Sektor sektor) {

        Map<Sektor, List<Tilknytningtype>> allowedOrgtilknytningTypeForSektorMap = Map.of(
            Sektor.EKSTERN, List.of(Tilknytningtype.KONSULENT),
            Sektor.NAV_KOMMUNAL, Collections.emptyList(),
            Sektor.NAV_STATLIG, Collections.emptyList()
        );

        if (!allowedOrgtilknytningTypeForSektorMap.get(sektor).contains(tilknytningtype)) {
            throw InvalidOrgtilknytningTypeAndSektorCombinationException.forCombination(tilknytningtype, sektor);
        }

    }

    public void addOrgtilknyting(LeggTilOrgtilknytningRequest leggTilOrgtilknytningRequest) {
        validateLeggTilOrgilknytningRequest(leggTilOrgtilknytningRequest);
        var metaData = new InputMetadata(
            "REST",
            "addOrgtilknyting",
            leggTilOrgtilknytningRequest,
            new Origination(
                getAuthenticatedNavIdent(),
                SecurityUtil.getAuthenticatedAzpName()
            )
        );

        var orgTilknytningFid = orgtilknytningCommandService.opprettOrgtilknytning(
            new OpprettOrgtilknytning(
                leggTilOrgtilknytningRequest.navident(),
                leggTilOrgtilknytningRequest.orgEnhetId(),
                leggTilOrgtilknytningRequest.gyldigFom(),
                metaData,
                null
            )
        );

        leggTilOrgtilknytningRequest.gyldigTom().ifPresent(gyldigTom -> orgtilknytningCommandService.avvikleOrgtilknytning(
            orgTilknytningFid,
            new AvvikleOrgtilknytning(
                leggTilOrgtilknytningRequest.navident(),
                leggTilOrgtilknytningRequest.orgEnhetId(),
                gyldigTom,
                metaData,
                null
            )
        ));

        nomMastered.markRessursAsMasteredInNom(leggTilOrgtilknytningRequest.navident());

        //TODO: håndtere daglig oppfølging
    }

    private void validateLeggTilOrgilknytningRequest(LeggTilOrgtilknytningRequest leggTilOrgtilknytningRequest) {
        var ressursState = ressursStateRepository.findByNavident(leggTilOrgtilknytningRequest.navident()).orElseThrow(
            () -> new IllegalArgumentException("Finner ikke ressurs med navident %s".formatted(leggTilOrgtilknytningRequest.navident()))
        );

        if (ressursState.sektor() == Sektor.EKSTERN) {
            validateEksternOrgtilknytningPeriode(leggTilOrgtilknytningRequest.gyldigTom());
        }

        if (orgEnhetStateRepository.findById(leggTilOrgtilknytningRequest.orgEnhetId()).isEmpty()) {
            throw new IllegalArgumentException("Finner ikke orgenhet med id %s".formatted(leggTilOrgtilknytningRequest.orgEnhetId()));
        }

        validateChronologicalDates(leggTilOrgtilknytningRequest.gyldigFom(), leggTilOrgtilknytningRequest.gyldigTom().orElse(null));

        var orgtilknytninger = orgtilknytningStateRepository.findByNavident(leggTilOrgtilknytningRequest.navident())
            .stream()
            .filter(orgtilknytningState -> orgtilknytningState.orgEnhetId().equals(leggTilOrgtilknytningRequest.orgEnhetId()))
            .toList();

        orgtilknytninger.forEach(orgtilknytningState -> {
            if (isPeriodsOverlapping(
                orgtilknytningState.gyldigFom(),
                orgtilknytningState.gyldigTom(),
                leggTilOrgtilknytningRequest.gyldigFom(),
                leggTilOrgtilknytningRequest.gyldigTom()
            )) {
                throw new OrgtilknytningPeriodeOverlapException("Det finnes allerede en orgtilknytning innenfor forespurt periode");

            }
        });
    }

    private void validateEksternOrgtilknytningPeriode(Optional<LocalDate> gyldigTom) {
        var requestedGyldigTomIsMoreThan1YearFromNow = gyldigTom.orElse(LocalDate.MAX).isAfter(LocalDate.now().plusYears(1));
        if (requestedGyldigTomIsMoreThan1YearFromNow) {
            throw new IllegalArgumentException("Sluttdato kan være maks 1 år frem i tid");
        }
    }
}
