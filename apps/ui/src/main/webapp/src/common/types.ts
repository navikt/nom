export interface OrgEnhetFelter {
  enhetNr: string;
  enhetNiva: string;
  enhetNavn: string;
  enhetType: string;
  gyldigFom: string;
  gyldigTom: string;
}

export interface OrgEnhetKoblinger {
  sti: OrgEnhetFelter[];
  underEnheter: OrgEnhetFelter[];
  ressurser: Ressurs[];
}

export interface OrgEnhetData {
  data: OrgEnhetFelter;
  koblinger: OrgEnhetKoblinger;
  ledere: Ressurs[];
}

export interface OrganisasjonsEnhet {
  id: string;
  navn: string;
  gyldigFom: string;
  gyldigTom: string;
}

export interface Ressurs {
  id: string;
  pnr: string;
  person?: {
    navn: {
      fornavn?: string;
      mellomnavn?: string;
      etternavn?: string;
    };
  };
}

export interface RessursFlat {
  id: string;
  pnr: string;
  fornavn?: string;
  mellomnavn?: string;
  etternavn?: string;
}
