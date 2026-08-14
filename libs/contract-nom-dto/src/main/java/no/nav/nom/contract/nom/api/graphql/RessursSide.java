package no.nav.nom.contract.nom.api.graphql;

import java.util.List;

public class RessursSide {

    private final List<Ressurs> content;
    private final int totalCount;
    private final int totalPages;
    private final int pageNumber;
    private final int pageSize;
    private final int numberOfElements;
    private final boolean hasNextPage;
    private final boolean hasPreviousPage;
    private final boolean first;
    private final boolean last;

    public RessursSide(
        List<Ressurs> content,
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
        this.content = content;
        this.totalCount = totalCount;
        this.totalPages = totalPages;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.numberOfElements = numberOfElements;
        this.hasNextPage = hasNextPage;
        this.hasPreviousPage = hasPreviousPage;
        this.first = first;
        this.last = last;
    }

    public List<Ressurs> getContent() {
        return content;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public int getPageSize() {
        return pageSize;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public boolean getHasNextPage() {
        return hasNextPage;
    }

    public boolean getHasPreviousPage() {
        return hasPreviousPage;
    }

    public boolean getFirst() {
        return first;
    }

    public boolean getLast() {
        return last;
    }
}
