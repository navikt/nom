package no.nav.nom.libs.gql.projections;


import java.util.Optional;

/**
 * Projected payload for binding the graphql input type RessursSearch
 * Use resolveIdent() to resolve the current navident, and then the personident
 */
public class RessursSearch {

    private final String navident;
    private final String telefonNummer;
    private final String personident;

    public RessursSearch(String navident, String telefonNummer, String personident, String navIdent, String personIdent) {
        this.navident = navident != null ? navident : navIdent;
        this.telefonNummer = telefonNummer;
        this.personident = personident != null ? personident : personIdent;
    }

    @Deprecated(forRemoval = true)
    public String getNavIdent(){
        return this.navident;
    }

    public String getNavident(){
        return this.navident;
    }

    public String getTelefonNummer(){
        return this.telefonNummer;
    }

    @Deprecated(forRemoval = true)
    public String getPersonIdent(){
        return this.personident;
    }

    public String getPersonident(){
        return this.personident;
    }

    public boolean isPersonidentPresent() {
        return getPersonident() != null || getPersonIdent() != null;
    }

    public String resolvePersonident() {
        return getPersonident() != null ? getPersonident() : getPersonIdent();
    }

    public boolean isNavidentPresent() {
        return getNavIdent() != null || getNavident() != null;
    }

    public String resolveNavident() {
        return getNavident() != null ? getNavident() : getNavIdent();
    }

    public Optional<String> resolveIdent() {
        if (isNavidentPresent()) {
            return Optional.of(resolveNavident());
        } else if (isPersonidentPresent()) {
            return Optional.of(resolvePersonident());
        }
        return Optional.empty();
    }
}
