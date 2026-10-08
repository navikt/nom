package no.nav.nom.apps.sfintegrasjon.sfclient.odata;

import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FODepartment;
import no.nav.nom.sf.odata.vdm.namespaces.successfactors.FODivision;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

public final class SfDepartment implements SfOrgEnhet {
    FODepartment department;
    LocalDate startDate;
    LocalDate endDate;
    public SfDepartment(FODepartment department) {
        this.department = department;
        this.startDate = department.getStartDate() != null ? department.getStartDate().toLocalDate() : null;
        this.endDate = department.getEndDate() != null ? department.getEndDate().toLocalDate() : null;
    }

    public FODepartment getFODepartment() {
        return department;
    }

    @Override
    public SfOrgEnhetType type() {
        return SfOrgEnhetType.DEPARTMENT;
    }

    @Override
    public String externalCode() {
        return department.getExternalCode();
    }

    @Override
    public String name() {
        return department.getName();
    }

    @Override
    public String description() { return ""; }

    @Override
    public Optional<String> cust_BusinessUnit() { return Optional.ofNullable(department.getCust_Division()); }

    @Override
    public Optional<String> cust_Division() { return Optional.ofNullable(department.getCust_Division()); }

    @Override
    public String cust_LegacyID() { return department.getCust_LegacyID();}

    @Override
    public Optional<String> cust_Location() { return Optional.ofNullable(department.getCust_Location()); }

    @Override
    public Optional<String> cust_Tilgangsenhet() { return Optional.ofNullable(department.getCust_Tilgangsenhet());}

    @Override
    public Optional<String> cust_LederNavIdent() { return Optional.ofNullable(department.getCust_LederNavIdent()); }

    @Override
    public Optional<String> cust_Virksomhet() { return Optional.ofNullable(department.getCust_Virksomhet()); }

    @Override
    public Optional<String> cust_nom_nivaa() { return Optional.ofNullable(department.getCust_nom_nivaa()); }

    @Override
    public Optional<String> cust_orgenhet_type() { return Optional.ofNullable(department.getCust_orgenhet_type()); }

    @Override
    public LocalDate startDate() { return startDate; }

    @Override
    public LocalDate endDate() {
        return endDate;
    }

    @Override
    public String headOfUnit() {
        return department.getHeadOfUnit();
    }

    FODivision parentDivision;
    @Override
    public SfOrgEnhet parentEnhet() {
        if (parentDivision == null) {
            parentDivision = department.getCust_DivisionNavOrFetch();
        }
        if (parentDivision != null) {
            return new SfDivision(parentDivision);
        }
        return null;
    }

    @Override
    public Optional<String> costCenter() {
        return Optional.ofNullable(department.getCostCenter());
    }

    @Override
    public Optional<String> division() {
        return Optional.ofNullable(department.getCust_Division());
    }

    @Override
    public Optional<String> location() {
        return Optional.ofNullable(department.getCust_Location());
    }

    @Override
    public Optional<String> virksomhet() {
        return Optional.ofNullable(department.getCust_Virksomhet());
    }

    @Override
    public String mdfSystemRecordId() {
        return department.getMdfSystemRecordId();
    }

    @Override
    public String status() {
        return department.getStatus();
    }

    @Override
    public String createdBy() {
        return department.getCreatedBy();
    }

    @Override
    public LocalDateTime created() {
        return department.getCreatedOn();
    }

    @Override
    public String lastModifiedBy() {
        return department.getLastModifiedBy();
    }

    @Override
    public LocalDateTime lastModified() {
        return department.getLastModifiedOn();
    }
}
