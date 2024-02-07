package no.nav.nom.apps.api.orgtilknytning.dvh;

import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.common.PrefixedUlidCreator;
import no.nav.nom.apps.api.orgenhet.IOrgenhet;
import no.nav.nom.apps.api.orgenhet.OrgenhetService;
import no.nav.nom.apps.api.orgtilknytning.OrgtilknytningService;
import no.nav.nom.apps.api.ressurs.implementations.dvh.DvhRessurs;
import no.nav.nom.contract.nom.state.OrgtilknytningState;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class KommunaleRessurserProcessor {
    private final OrgtilknytningService orgtilknytningService;
    private final OrgenhetService orgenhetService;

    public KommunaleRessurserProcessor(
        OrgtilknytningService orgtilknytningService,
        OrgenhetService orgenhetService
    ) {
        this.orgtilknytningService = orgtilknytningService;
        this.orgenhetService = orgenhetService;
    }

    public Optional<OrgtilknytningState> process(DvhRessurs kommunalRessurs, List<StillingPeriode> stillingPerioder) {
        var eksisterendeOrgtilknytninger = orgtilknytningService.getByNavIdent(kommunalRessurs.getNavIdent());

        log.debug("Prosesserer kommunal ressurs {}\n{}", kommunalRessurs.getNavIdent(), detaljerToString(kommunalRessurs, stillingPerioder, eksisterendeOrgtilknytninger));

        var aktiveEksisterendeOrgtilknytninger = eksisterendeOrgtilknytninger.stream().filter(PeriodeUtil::isCurrentPeriod).toList();

        if (aktiveEksisterendeOrgtilknytninger.size() == 0) {
            String fid = PrefixedUlidCreator.createTmpOrgtilknytningUlid();
            log.debug("Kommunal Ressurs {} har ingen eksisterende aktive orgtilknytninger. Lager en temporær som er basert på OrgenhetPaaRessurs.", kommunalRessurs.getNavIdent());

            var orgenhetPaaRessurs = getOrgenhetPaaRessurs(kommunalRessurs);
            if (orgenhetPaaRessurs.isEmpty()) {
                log.error("Kommunal Ressurs {} - klarer ikke finne Nom enhet for Orgenhet På Ressurs. Kan ikke behandles.", kommunalRessurs.getNavIdent());
                return Optional.empty();
            }

            var maksSluttDatoPaaEksisterendeOrgtilknytninger = maksSluttDato(eksisterendeOrgtilknytninger);
            var startDatoPaaTmpOT = maksSluttDatoPaaEksisterendeOrgtilknytninger == null ?
                kommunalRessurs.getStartDato() :
                maksSluttDatoPaaEksisterendeOrgtilknytninger.plusDays(1);

            startDatoPaaTmpOT = startDatoPaaTmpOT.isBefore(orgenhetPaaRessurs.get().getGyldigFom()) ?
                orgenhetPaaRessurs.get().getGyldigFom() :
                startDatoPaaTmpOT;

            OrgtilknytningState ot = new OrgtilknytningState(
                fid,
                kommunalRessurs.getNavIdent(),
                orgenhetPaaRessurs.get().getId(),
                true,
                startDatoPaaTmpOT,
                Optional.ofNullable(kommunalRessurs.getSluttdato()),
                Optional.empty()
            );
            return Optional.of(ot);
        }
        return Optional.empty();
    }

    private static LocalDate maksSluttDato(Collection<OrgtilknytningState> eksisterendeOrgtilknytninger) {
        return eksisterendeOrgtilknytninger.stream()
            .map(OrgtilknytningState::gyldigTom)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .max(LocalDate::compareTo)
            .orElse(null);
    }

    private Optional<IOrgenhet> getOrgenhetPaaRessurs(DvhRessurs dvhRessurs) {
        return orgenhetService.getEnhetByAgressoIdNivaAndName(
            dvhRessurs.getOrgenhetPaaRessurs(),
            dvhRessurs.getOrgenhetPaaRessursNiva(),
            dvhRessurs.getOrgenhetPaaRessursBeskrivelse()
        );
    }

    private String detaljerToString(DvhRessurs kommunalRessurs, List<StillingPeriode> stillingPerioder, Collection<OrgtilknytningState> eksisterendeOrgtilknytninger) {
        StringBuffer strbuf = new StringBuffer();
        strbuf.append("DvhRessurs rawData:\n\t").append(kommunalRessurs.getRawData()).append("\n");
        strbuf.append("StillingPerioder:\n");
        StillingPeriode.toString(stillingPerioder, true);
        strbuf.append("Eksisterende orgtilknytninger:\n");
        eksisterendeOrgtilknytninger.forEach( o -> strbuf.append("\t").append(o).append("\n"));
        return strbuf.toString();
    }
}
