export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
/** All built-in and custom scalars, mapped to their actual values */
export type Scalars = {
  ID: { input: string; output: string; }
  String: { input: string; output: string; }
  Boolean: { input: boolean; output: boolean; }
  Int: { input: number; output: number; }
  Float: { input: number; output: number; }
  Date: { input: any; output: any; }
  DateTime: { input: any; output: any; }
  JSON: { input: any; output: any; }
};

export type AgressoInfo = {
  __typename?: 'AgressoInfo';
  agressoId?: Maybe<Scalars['ID']['output']>;
  agressoOrgNiv?: Maybe<Scalars['String']['output']>;
  agressoOrgenhetType?: Maybe<Scalars['String']['output']>;
};

export enum GyldigTomSelection {
  Alle = 'ALLE',
  IkkePassert = 'IKKE_PASSERT',
  Passert = 'PASSERT'
}

export enum IdentType {
  Robot = 'ROBOT',
  Standard = 'STANDARD',
  Ukjent = 'UKJENT',
  UtenlandskKonsulent = 'UTENLANDSK_KONSULENT',
  Vikafossen = 'VIKAFOSSEN'
}

export enum IdentTypeSelection {
  Alle = 'ALLE',
  Robot = 'ROBOT',
  Standard = 'STANDARD',
  Ukjent = 'UKJENT',
  UtenlandskKonsulent = 'UTENLANDSK_KONSULENT',
  Vikafossen = 'VIKAFOSSEN'
}

export type LederOrgEnhet = {
  __typename?: 'LederOrgEnhet';
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  orgEnhet: OrgEnhet;
};

export enum LovnodeSelection {
  Alle = 'ALLE',
  IkkeLovnodeEnhet = 'IKKE_LOVNODE_ENHET',
  LovnodeEnhet = 'LOVNODE_ENHET'
}

export enum NomNivaa {
  Arbeidsomraade = 'ARBEIDSOMRAADE',
  Driftsenhet = 'DRIFTSENHET',
  Linjeenhet = 'LINJEENHET'
}

export type OrgEnhet = {
  __typename?: 'OrgEnhet';
  /** @deprecated agressoId er erstattet med agressoInfo.agressoId */
  agressoId: Scalars['ID']['output'];
  /**  Deprekert Nov.2025 */
  agressoInfo?: Maybe<AgressoInfo>;
  /**
   *  Deprekert Nov.2025
   * @deprecated agressoOrgenhetType er erstattet med agressoInfo.agressoOrgenhetType
   */
  agressoOrgenhetType?: Maybe<Scalars['String']['output']>;
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  id: Scalars['ID']['output'];
  /** @deprecated koblinger er erstattet med orgTilknytninger */
  koblinger: Array<OrgEnhetsKobling>;
  /** @deprecated leder er erstattet med ledere */
  leder: Array<OrgEnhetsLeder>;
  /**  Deprekert Nov.2025 */
  ledere: Array<OrgEnhetsLeder>;
  navn: Scalars['String']['output'];
  nomNivaa?: Maybe<NomNivaa>;
  orgEnhetsType?: Maybe<OrgEnhetsType>;
  /**
   *  Deprekert Nov.2025
   * @deprecated orgNiv er erstattet med agressoInfo.agressoOrgNiv
   */
  orgNiv: Scalars['String']['output'];
  /**  Deprekert Nov.2025 */
  orgTilknytninger: Array<OrgTilknytning>;
  organiseringer: Array<Organisering>;
  remedyEnhetId?: Maybe<Scalars['String']['output']>;
  tilgangsenhetId?: Maybe<Scalars['String']['output']>;
};


export type OrgEnhetLedereArgs = {
  utvalg?: InputMaybe<PeriodiskUtvalgArg>;
};


export type OrgEnhetOrgTilknytningerArgs = {
  utvalg?: InputMaybe<PeriodiskUtvalgArg>;
};


export type OrgEnhetOrganiseringerArgs = {
  retning?: InputMaybe<Retning>;
  utvalg?: InputMaybe<PeriodiskUtvalgArg>;
};

