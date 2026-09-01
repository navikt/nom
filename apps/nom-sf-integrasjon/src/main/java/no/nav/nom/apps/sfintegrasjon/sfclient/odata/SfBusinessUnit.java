package no.nav.nom.apps.sfintegrasjon.sfclient.odata;

import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FOBusinessUnit;
import org.apache.commons.lang3.NotImplementedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public final class SfBusinessUnit implements SfOrgEnhet {
    FOBusinessUnit businessUnit;
    LocalDate startDate;
    LocalDate endDate;

    public SfBusinessUnit(FOBusinessUnit businessUnit) {
        this.businessUnit = businessUnit;
        this.startDate = businessUnit.getStartDate() != null ? businessUnit.getStartDate().toLocalDate() : null;
        this.endDate = businessUnit.getEndDate() != null ? businessUnit.getEndDate().toLocalDate() : null;
    }

    public FOBusinessUnit getFOBusinessUnit() {
        return businessUnit;
    }

    @Override
    public SfOrgEnhetType type() {
        return SfOrgEnhetType.BUSINESS_UNIT;
    }

    @Override
    public String externalCode() {
        return businessUnit.getExternalCode();
    }

    @Override
    public String name() {
        return businessUnit.getName();
    }

    @Override
    public String description() { return ""; }

    @Override
    public Optional<String> cust_BusinessUnit() { return Optional.empty(); }

    @Override
    public Optional<String> cust_Division() { return Optional.empty(); }

    @Override
    public String cust_LegacyID() { return businessUnit.getCust_LegacyID();}

    @Override
    public Optional<String> cust_Location() { return Optional.empty(); }

    @Override
    public Optional<String> cust_Tilgangsenhet() { return Optional.empty(); }

    @Override
    public Optional<String> cust_LederNavIdent() { return Optional.empty(); }

    @Override
    public Optional<String> cust_Virksomhet() { return Optional.empty(); }

    @Override
    public Optional<String> cust_nom_nivaa() { return Optional.empty(); }

    @Override
    public Optional<String> cust_orgenhet_type() { return Optional.empty(); }

    @Override
    public LocalDate startDate() { return startDate; }

    @Override
    public LocalDate endDate() {
        return endDate;
    }

    @Override
    public String headOfUnit() {
        return businessUnit.getHeadOfUnit();
    }

    @Override
    public SfOrgEnhet parentEnhet() {
        throw new NotImplementedException("parentEnhet not implemented for SfBusinessUnit");
    }

    @Override
    public Optional<String> costCenter() {
        return Optional.empty();
    }

    @Override
    public Optional<String> division() {
        return Optional.empty();
    }

    @Override
    public Optional<String> location() {
        return Optional.empty();
    }

    @Override
    public Optional<String> virksomhet() {
        return Optional.empty();
    }

    @Override
    public String mdfSystemRecordId() {
        return businessUnit.getMdfSystemRecordId();
    }

    @Override
    public String status() {
        return businessUnit.getStatus();
    }

    @Override
    public String createdBy() {
        return businessUnit.getCreatedBy();
    }

    @Override
    public LocalDateTime created() {
        return businessUnit.getCreatedOn();
    }

    @Override
    public String lastModifiedBy() {
        return businessUnit.getLastModifiedBy();
    }

    @Override
    public LocalDateTime lastModified() {
        return businessUnit.getLastModifiedOn();
    }
}
