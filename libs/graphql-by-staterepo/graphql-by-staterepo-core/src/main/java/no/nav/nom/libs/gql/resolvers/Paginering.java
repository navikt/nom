package no.nav.nom.libs.gql.resolvers;

import no.nav.nom.libs.gql.projections.Pagineringsparametre;

import java.util.List;

final class Paginering {

    private Paginering() {
    }

    record Side<T>(
        List<T> innhold,
        int totalCount,
        int totalPages,
        int pageNumber,
        int pageSize,
        int numberOfElements,
        boolean hasNextPage,
        boolean hasPreviousPage,
        boolean first,
        boolean last
    ) {
    }

    static void valider(Pagineringsparametre paginering) {
        if (paginering.pageNumber() < 0) {
            throw new IllegalArgumentException("pageNumber må være >= 0, men var " + paginering.pageNumber());
        }
        if (paginering.pageSize() < 1) {
            throw new IllegalArgumentException("pageSize må være >= 1, men var " + paginering.pageSize());
        }
        if (paginering.pageSize() > Pagineringsparametre.MAKS_SIDESTORRELSE) {
            throw new IllegalArgumentException("pageSize kan ikke overstige " + Pagineringsparametre.MAKS_SIDESTORRELSE + ", men var " + paginering.pageSize());
        }
    }

    static <T> Side<T> paginer(List<T> sortert, Pagineringsparametre paginering) {
        var pageNumber = paginering.pageNumber();
        var pageSize = paginering.pageSize();
        var totalCount = sortert.size();
        var totalPages = (int) Math.ceil((double) totalCount / pageSize);

        var innhold = sortert.stream()
            .skip((long) pageNumber * pageSize)
            .limit(pageSize)
            .toList();

        var numberOfElements = innhold.size();
        var hasPreviousPage = pageNumber > 0;
        var hasNextPage = (long) (pageNumber + 1) * pageSize < totalCount;
        var first = pageNumber == 0;
        var last = pageNumber >= totalPages - 1;

        return new Side<>(
            innhold,
            totalCount,
            totalPages,
            pageNumber,
            pageSize,
            numberOfElements,
            hasNextPage,
            hasPreviousPage,
            first,
            last
        );
    }
}
