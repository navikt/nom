export interface OrgEnhetFelter {
  enhetNr: string;
  enhetNavn: string;
  enhetType: string;
}

export interface OrgEnhetKoblinger {
  sti: OrgEnhetFelter[];
  underEnheter: OrgEnhetFelter[];
  ressurser: Ressurs[]
}

export interface OrgEnhetData {
  data: OrgEnhetFelter;
  koblinger: OrgEnhetKoblinger;
  leder: Ressurs
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
      fornavn?: string,
      mellomnavn?: string,
      etternavn?: string,
    }
  }
}
