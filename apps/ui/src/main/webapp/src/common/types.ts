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

