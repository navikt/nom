package no.nav.nom.apps.sfintegrasjon.sfhendelse.dto;

import no.nav.nom.apps.sfintegrasjon.sfhendelse.Stilling;
import no.nav.nom.contract.nom.Sektor;
import no.nav.nom.contract.nom.Tilknytningtype;
import no.nav.nom.contract.nom.state.OrgtilknytningState;

import java.time.LocalDate;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public record OrgtilknytningerEndring(
    List<OrgtilknytningerEndring.EksisterendeOt> eksisterende,
    List<OrgtilknytningerEndring.NyOt> nye
) implements HendelseDiff {
    public record EksisterendeOt(
        String fid,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        Sektor sektor,
        Boolean erDagligOppfolging,
        Tilknytningtype tilknytningtype,
        String primaryFysiskBrukerstedNummer,
        Set<String> optionalFysiskBrukersteder
    ) {
        public static OrgtilknytningerEndring.EksisterendeOt from(OrgtilknytningState orgtilknytningState) {
            return new OrgtilknytningerEndring.EksisterendeOt(
                orgtilknytningState.fid(),
                orgtilknytningState.gyldigFom(),
                orgtilknytningState.gyldigTom(),
                orgtilknytningState.sektor(),
                orgtilknytningState.erDagligOppfolging(),
                orgtilknytningState.orgtilknytningType(),
                orgtilknytningState.primaryFysiskBrukerstedNummer(),
                new HashSet<>(orgtilknytningState.additionalFysiskeBrukerstederNummer())
            );
        }

        public static OrgtilknytningerEndring.EksisterendeOt from(String fid, Stilling stilling) {
            return new OrgtilknytningerEndring.EksisterendeOt(
                fid,
                stilling.gyldigFom(),
                stilling.gyldigTom(),
                Sektor.NAV_STATLIG,
                stilling.erDol(),
                stilling.tilknytningtype(),
                stilling.primaryFysiskBrukersted(),
                new HashSet<>()
            );
        }
    }

    public record NyOt(
        String orgEnhetId,
        LocalDate gyldigFom,
        LocalDate gyldigTom,
        Sektor sektor,
        Boolean erDagligOppfolging,
        Tilknytningtype tilknytningtype,
        String primaryFysiskBrukerstedNummer,
        Set<String> optionalFysiskBrukersteder
    ) {
        public static NyOt from(Stilling stilling) {
            return new NyOt(
                stilling.orgenhetId(),
                stilling.gyldigFom(),
                stilling.gyldigTom(),
                Sektor.NAV_STATLIG,
                stilling.erDol(),
                stilling.tilknytningtype(),
                stilling.primaryFysiskBrukersted(),
                Collections.emptySet()
            );
        }
    }

    static OrgtilknytningerEndring from(List<OrgtilknytningState> eksisterende, List<OrgtilknytningerEndring.NyOt> nye) {
        return new OrgtilknytningerEndring(
            eksisterende.stream().map(OrgtilknytningerEndring.EksisterendeOt::from).toList(),
            nye
        );
    }
}
