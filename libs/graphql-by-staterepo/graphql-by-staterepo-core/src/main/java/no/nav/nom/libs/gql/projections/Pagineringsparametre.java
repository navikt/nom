package no.nav.nom.libs.gql.projections;

public record Pagineringsparametre(int pageNumber, int pageSize) {

    public static final int MAKS_SIDESTORRELSE = 1000;

    public static final Pagineringsparametre DEFAULT = new Pagineringsparametre(0, 100);
}
