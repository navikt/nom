package no.nav.nom.libs.state.common;

public record VisitorError<T>(String key, T stateObject, Exception e) {}
