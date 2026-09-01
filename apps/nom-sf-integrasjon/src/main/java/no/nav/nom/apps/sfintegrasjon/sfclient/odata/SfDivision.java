package no.nav.nom.apps.sfintegrasjon.sfclient.odata;

import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FODivision;
import org.apache.commons.lang3.NotImplementedException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public final class SfDivision implements SfOrgEnhet {
    FODivision division;
    LocalDate startDate;
    LocalDate endDate;

    public SfDivision(FODivision division) {
        this.division = division;
        this.startDate = division.getStartDate() != null ? division.getStartDate().toLocalDate() : null;
        this.endDate = division.getEndDate() != null ? division.getEndDate().toLocalDate() : null;
    }

    public FODivision getFODivision() {
        return division;
    }

    @Override
    public SfOrgEnhetType type() {
        return SfOrgEnhetType.DIVISION;
    }

    @Override
    public String externalCode() {
        return division.getExternalCode();
    }

    @Override
    public String name() {
        return division.getName();
    }

    @Override
    public String description() { return division.getDescription(); }

    @Override
    public Optional<String> cust_BusinessUnit() { return Optional.ofNullable(division.getCust_BusinessUnit()); }

    @Override
    public Optional<String> cust_Division() { return Optional.empty(); }

    @Override
    public String cust_LegacyID() { return division.getCust_LegacyID();}

    @Override
    public Optional<String> cust_Tilgangsenhet() { return Optional.ofNullable(division.getCust_Tilgangsenhet());}

    @Override
    public Optional<String> cust_LederNavIdent() { return Optional.ofNullable(division.getCust_leder_navident()); }

    @Override
    public Optional<String> cust_Virksomhet() { return Optional.empty(); }

    @Override
    public Optional<String> cust_nom_nivaa(){ return Optional.ofNullable(division.getCust_nom_nivaa()); }

    @Override
    public Optional<String> cust_orgenhet_type() { return Optional.ofNullable(division.getCust_orgenhet_type()); }

    @Override
    public Optional<String> cust_Location() { return Optional.empty(); }

    @Override
    public LocalDate startDate() { return startDate; }

    @Override
    public LocalDate endDate() {
        return endDate;
    }

    @Override
    public String headOfUnit() {
        return division.getHeadOfUnit();
    }

    FODivision divisionParent;
    @Override
    public SfOrgEnhet parentEnhet() {
        if (divisionParent == null) {
            divisionParent = division.getParentNavOrFetch();
        }
        if (divisionParent != null) {
            return new SfDivision(divisionParent);
        }
        var businessUnit = division.getCust_BusinessUnitNavOrFetch();
        if (businessUnit != null) {
            return new SfBusinessUnit(businessUnit);
        }
        return null;
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
        return division.getMdfSystemRecordId();
    }

    @Override
    public String status() {
        return division.getStatus();
    }

    @Override
    public String createdBy() {
        return division.getCreatedBy();
    }

    @Override
    public LocalDateTime created() {
        return division.getCreatedOn();
    }

    @Override
    public String lastModifiedBy() {
        return division.getLastModifiedBy();
    }

    @Override
    public LocalDateTime lastModified() {
        return division.getLastModifiedOn();
    }
}
