package no.nav.nom.libs.lucenewrapper.search;

public interface QueryFilter<T> {

    Class<T> getEntityClass();


    Mode mode();


    /**
     * {@link QueryFilter} mode
     */
    enum Mode {
        /**
         * Filter mode when the targeted type shall be excluded from the search result
         */
        EXCLUDE,

        /**
         * Filter mode when the targeted type T shall be filtered according to type T's default filter
         */
        INCLUDE,

        /**
         * Filter mode when the targeted type T shall be filtered according to a {@link QueryFilter}'s payload
         */
        CUSTOM,
    }

}
