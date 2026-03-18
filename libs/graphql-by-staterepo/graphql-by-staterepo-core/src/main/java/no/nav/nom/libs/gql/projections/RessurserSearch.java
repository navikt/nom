package no.nav.nom.libs.gql.projections;


import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static java.util.Collections.emptySet;

public class RessurserSearch {
    public static final RessurserSearch EMPTY_SEARCH = emptyRessurserSearch();
    private final Set<String> personIdenter;
    private final Set<String> navIdenter;

    public RessurserSearch(
        Set<String> personidenter,
        Set<String> personIdenter,
        Set<String> navidenter,
        Set<String> navIdenter
    ) {
        var personidenterSet = new HashSet<String>();
        Optional.ofNullable(personidenter).ifPresent(personidenterSet::addAll);
        Optional.ofNullable(personIdenter).ifPresent(personidenterSet::addAll);

        var navidenterSet = new HashSet<String>();
        Optional.ofNullable(navidenter).ifPresent(navidenterSet::addAll);
        Optional.ofNullable(navIdenter).ifPresent(navidenterSet::addAll);

        this.personIdenter = personidenterSet;
        this.navIdenter = navidenterSet;
    }

    @Deprecated(forRemoval = true)
    public Set<String> getPersonIdenter(){
        return this.personIdenter;
    }

    public Set<String> getPersonidenter(){
        return this.personIdenter;
    }

    @Deprecated(forRemoval = true)
    public Set<String> getNavIdenter(){
        return this.navIdenter;

    }

    public Set<String> getNavidenter(){
        return this.navIdenter;
    }

    public Set<String> resolvePersonidenter() {
        return this.personIdenter;
    }

    public Set<String> resolveNavidenter() {
        return this.navIdenter;
    }

    public static RessurserSearch emptyRessurserSearch() {
        return new RessurserSearch(emptySet(),emptySet(),emptySet(),emptySet() );
    }
}
