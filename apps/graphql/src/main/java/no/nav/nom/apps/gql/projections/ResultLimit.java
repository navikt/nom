package no.nav.nom.apps.gql.projections;

public interface ResultLimit {

    Limit getLimit();

    enum Limit {
        LIMIT_10(10),
        LIMIT_20(20),
        LIMIT_30(30),
        LIMIT_50(50),
        LIMIT_100(100),
        LIMIT_200(200),
        UNLIMITED;

        private final Integer value;

        Limit(int value) {
            this.value = value;
        }
        Limit() {
            value = null;
        }

        public Integer getValue() {
            return value;
        }
    }

}