export type OrgEnhetResult = {
  __typename?: 'OrgEnhetResult';
  agressoId?: Maybe<Scalars['String']['output']>;
  agressoOrgNiv?: Maybe<Scalars['String']['output']>;
  code: ResultCode;
  /** @deprecated id er erstattet med agressoId */
  id?: Maybe<Scalars['String']['output']>;
  nomId?: Maybe<Scalars['String']['output']>;
  orgEnhet?: Maybe<OrgEnhet>;
  /** @deprecated orgNiv er erstattet med agressoOrgNiv */
  orgNiv?: Maybe<Scalars['String']['output']>;
  /** @deprecated organisasjonsenhet er erstattet med orgEnhet */
  organisasjonsenhet?: Maybe<OrgEnhet>;
};

export type OrgEnhetSearch = {
  agressoId?: InputMaybe<Scalars['String']['input']>;
  agressoOrgNiv?: InputMaybe<Scalars['String']['input']>;
  id?: InputMaybe<Scalars['String']['input']>;
  navn?: InputMaybe<Scalars['String']['input']>;
};

export type OrgEnhetSearchFilter = {
  gyldigTomSelection?: GyldigTomSelection;
  limit?: ResultLimit;
  orgnivSelection?: OrgnivSelection;
};

export type OrgEnhetSide = {
  __typename?: 'OrgEnhetSide';
  content: Array<OrgEnhet>;
  first: Scalars['Boolean']['output'];
  hasNextPage: Scalars['Boolean']['output'];
  hasPreviousPage: Scalars['Boolean']['output'];
  last: Scalars['Boolean']['output'];
  numberOfElements: Scalars['Int']['output'];
  pageNumber: Scalars['Int']['output'];
  pageSize: Scalars['Int']['output'];
  totalCount: Scalars['Int']['output'];
  totalPages: Scalars['Int']['output'];
};

export type OrgEnheterSearch = {
  agressoIder?: InputMaybe<Array<Scalars['String']['input']>>;
  agressoOrgNiv?: InputMaybe<Scalars['String']['input']>;
  ider?: InputMaybe<Array<Scalars['String']['input']>>;
};

export type OrgEnheterSearchFilter = {
  lovnodeSelection?: InputMaybe<LovnodeSelection>;
  nomNivaaSelection?: InputMaybe<Array<NomNivaa>>;
  organiseringSelection?: InputMaybe<OrganiseringSelection>;
  organisertUnderOrgEnhetId?: InputMaybe<Scalars['String']['input']>;
  periodiskUtvalg?: InputMaybe<PeriodiskUtvalgArg>;
  tilgangsenhetId?: InputMaybe<Scalars['String']['input']>;
};

export type OrgEnhetsKobling = {
  __typename?: 'OrgEnhetsKobling';
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  /**  Deprekeres */
  ressurs: Ressurs;
};

export type OrgEnhetsLeder = {
  __typename?: 'OrgEnhetsLeder';
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  ressurs: Ressurs;
};

export enum OrgEnhetsType {
  Arbeidslivssenter = 'ARBEIDSLIVSSENTER',
  Arbeidsraadgivning = 'ARBEIDSRAADGIVNING',
  Direktorat = 'DIREKTORAT',
  /**     DIR - Hvis det skulle finnes DIR type på Enhet i NOM, konverteres denne til DIREKTORAT i GQL */
  Fylke = 'FYLKE',
  HjelpemidlerOgTilrettelegging = 'HJELPEMIDLER_OG_TILRETTELEGGING',
  Klageinstans = 'KLAGEINSTANS',
  KontrollKontrollenhet = 'KONTROLL_KONTROLLENHET',
  NavArbeidOgYtelser = 'NAV_ARBEID_OG_YTELSER',
  NavFamilieOgPensjonsytelser = 'NAV_FAMILIE_OG_PENSJONSYTELSER',
  NavKontaktsenter = 'NAV_KONTAKTSENTER',
  NavKontor = 'NAV_KONTOR',
  NavOkonomitjeneste = 'NAV_OKONOMITJENESTE',
  Tiltak = 'TILTAK'
}

