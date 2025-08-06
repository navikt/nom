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

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static no.nav.nom.apps.api.common.DateUtils.isPeriodActiveOrFuture;
import static no.nav.nom.apps.api.graphql.common.SecurityUtil.getAuthenticatedNavIdent;

@Component
public class KonverterEksternSamarbeidspartnerTilknytningtype {
    private final List<String> EXCLUDE_2310 = Arrays.asList("L105654", "Y130043	", "L133149	", "H157490	", "B100739	", "S127124	", "S155717	", "N153314	", "S120955	", "S108637	", "S122231	", "S108216	", "A100148	", "A100212	", "P155451	", "T109635	", "L105471	", "S107967	", "S109003	", "K172311");
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


    public String konverterTilknytningtyperTilEksterneOrgEnheter() {
        StringBuffer result = new StringBuffer();
        result.append(konverterAlleTilknytningtyperForOrgEnhet(helfo, Tilknytningtype.HELFO));
        result.append(konverterAlleTilknytningtyperForOrgEnhet(skatt, Tilknytningtype.SKATTEETATEN));
        result.append(konverterAlleTilknytningtyperForOrgEnhet(riksrev, Tilknytningtype.RIKSREVISJONEN));
        return result.toString();
    }

    private String konverterAlleTilknytningtyperForOrgEnhet(String orgEnhetId, Tilknytningtype tilknytningtype) {
        var orgtilknytninger = orgtilknytningStateRepository.getOrgtilknytningerPaaEnhet(orgEnhetId);
        StringBuffer result = new StringBuffer();
        orgtilknytninger
            .forEach(orgtilknytningState -> {
                if (EXCLUDE_2310.contains(orgtilknytningState.navident())) {
                    result.append("Ekskludert: ").append(orgtilknytningState.navident()).append("\n");
                } else if (isPeriodActiveOrFuture(orgtilknytningState.gyldigFom(), orgtilknytningState.gyldigTom().orElse(null))) {
                    if (orgtilknytningState.orgtilknytningType().isEmpty()  || orgtilknytningState.orgtilknytningType().equals(Optional.of(Tilknytningtype.EKSTERN_SAMARBEIDSPARTNER))) {
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
                        result.append(orgtilknytningState.fid()).append(" - ").append(orgtilknytningState.navident()).append(" - ").append(orgEnhetId).append(" - ").append(tilknytningtype).append("\n");
                    }
                }
        });
        return result.toString();
    }
}
