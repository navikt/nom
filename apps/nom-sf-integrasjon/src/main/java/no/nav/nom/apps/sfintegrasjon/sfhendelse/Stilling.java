package no.nav.nom.apps.sfintegrasjon.sfhendelse;

import lombok.Builder;
import no.nav.nom.contract.nom.Tilknytningtype;

import java.time.LocalDate;
import java.util.Objects;

@Builder
public record Stilling(
    StillingId stillingId,
    LocalDate gyldigFom,
    LocalDate gyldigTom,
    String primaryFysiskBrukersted
) {
    record StillingId(
        String orgenhetId,
        Tilknytningtype tilknytningtype,
        Boolean erDol
    ) {
        @Override
        public boolean equals(Object o) {
            var other = (StillingId) o;

            return orgenhetId.equals(other.orgenhetId())
                && tilknytningtype == other.tilknytningtype()
                && erDol.equals(other.erDol());
        }

        @Override
        public int hashCode() {
            return Objects.hash(orgenhetId, tilknytningtype);
        }

        public Boolean erDoll() {
            return erDol;
        }
    }

    public String orgenhetId() {
        return stillingId.orgenhetId();
    }

    public Boolean erDol() {
        return stillingId.erDol();
    }

    public Tilknytningtype tilknytningtype() {
        return stillingId.tilknytningtype();
    }
}