export type OrgTilknytning = {
  __typename?: 'OrgTilknytning';
  additionalFysiskeBrukerstederNummer?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  erDagligOppfolging?: Maybe<Scalars['Boolean']['output']>;
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  primaryFysiskBrukersted?: Maybe<Scalars['String']['output']>;
  ressurs: Ressurs;
  sektor?: Maybe<Sektor>;
  tilknytningType?: Maybe<TilknytningType>;
};

export type Organisering = {
  __typename?: 'Organisering';
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  orgEnhet: OrgEnhet;
  retning: Retning;
};

export enum OrganiseringSelection {
  Alle = 'ALLE',
  UnderAngittOrgenhet = 'UNDER_ANGITT_ORGENHET',
  UnderHelfo = 'UNDER_HELFO',
  UnderRiksrevisjonen = 'UNDER_RIKSREVISJONEN',
  UnderSkatt = 'UNDER_SKATT',
  UnderToppnodeNav = 'UNDER_TOPPNODE_NAV',
  UnderVikafossen = 'UNDER_VIKAFOSSEN'
}

export enum OrgnivSelection {
  Alle = 'ALLE',
  IkkeOrgnivOrgenhet = 'IKKE_ORGNIV_ORGENHET',
  OrgnivOrgenhet = 'ORGNIV_ORGENHET'
}

export type PagineringInput = {
  pageNumber?: Scalars['Int']['input'];
  pageSize?: Scalars['Int']['input'];
};

export enum PeriodiskUtvalgArg {
  Aktive = 'AKTIVE',
  Alle = 'ALLE',
  Fremtidige = 'FREMTIDIGE',
  Historiske = 'HISTORISKE'
}

export type Query = {
  __typename?: 'Query';
  orgEnhet?: Maybe<OrgEnhet>;
  orgEnheter: Array<OrgEnhetResult>;
  orgEnheterPaginert: OrgEnhetSide;
  /** @deprecated Bruk orgEnhet query med samme parametre */
  organisasjonsenhet?: Maybe<OrgEnhet>;
  /** @deprecated Bruk orgEnheter query med samme parametre */
  organisasjonsenheter: Array<OrgEnhetResult>;
  ressurs?: Maybe<Ressurs>;
  ressurser: Array<RessursResult>;
  ressurserPaginert: RessursSide;
  search: Array<SearchResult>;
  searchOrgEnhet: Array<OrgEnhet>;
  searchRessurs: Array<Ressurs>;
};


export type QueryOrgEnhetArgs = {
  where: OrgEnhetSearch;
};


export type QueryOrgEnheterArgs = {
  filter?: InputMaybe<OrgEnheterSearchFilter>;
  where?: InputMaybe<OrgEnheterSearch>;
};


export type QueryOrgEnheterPaginertArgs = {
  filter?: InputMaybe<OrgEnheterSearchFilter>;
  paginering?: InputMaybe<PagineringInput>;
};


export type QueryOrganisasjonsenhetArgs = {
  where: OrgEnhetSearch;
};


export type QueryOrganisasjonsenheterArgs = {
  where?: InputMaybe<OrgEnheterSearch>;
};


export type QueryRessursArgs = {
  where: RessursSearch;
};


export type QueryRessurserArgs = {
  filter?: InputMaybe<RessurserSearchFilter>;
  where?: InputMaybe<RessurserSearch>;
};


export type QueryRessurserPaginertArgs = {
  filter?: InputMaybe<RessurserSearchFilter>;
  paginering?: InputMaybe<PagineringInput>;
};


export type QuerySearchArgs = {
  orgEnhetFilter?: InputMaybe<OrgEnhetSearchFilter>;
  ressursFilter?: InputMaybe<RessursSearchFilter>;
  term: Scalars['String']['input'];
};


export type QuerySearchOrgEnhetArgs = {
  filter?: InputMaybe<OrgEnhetSearchFilter>;
  term: Scalars['String']['input'];
};


