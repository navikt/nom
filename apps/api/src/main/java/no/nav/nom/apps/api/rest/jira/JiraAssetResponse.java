package no.nav.nom.apps.api.rest.jira;

public record JiraAssetResponse(
    String brukerstednummer,
    String brukerstednavn,
    String besoksadresse,
    String postnummer,
    String poststed,
    String kommune,
    Integer antallAnsatte
) {
}
