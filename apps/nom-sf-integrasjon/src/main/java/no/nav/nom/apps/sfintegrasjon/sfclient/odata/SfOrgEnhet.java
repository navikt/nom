package no.nav.nom.apps.sfintegrasjon.sfclient.odata;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public sealed interface SfOrgEnhet permits SfBusinessUnit, SfDepartment, SfDivision {
    SfOrgEnhetType type();

    String externalCode();
    String name();
    String description();

    Optional<String> cust_BusinessUnit();
    Optional<String> cust_Division();
    String cust_LegacyID();
    Optional<String> cust_Location();
    Optional<String> cust_Tilgangsenhet();
    Optional<String> cust_LederNavIdent();
    Optional<String> cust_Virksomhet();
    Optional<String> cust_nom_nivaa();
    Optional<String> cust_orgenhet_type();

    LocalDate startDate();
    LocalDate endDate();

    String headOfUnit();
    SfOrgEnhet parentEnhet();

    Optional<String> costCenter();
    Optional<String> division();
    Optional<String> location();
    Optional<String> virksomhet();

    String mdfSystemRecordId();
    String status();

    String createdBy();
    LocalDateTime created();
    String lastModifiedBy();
    LocalDateTime lastModified();
}