export type QuerySearchRessursArgs = {
  filter?: InputMaybe<RessursSearchFilter>;
  term: Scalars['String']['input'];
};

export type Ressurs = {
  __typename?: 'Ressurs';
  additionalFysiskeBrukerstederNummer: Array<Maybe<Scalars['String']['output']>>;
  epost?: Maybe<Scalars['String']['output']>;
  etternavn?: Maybe<Scalars['String']['output']>;
  fornavn?: Maybe<Scalars['String']['output']>;
  /**  Deprekert Nov.2025 */
  gjeldendeSektor: Sektor;
  gjeldendeTilknytningtype?: Maybe<TilknytningType>;
  identType: IdentType;
  lederFor: Array<LederOrgEnhet>;
  ledere: Array<RessursLeder>;
  /** @deprecated navIdent er erstattet med navident */
  navIdent: Scalars['String']['output'];
  navident: Scalars['String']['output'];
  /** @deprecated orgTilknytning er erstattet med orgTilknytninger */
  orgTilknytning: Array<RessursOrgTilknytning>;
  /**  Deprekert Nov.2025 */
  orgTilknytninger: Array<RessursOrgTilknytning>;
  /** @deprecated personIdent er erstattet med personident */
  personIdent: Scalars['String']['output'];
  personident: Scalars['String']['output'];
  primaryFysiskBrukersted: Scalars['String']['output'];
  primaryTelefon?: Maybe<Scalars['String']['output']>;
  /** @deprecated sektor er erstattet med gjeldendeSektor */
  sektor: Array<Sektor>;
  sluttdato?: Maybe<Scalars['Date']['output']>;
  startdato?: Maybe<Scalars['Date']['output']>;
  telefon: Array<Maybe<Telefon>>;
  /** @deprecated visningsNavn er erstattet med visningsnavn */
  visningsNavn?: Maybe<Scalars['String']['output']>;
  visningsnavn?: Maybe<Scalars['String']['output']>;
};


export type RessursLederForArgs = {
  utvalg?: InputMaybe<PeriodiskUtvalgArg>;
};


export type RessursLedereArgs = {
  utvalg?: InputMaybe<PeriodiskUtvalgArg>;
};


export type RessursOrgTilknytningerArgs = {
  utvalg?: InputMaybe<PeriodiskUtvalgArg>;
};

export type RessursLeder = {
  __typename?: 'RessursLeder';
  erDagligOppfolging?: Maybe<Scalars['Boolean']['output']>;
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  orgEnhet: OrgEnhet;
  ressurs: Ressurs;
};

export type RessursOrgTilknytning = {
  __typename?: 'RessursOrgTilknytning';
  additionalFysiskeBrukerstederNummer?: Maybe<Array<Maybe<Scalars['String']['output']>>>;
  erDagligOppfolging?: Maybe<Scalars['Boolean']['output']>;
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  orgEnhet: OrgEnhet;
  primaryFysiskBrukersted?: Maybe<Scalars['String']['output']>;
  sektor?: Maybe<Sektor>;
  tilknytningType?: Maybe<TilknytningType>;
};

export type RessursResult = {
  __typename?: 'RessursResult';
  code: ResultCode;
  id: Scalars['String']['output'];
  ressurs?: Maybe<Ressurs>;
};

export type RessursSearch = {
  navident?: InputMaybe<Scalars['String']['input']>;
  personident?: InputMaybe<Scalars['String']['input']>;
  telefonNummer?: InputMaybe<Scalars['String']['input']>;
};

export type RessursSearchFilter = {
  limit?: ResultLimit;
  sektorSelection?: SektorSelection;
  statusSelection?: StatusSelection;
};

export type RessursSide = {
  __typename?: 'RessursSide';
  content: Array<Ressurs>;
  first: Scalars['Boolean']['output'];
  hasNextPage: Scalars['Boolean']['output'];
  hasPreviousPage: Scalars['Boolean']['output'];
  last: Scalars['Boolean']['output'];
  numberOfElements: Scalars['Int']['output'];
  pageNumber: Scalars['Int']['output'];
  pageSize: Scalars['Int']['output'];
  totalCount: Scalars['Int']['output'];
  totalPages: Scalars['Int']['output'];
};

