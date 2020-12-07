export interface OrgEnhetFelter{
    enhetNr: string;
    enhetNavn: string;
    enhetType: string;
}

export interface OrgEnhetKoblinger{
    sti: OrgEnhetFelter[];
    underEnheter: OrgEnhetFelter[];
}

export interface OrgEnhetData{
    data: OrgEnhetFelter;
    koblinger: OrgEnhetKoblinger;
}

export interface OrganisasjonsEnhet{
    id: string;
    navn: string;
    gyldigFom: string;
    gyldigTom: string;
}