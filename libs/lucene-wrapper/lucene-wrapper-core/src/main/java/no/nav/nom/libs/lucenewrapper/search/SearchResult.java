package no.nav.nom.libs.lucenewrapper.search;

public record SearchResult(String id, float score, String entType, String name) {
}