export type RessurserSearch = {
  navidenter?: InputMaybe<Array<Scalars['String']['input']>>;
  personidenter?: InputMaybe<Array<Scalars['String']['input']>>;
};

export type RessurserSearchFilter = {
  fysiskBrukersted?: InputMaybe<Scalars['String']['input']>;
  gjeldendeSektorSelection?: InputMaybe<SektorSelection>;
  identTypeSelection?: InputMaybe<IdentTypeSelection>;
  periodiskUtvalg?: InputMaybe<PeriodiskUtvalgArg>;
  tilgangsenhetId?: InputMaybe<Scalars['String']['input']>;
};

export enum ResultCode {
  Error = 'ERROR',
  NotFound = 'NOT_FOUND',
  NotInFilter = 'NOT_IN_FILTER',
  Ok = 'OK'
}

export enum ResultLimit {
  Limit_10 = 'LIMIT_10',
  Limit_20 = 'LIMIT_20',
  Limit_30 = 'LIMIT_30',
  Limit_50 = 'LIMIT_50',
  Limit_100 = 'LIMIT_100',
  Limit_200 = 'LIMIT_200',
  Unlimited = 'UNLIMITED'
}

export enum Retning {
  Over = 'over',
  Under = 'under'
}

export type SearchResult = OrgEnhet | Ressurs;

export enum Sektor {
  Ekstern = 'EKSTERN',
  NavKommunal = 'NAV_KOMMUNAL',
  NavStatlig = 'NAV_STATLIG'
}

export enum SektorSelection {
  Alle = 'ALLE',
  Ekstern = 'EKSTERN',
  IkkeEkstern = 'IKKE_EKSTERN',
  Kommunal = 'KOMMUNAL',
  Statlig = 'STATLIG'
}

export enum StatusSelection {
  Aktiv = 'AKTIV',
  Alle = 'ALLE',
  Inaktiv = 'INAKTIV'
}

export type Telefon = {
  __typename?: 'Telefon';
  beskrivelse?: Maybe<Scalars['String']['output']>;
  nummer: Scalars['String']['output'];
  type: TelefonType;
};

export enum TelefonType {
  NavKontorTelefon = 'NAV_KONTOR_TELEFON',
  NavTjenesteTelefon = 'NAV_TJENESTE_TELEFON',
  PrivatTelefon = 'PRIVAT_TELEFON'
}

export enum TilknytningType {
  Annet = 'ANNET',
  Arbeidstrening = 'ARBEIDSTRENING',
  EksternSamarbeidspartner = 'EKSTERN_SAMARBEIDSPARTNER',
  Fast = 'FAST',
  Helfo = 'HELFO',
  Ingen = 'INGEN',
  Jobbspesialist = 'JOBBSPESIALIST',
  Konsulent = 'KONSULENT',
  Laerling = 'LAERLING',
  Midlertidig = 'MIDLERTIDIG',
  Pensjonist = 'PENSJONIST',
  Praksisarbeid = 'PRAKSISARBEID',
  Riksrevisjonen = 'RIKSREVISJONEN',
  Skatteetaten = 'SKATTEETATEN',
  Statsforvalteren = 'STATSFORVALTEREN',
  Student = 'STUDENT',
  UforutsettBehov = 'UFORUTSETT_BEHOV',
  Utdanningsstillinger = 'UTDANNINGSSTILLINGER',
  Vikar = 'VIKAR'
}

export type SearchOrgenheterQueryVariables = Exact<{
  term: Scalars['String']['input'];
  filter?: InputMaybe<OrgEnhetSearchFilter>;
}>;


export type SearchOrgenheterQuery = { __typename?: 'Query', searchOrgEnhet: Array<{ __typename?: 'OrgEnhet', id: string, navn: string, remedyEnhetId?: string | null, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', visningsnavn?: string | null } }> }> };
