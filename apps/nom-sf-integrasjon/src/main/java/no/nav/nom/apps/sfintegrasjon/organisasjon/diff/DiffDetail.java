package no.nav.nom.apps.sfintegrasjon.organisasjon.diff;

import no.nav.nom.apps.sfintegrasjon.sfclient.odata.SfOrgEnhetType;

import java.time.LocalDate;

public interface DiffDetail {
    OrgenhetTypeOfDiff typeOfDiff();
    String description();
}

record MissingInSf(String nomId) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "Finner ingen SF enheter for NOM enhet %s".formatted(nomId);
    }
}

record NomNivaaMissingInSF(LocalDate date, String nomNivaa) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NOM_NIVAA_MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s mangler NOM Nivå, forventet %s".formatted(date, nomNivaa);
    }
}

record NomNivaaMismatch(LocalDate date, String sfNivaa, String nomNivaa) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NOM_NIVAA_MISMATCH;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s har NOM Nivå %s, forventet %s".formatted(date, sfNivaa, nomNivaa);
    }
}

record NameMissingInSF(LocalDate date, String nomName) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NAME_MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s mangler navn, forventet %s".formatted(date, nomName);
    }
}

record NameMismatch(LocalDate date, String sfName, String nomName) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NAME_MISMATCH;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s har navn %s, forventet %s".formatted(date, sfName, nomName);
    }
}

record NomTypeMissingInSF(LocalDate date, String nomType) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NOM_TYPE_MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s mangler NOM Type, forventet %s".formatted(date, nomType);
    }
}

record NomTypeMismatch(LocalDate date, String sfType, String nomType) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NOM_TYPE_MISMATCH;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s har NOM Type %s, forventet %s".formatted(date, sfType, nomType);
    }
}

record NomTilgangsenhetMissingInSF(LocalDate date, String nomTilgangsenhet) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NOM_TILGANGSENHET_MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s mangler NOM Tilgangsenhet, forventet %s".formatted(date, nomTilgangsenhet);
    }
}

record NomTilgangsenhetMismatch(LocalDate date, String sfTilgangsenhet, String nomTilgangsenhet) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.NOM_TILGANGSENHET_MISMATCH;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s har NOM Tilgangsenhet %s, forventet %s".formatted(date, sfTilgangsenhet, nomTilgangsenhet);
    }
}

record KostnadsstedMissingInSF(LocalDate date, String nomKostnadssted) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.KOSTNADSTED_MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s mangler Kostnadssted, forventet %s".formatted(date, nomKostnadssted);
    }
}

record KostnadsstedMismatch(LocalDate date, String sfKostnadssted, String nomKostnadssted) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.KOSTNADSTED_MISMATCH;
    }

    @Override
    public String description() {
        return "Versjon med startdato %s har Kostnadssted %s, forventet %s".formatted(date, sfKostnadssted, nomKostnadssted);
    }
}

record LederMissingInNOM(LocalDate date, String sfLeder) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.LEDER_MISSING_IN_NOM;
    }

    @Override
    public String description() {
        return "På dato %s mangler NOM leder, leder i SF er %s".formatted(date, sfLeder);
    }
}

record LederMissingInSF(LocalDate date, String nomLeder) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.LEDER_MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "På dato %s mangler SF leder, forventet %s".formatted(date, nomLeder);
    }
}

record LederMismatchDetail(LocalDate date, String sfLeder, String nomLeder) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.LEDER_MISMATCH;
    }

    @Override
    public String description() {
        return "På dato %s har SF leder %s, forventet %s".formatted(date, sfLeder, nomLeder);
    }
}

record ParentMissingInNOM(LocalDate date, String sfParentId) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.PARENT_MISSING_IN_NOM;
    }

    @Override
    public String description() {
        return "På dato %s mangler NOM parent enhet, parent enhet i SF er %s".formatted(date, sfParentId);
    }
}

record ParentMissingInSF(LocalDate date, String nomParentId) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.PARENT_MISSING_IN_SF;
    }

    @Override
    public String description() {
        return "På dato %s mangler SF parent enhet, forventet %s".formatted(date, nomParentId);
    }
}

record ParentMismatchDetail(LocalDate date, String sfParentId, String nomParentId) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.PARENT_MISMATCH;
    }

    @Override
    public String description() {
        return "På dato %s har SF parent %s, forventet %s".formatted(date, sfParentId, nomParentId);
    }
}

record SFEnhetsObjektMismatch(LocalDate date, SfOrgEnhetType sfType, SfOrgEnhetType forventetType) implements DiffDetail {
    @Override
    public OrgenhetTypeOfDiff typeOfDiff() {
        return OrgenhetTypeOfDiff.SF_ENHETS_OBJEKT_MISMATCH;
    }

    @Override
    public String description() {
        return "På dato %s er SF enhet av type %s, forventet %s".formatted(date, sfType, forventetType);
    }
}