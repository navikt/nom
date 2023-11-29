package no.nav.nom.apps.api.teamcat;

public record TeamcatOrgenhetDTO(
    String id,
    String navn,

    String enhetOverId,
    String avdelingnavn,
    String lederNavident) {
}
