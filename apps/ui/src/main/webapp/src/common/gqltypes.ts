export interface GqlRessurs {
  nomId: string;
  navIdent: string;
  personIdent: string;
  koblinger: GqlRessursKobling[];
  lederFor: GqlLederOrganisasjonsenhet[];
  ledere: GqlRessursLeder[];
  person?: GqlPerson;
  visningsnavn?: string;
  epost: string;
}

export interface GqlRessursKobling {
  organisasjonsenhet: GqlOrganisasjonsenhet;
}

export interface GqlLederOrganisasjonsenhet {
  organisasjonsenhet: GqlOrganisasjonsenhet;
}

export interface GqlRessursLeder {
  ressurs: GqlRessurs;
}
export interface GqlPerson {
  navn: GqlNavn;
}

export interface GqlNavn {
  fornavn: string;
  mellomnavn?: string;
  etternavn: string;
}

export interface GqlOrganisasjonsenhet {
  agressoId: string;
  navn: string;
  gyldigFom: string;
  gyldigTom: string;
  organiseringer: GqlOrganisering[];
  leder: GqlOrganisasjonsenhetsLeder[];
  koblinger: GqlOrganisasjonsenhetsKobling[];
  type?: Kode
}

export interface GqlOrganisering {
  retning: "over" | "under";
  organisasjonsenhet: GqlOrganisasjonsenhet;
  gyldigFom: string;
  gyldigTom: string;
}

export interface GqlOrganisasjonsenhetsLeder {
  ressurs: GqlRessurs;
}

export interface GqlOrganisasjonsenhetsKobling {
  ressurs: GqlRessurs;
  gyldigFom: string;
  gyldigTom: string;
}

export interface Kode {
  kode: string;
  navn: string;
  gyldigFom?: string;
  gyldigTom?: string;
}

export interface GqlResponse {
  data?:
    | GqlRessurs
    | GqlRessurs[]
    | GqlOrganisasjonsenhet
    | GqlOrganisasjonsenhet[];
  error?: any;
}
