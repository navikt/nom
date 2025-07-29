package no.nav.nom.apps.api.orgtilknytning.job;

import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.OrgtilknytningCommandService;
import no.nav.nom.apps.api.orgtilknytning.eventsourcing.producer.commands.EndreTilknytningtypeCommand;
import no.nav.nom.apps.api.ressurs.NomMastered;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.eventsource.InputMetadata;
import no.nav.nom.contract.nom.eventsource.Origination;
import no.nav.nom.libs.state.orgtilknytning.OrgtilknytningStateRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static no.nav.nom.apps.api.common.DateUtils.isPeriodActiveOrFuture;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Component
public class KonverterEksternSamarbeidspartnerTilknytningtype {

    private final OrgtilknytningStateRepository orgtilknytningStateRepository;
    private final OrgtilknytningCommandService orgtilknytningCommandService;
    private final NomMastered nomMastered;
    private final String helfo;
    private final String skatt;
    private final String riksrev;

    public KonverterEksternSamarbeidspartnerTilknytningtype(
        OrgtilknytningStateRepository orgtilknytningStateRepository,
        OrgtilknytningCommandService orgtilknytningCommandService,
        NomMastered nomMastered,
        @Value("${app.orgenhet.helfo}") String helfo,
        @Value("${app.orgenhet.skatt}") String skatt,
        @Value("${app.orgenhet.riksrev}") String riksrev
    ) {
        this.orgtilknytningStateRepository = orgtilknytningStateRepository;
        this.orgtilknytningCommandService = orgtilknytningCommandService;
        this.helfo = helfo;
        this.skatt = skatt;
        this.riksrev = riksrev;
        this.nomMastered = nomMastered;
    }


    public void konverterTilknytningtyperTilEksterneOrgEnheter() {
        konverterAlleTilknytningtyperForOrgEnhet(helfo, Tilknytningtype.HELFO);
        konverterAlleTilknytningtyperForOrgEnhet(skatt, Tilknytningtype.SKATTEETATEN);
        konverterAlleTilknytningtyperForOrgEnhet(riksrev, Tilknytningtype.RIKSREVISJONEN);
    }

    private void konverterAlleTilknytningtyperForOrgEnhet(String orgEnhetId, Tilknytningtype tilknytningtype) {
        var orgtilknytninger = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet(orgEnhetId);
        orgtilknytninger.forEach(orgtilknytningState -> {
            if (isPeriodActiveOrFuture(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom().orElse(null))) {
                if (orgtilknytningState.orgtilknytningType().isEmpty()
                    || orgtilknytningState.orgtilknytningType().equals(Optional.of(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER))) {
                    orgtilknytningCommandService.endreTilknytningtype(EndreTilknytningtypeCommand.builder()
                        .fid(orgtilknytningState.fid())
                        .tilknytningtype(Optional.of(tilknytningtype))
                        .inputMetadata(InputMetadata.builder()
                            .inputInterfaceName("REST-API")
                            .inputInterfaceMethodName("konverter-tilknytning-typer")
                            .inputInterfaceMethodArguments(orgtilknytningState)
                            .origination(new Origination(getAuthenticatedNavIdent(), "Swagger"))
                            .build())
                        .build());
                    if (!nomMastered.isMastered(orgtilknytningState.navident())) nomMastered.markRessursAsMasteredInNom(orgtilknytningState.navident());
                }
            }
        });
    }
}
