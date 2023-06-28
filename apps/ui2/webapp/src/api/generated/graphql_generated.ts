import { GraphQLResolveInfo, GraphQLScalarType, GraphQLScalarTypeConfig } from 'graphql';
import { gql } from '@apollo/client';
import * as Apollo from '@apollo/client';
export type Maybe<T> = T | null;
export type InputMaybe<T> = Maybe<T>;
export type Exact<T extends { [key: string]: unknown }> = { [K in keyof T]: T[K] };
export type MakeOptional<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]?: Maybe<T[SubKey]> };
export type MakeMaybe<T, K extends keyof T> = Omit<T, K> & { [SubKey in K]: Maybe<T[SubKey]> };
export type MakeEmpty<T extends { [key: string]: unknown }, K extends keyof T> = { [_ in K]?: never };
export type Incremental<T> = T | { [P in keyof T]?: P extends ' $fragmentName' | '__typename' ? T[P] : never };
export type RequireFields<T, K extends keyof T> = Omit<T, K> & { [P in K]-?: NonNullable<T[P]> };
const defaultOptions = {} as const;
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

export enum AnsettelsesType {
  Fast = 'FAST',
  Midlertidig = 'MIDLERTIDIG'
}

export type FolkeregisterPerson = {
  __typename?: 'FolkeregisterPerson';
  navn: Navn;
};

export enum GyldigTomSelection {
  Alle = 'ALLE',
  IkkePassert = 'IKKE_PASSERT',
  Passert = 'PASSERT'
}

export type Kode = {
  __typename?: 'Kode';
  gyldigFom?: Maybe<Scalars['Date']['output']>;
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  kode: Scalars['String']['output'];
  navn: Scalars['String']['output'];
};

export type LederOrgEnhet = {
  __typename?: 'LederOrgEnhet';
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  orgEnhet: OrgEnhet;
  /** @deprecated organisasjonsenhet er erstattet med orgEnhet */
  organisasjonsenhet: OrgEnhet;
};

export type Mutation = {
  __typename?: 'Mutation';
  opprettRessurs?: Maybe<OpprettRessursResponse>;
};


export type MutationOpprettRessursArgs = {
  request: OpprettRessursRequest;
};

export type Navn = {
  __typename?: 'Navn';
  etternavn: Scalars['String']['output'];
  fornavn: Scalars['String']['output'];
  mellomnavn?: Maybe<Scalars['String']['output']>;
};

export enum NomNivaa {
  Arbeidsomraade = 'ARBEIDSOMRAADE',
  Driftsenhet = 'DRIFTSENHET',
  Linjeenhet = 'LINJEENHET'
}

/**  Requests */
export type OpprettRessursRequest = {
  ansettelsesType: AnsettelsesType;
  orgEnhetId?: InputMaybe<Scalars['String']['input']>;
  personident: Scalars['String']['input'];
  sektor: Sektor;
  startDato?: InputMaybe<Scalars['Date']['input']>;
  visningsetternavn: Scalars['String']['input'];
  visningsfornavn: Scalars['String']['input'];
  visningsmellomnavn?: InputMaybe<Scalars['String']['input']>;
};

/**  Responses */
export type OpprettRessursResponse = {
  __typename?: 'OpprettRessursResponse';
  navident: Scalars['String']['output'];
  personident: Scalars['String']['output'];
  sektor: Sektor;
  visningsetternavn: Scalars['String']['output'];
  visningsfornavn: Scalars['String']['output'];
};

export type OrgEnhet = {
  __typename?: 'OrgEnhet';
  agressoId: Scalars['ID']['output'];
  agressoOrgenhetType?: Maybe<Scalars['String']['output']>;
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  id: Scalars['ID']['output'];
  koblinger: Array<OrgEnhetsKobling>;
  leder: Array<OrgEnhetsLeder>;
  navn: Scalars['String']['output'];
  nomNivaa?: Maybe<NomNivaa>;
  /** @deprecated orgEnhetsKategori er erstattet med nomNivaa */
  orgEnhetsKategori?: Maybe<NomNivaa>;
  orgEnhetsType?: Maybe<OrgEnhetsType>;
  orgNiv: Scalars['String']['output'];
  organiseringer: Array<Organisering>;
  remedyEnhetId?: Maybe<Scalars['String']['output']>;
  type?: Maybe<Kode>;
};


export type OrgEnhetOrganiseringerArgs = {
  retning?: InputMaybe<Retning>;
};

export type OrgEnhetResult = {
  __typename?: 'OrgEnhetResult';
  code: ResultCode;
  id: Scalars['String']['output'];
  nomId: Scalars['String']['output'];
  orgEnhet?: Maybe<OrgEnhet>;
  orgNiv?: Maybe<Scalars['String']['output']>;
  /** @deprecated organisasjonsenhet er erstattet med orgEnhet */
  organisasjonsenhet?: Maybe<OrgEnhet>;
};

export type OrgEnhetSearch = {
  agressoId?: InputMaybe<Scalars['String']['input']>;
  id?: InputMaybe<Scalars['String']['input']>;
  navn?: InputMaybe<Scalars['String']['input']>;
  orgNiv?: InputMaybe<Scalars['String']['input']>;
};

export type OrgEnhetSearchFilter = {
  gyldigTomSelection?: GyldigTomSelection;
  limit?: ResultLimit;
  orgnivSelection?: OrgnivSelection;
};

export type OrgEnheterSearch = {
  agressoIder?: InputMaybe<Array<Scalars['String']['input']>>;
  ider?: InputMaybe<Array<Scalars['String']['input']>>;
  nomNivaa?: InputMaybe<Array<NomNivaa>>;
  orgNiv?: InputMaybe<Scalars['String']['input']>;
};

export type OrgEnhetsKobling = {
  __typename?: 'OrgEnhetsKobling';
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  ressurs: Ressurs;
};

export type OrgEnhetsLeder = {
  __typename?: 'OrgEnhetsLeder';
  ressurs: Ressurs;
};

export enum OrgEnhetsType {
  Dir = 'DIR',
  Etat = 'ETAT'
}

export type Organisering = {
  __typename?: 'Organisering';
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  orgEnhet: OrgEnhet;
  /** @deprecated organisasjonsenhet er erstattet med orgEnhet */
  organisasjonsenhet: OrgEnhet;
  retning: Retning;
};

export enum OrgnivSelection {
  Alle = 'ALLE',
  IkkeOrgnivOrgenhet = 'IKKE_ORGNIV_ORGENHET',
  OrgnivOrgenhet = 'ORGNIV_ORGENHET'
}

export type PersonResult = {
  __typename?: 'PersonResult';
  folkeregisterPerson?: Maybe<FolkeregisterPerson>;
  /** @deprecated navIdent er erstattet med navident */
  navIdent?: Maybe<Scalars['String']['output']>;
  navident?: Maybe<Scalars['String']['output']>;
  /** @deprecated person er erstattet med folkeregisterPerson */
  person?: Maybe<FolkeregisterPerson>;
  /** @deprecated personIdent er erstattet med personident */
  personIdent: Scalars['String']['output'];
  personident: Scalars['String']['output'];
};

export type PersonSearch = {
  personident?: InputMaybe<Scalars['String']['input']>;
};

export type Query = {
  __typename?: 'Query';
  orgEnhet?: Maybe<OrgEnhet>;
  orgEnheter: Array<OrgEnhetResult>;
  /** @deprecated Bruk orgEnhet query med samme parametre */
  organisasjonsenhet?: Maybe<OrgEnhet>;
  /** @deprecated Bruk orgEnheter query med samme parametre */
  organisasjonsenheter: Array<OrgEnhetResult>;
  person?: Maybe<PersonResult>;
  ressurs?: Maybe<Ressurs>;
  ressurser: Array<RessursResult>;
  search: Array<SearchResult>;
  searchOrgEnhet: Array<OrgEnhet>;
  searchRessurs: Array<Ressurs>;
};


export type QueryOrgEnhetArgs = {
  where: OrgEnhetSearch;
};


export type QueryOrgEnheterArgs = {
  where?: InputMaybe<OrgEnheterSearch>;
};


export type QueryOrganisasjonsenhetArgs = {
  where: OrgEnhetSearch;
};


export type QueryOrganisasjonsenheterArgs = {
  where?: InputMaybe<OrgEnheterSearch>;
};


export type QueryPersonArgs = {
  where?: InputMaybe<PersonSearch>;
};


export type QueryRessursArgs = {
  where: RessursSearch;
};


export type QueryRessurserArgs = {
  where?: InputMaybe<RessurserSearch>;
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
  epost?: Maybe<Scalars['String']['output']>;
  etternavn?: Maybe<Scalars['String']['output']>;
  folkeregisterPerson?: Maybe<FolkeregisterPerson>;
  fornavn?: Maybe<Scalars['String']['output']>;
  /** @deprecated koblinger er erstatet med orgTilknytning */
  koblinger: Array<RessursOrgTilknytning>;
  lederFor: Array<LederOrgEnhet>;
  ledere: Array<RessursLeder>;
  /** @deprecated navIdent er erstattet med navident */
  navIdent: Scalars['String']['output'];
  navident: Scalars['String']['output'];
  orgTilknytning: Array<RessursOrgTilknytning>;
  /** @deprecated person er erstatet med folkeregisterPerson */
  person?: Maybe<FolkeregisterPerson>;
  /** @deprecated personIdent er erstattet med personident */
  personIdent: Scalars['String']['output'];
  personident: Scalars['String']['output'];
  /** @deprecated ressurstype er erstatet med sektor */
  ressurstype: Array<Sektor>;
  sektor: Array<Sektor>;
  /** @deprecated Dårlig datakvalitet, kontakt #nom for mer informasjon før bruk */
  sluttdato?: Maybe<Scalars['Date']['output']>;
  telefon: Array<Telefon>;
  /** @deprecated visningsNavn er erstattet med visningsnavn */
  visningsNavn?: Maybe<Scalars['String']['output']>;
  visningsnavn?: Maybe<Scalars['String']['output']>;
};

export type RessursLeder = {
  __typename?: 'RessursLeder';
  erDagligOppfolging?: Maybe<Scalars['Boolean']['output']>;
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  ressurs: Ressurs;
};

export type RessursOrgTilknytning = {
  __typename?: 'RessursOrgTilknytning';
  erDagligOppfolging?: Maybe<Scalars['Boolean']['output']>;
  gyldigFom: Scalars['Date']['output'];
  gyldigTom?: Maybe<Scalars['Date']['output']>;
  orgEnhet: OrgEnhet;
  /** @deprecated organisasjonsenhet er erstattet med orgEnhet */
  organisasjonsenhet: OrgEnhet;
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
};

export type RessursSearchFilter = {
  limit?: ResultLimit;
  sektorSelection?: SektorSelection;
  statusSelection?: StatusSelection;
};

export type RessurserSearch = {
  navidenter?: InputMaybe<Array<Scalars['String']['input']>>;
  personidenter?: InputMaybe<Array<Scalars['String']['input']>>;
};

export enum ResultCode {
  Error = 'ERROR',
  NotFound = 'NOT_FOUND',
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

export type OpprettRessursMutationVariables = Exact<{
  request: OpprettRessursRequest;
}>;


export type OpprettRessursMutation = { __typename?: 'Mutation', opprettRessurs?: { __typename?: 'OpprettRessursResponse', navident: string, personident: string, visningsfornavn: string, visningsetternavn: string, sektor: Sektor } | null };

export type HentAlleRessurserQueryVariables = Exact<{ [key: string]: never; }>;


export type HentAlleRessurserQuery = { __typename?: 'Query', ressurser: Array<{ __typename?: 'RessursResult', ressurs?: { __typename?: 'Ressurs', navident: string } | null }> };

export type HentRessursMedNavidentQueryVariables = Exact<{
  navident?: InputMaybe<Scalars['String']['input']>;
}>;


export type HentRessursMedNavidentQuery = { __typename?: 'Query', ressurs?: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, epost?: string | null, sektor: Array<Sektor>, visningsnavn?: string | null, sluttdato?: any | null, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }>, ledere: Array<{ __typename?: 'RessursLeder', erDagligOppfolging?: boolean | null, ressurs: { __typename?: 'Ressurs', navident: string, visningsnavn?: string | null, fornavn?: string | null, etternavn?: string | null, orgTilknytning: Array<{ __typename?: 'RessursOrgTilknytning', orgEnhet: { __typename?: 'OrgEnhet', navn: string, id: string } }> } }>, lederFor: Array<{ __typename?: 'LederOrgEnhet', orgEnhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, orgTilknytning: Array<{ __typename?: 'RessursOrgTilknytning', erDagligOppfolging?: boolean | null, orgEnhet: { __typename?: 'OrgEnhet', navn: string, id: string, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null } }> } }> } | null };

export type HentAlleOrganisasjonsenheterQueryVariables = Exact<{ [key: string]: never; }>;


export type HentAlleOrganisasjonsenheterQuery = { __typename?: 'Query', organisasjonsenheter: Array<{ __typename?: 'OrgEnhetResult', id: string, code: ResultCode, organisasjonsenhet?: { __typename?: 'OrgEnhet', navn: string, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, personident: string } }> } | null }> };

export type HentPersonQueryVariables = Exact<{
  personident: Scalars['String']['input'];
}>;


export type HentPersonQuery = { __typename?: 'Query', person?: { __typename?: 'PersonResult', navident?: string | null, personident: string, person?: { __typename?: 'FolkeregisterPerson', navn: { __typename?: 'Navn', fornavn: string, mellomnavn?: string | null, etternavn: string } } | null } | null };

export type SearchRessursQueryVariables = Exact<{
  term: Scalars['String']['input'];
}>;


export type SearchRessursQuery = { __typename?: 'Query', search: Array<{ __typename?: 'OrgEnhet' } | { __typename?: 'Ressurs', navident: string, visningsnavn?: string | null }> };

export type RessursStandaloneFragment = { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> };

export type RessursWithLederFragment = { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, ledere: Array<{ __typename?: 'RessursLeder', ressurs: { __typename?: 'Ressurs', navident: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> };

export type RessursWithKoblingFragment = { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> };

export type RessursWithLederAndKoblingFragment = { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, ledere: Array<{ __typename?: 'RessursLeder', ressurs: { __typename?: 'Ressurs', navident: string } }>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> };

export type OrgenhetStandaloneWithLederFragment = { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> };

export type HentHierarkiQueryVariables = Exact<{
  id: Scalars['String']['input'];
}>;


export type HentHierarkiQuery = { __typename?: 'Query', enhet?: { __typename?: 'OrgEnhet', id: string, remedyEnhetId?: string | null, navn: string, gyldigFom: any, gyldigTom?: any | null, type?: { __typename?: 'Kode', kode: string } | null, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, orgTilknytning: Array<{ __typename?: 'RessursOrgTilknytning', orgEnhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } | null, ressursKoblinger?: { __typename?: 'OrgEnhet', koblinger: Array<{ __typename?: 'OrgEnhetsKobling', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, ledere: Array<{ __typename?: 'RessursLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } | null, enhetsKoblinger?: { __typename?: 'OrgEnhet', underenheter: Array<{ __typename?: 'Organisering', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } }>, overenheter: Array<{ __typename?: 'Organisering', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, organiseringer: Array<{ __typename?: 'Organisering', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, organiseringer: Array<{ __typename?: 'Organisering', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, organiseringer: Array<{ __typename?: 'Organisering', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, organiseringer: Array<{ __typename?: 'Organisering', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, organiseringer: Array<{ __typename?: 'Organisering', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string, gyldigFom: any, gyldigTom?: any | null, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } }>, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } }>, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } }>, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } }>, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } }>, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }> } }> } | null };

export type HentLederFornavidentQueryVariables = Exact<{
  navident?: InputMaybe<Scalars['String']['input']>;
}>;


export type HentLederFornavidentQuery = { __typename?: 'Query', ressurs?: { __typename?: 'Ressurs', navident: string, ledere: Array<{ __typename?: 'RessursLeder', ressurs: { __typename?: 'Ressurs', navident: string, visningsnavn?: string | null, fornavn?: string | null, etternavn?: string | null, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', navn: string, id: string } }> } }> } | null };

export type HentRessursQueryVariables = Exact<{
  navident?: InputMaybe<Scalars['String']['input']>;
}>;


export type HentRessursQuery = { __typename?: 'Query', ressurs?: { __typename?: 'Ressurs', sektor: Array<Sektor>, navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, ledere: Array<{ __typename?: 'RessursLeder', ressurs: { __typename?: 'Ressurs', navident: string, fornavn?: string | null, etternavn?: string | null, visningsnavn?: string | null, epost?: string | null, sektor: Array<Sektor>, orgTilknytning: Array<{ __typename?: 'RessursOrgTilknytning', orgEnhet: { __typename?: 'OrgEnhet', navn: string, id: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } }>, koblinger: Array<{ __typename?: 'RessursOrgTilknytning', organisasjonsenhet: { __typename?: 'OrgEnhet', id: string, navn: string } }>, telefon: Array<{ __typename?: 'Telefon', beskrivelse?: string | null, nummer: string, type: TelefonType }> } | null };

export type SearchOrgenheterQueryVariables = Exact<{
  term: Scalars['String']['input'];
  filter?: InputMaybe<OrgEnhetSearchFilter>;
}>;


export type SearchOrgenheterQuery = { __typename?: 'Query', searchOrgEnhet: Array<{ __typename?: 'OrgEnhet', id: string, navn: string, remedyEnhetId?: string | null, leder: Array<{ __typename?: 'OrgEnhetsLeder', ressurs: { __typename?: 'Ressurs', visningsnavn?: string | null } }> }> };



export type ResolverTypeWrapper<T> = Promise<T> | T;


export type ResolverWithResolve<TResult, TParent, TContext, TArgs> = {
  resolve: ResolverFn<TResult, TParent, TContext, TArgs>;
};
export type Resolver<TResult, TParent = {}, TContext = {}, TArgs = {}> = ResolverFn<TResult, TParent, TContext, TArgs> | ResolverWithResolve<TResult, TParent, TContext, TArgs>;

export type ResolverFn<TResult, TParent, TContext, TArgs> = (
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => Promise<TResult> | TResult;

export type SubscriptionSubscribeFn<TResult, TParent, TContext, TArgs> = (
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => AsyncIterable<TResult> | Promise<AsyncIterable<TResult>>;

export type SubscriptionResolveFn<TResult, TParent, TContext, TArgs> = (
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => TResult | Promise<TResult>;

export interface SubscriptionSubscriberObject<TResult, TKey extends string, TParent, TContext, TArgs> {
  subscribe: SubscriptionSubscribeFn<{ [key in TKey]: TResult }, TParent, TContext, TArgs>;
  resolve?: SubscriptionResolveFn<TResult, { [key in TKey]: TResult }, TContext, TArgs>;
}

export interface SubscriptionResolverObject<TResult, TParent, TContext, TArgs> {
  subscribe: SubscriptionSubscribeFn<any, TParent, TContext, TArgs>;
  resolve: SubscriptionResolveFn<TResult, any, TContext, TArgs>;
}

export type SubscriptionObject<TResult, TKey extends string, TParent, TContext, TArgs> =
  | SubscriptionSubscriberObject<TResult, TKey, TParent, TContext, TArgs>
  | SubscriptionResolverObject<TResult, TParent, TContext, TArgs>;

export type SubscriptionResolver<TResult, TKey extends string, TParent = {}, TContext = {}, TArgs = {}> =
  | ((...args: any[]) => SubscriptionObject<TResult, TKey, TParent, TContext, TArgs>)
  | SubscriptionObject<TResult, TKey, TParent, TContext, TArgs>;

export type TypeResolveFn<TTypes, TParent = {}, TContext = {}> = (
  parent: TParent,
  context: TContext,
  info: GraphQLResolveInfo
) => Maybe<TTypes> | Promise<Maybe<TTypes>>;

export type IsTypeOfResolverFn<T = {}, TContext = {}> = (obj: T, context: TContext, info: GraphQLResolveInfo) => boolean | Promise<boolean>;

export type NextResolverFn<T> = () => Promise<T>;

export type DirectiveResolverFn<TResult = {}, TParent = {}, TContext = {}, TArgs = {}> = (
  next: NextResolverFn<TResult>,
  parent: TParent,
  args: TArgs,
  context: TContext,
  info: GraphQLResolveInfo
) => TResult | Promise<TResult>;

/** Mapping of union types */
export type ResolversUnionTypes<RefType extends Record<string, unknown>> = {
  SearchResult: ( OrgEnhet ) | ( Ressurs );
};


/** Mapping between all available schema types and the resolvers types */
export type ResolversTypes = {
  AnsettelsesType: AnsettelsesType;
  Boolean: ResolverTypeWrapper<Scalars['Boolean']['output']>;
  Date: ResolverTypeWrapper<Scalars['Date']['output']>;
  DateTime: ResolverTypeWrapper<Scalars['DateTime']['output']>;
  FolkeregisterPerson: ResolverTypeWrapper<FolkeregisterPerson>;
  GyldigTomSelection: GyldigTomSelection;
  ID: ResolverTypeWrapper<Scalars['ID']['output']>;
  JSON: ResolverTypeWrapper<Scalars['JSON']['output']>;
  Kode: ResolverTypeWrapper<Kode>;
  LederOrgEnhet: ResolverTypeWrapper<LederOrgEnhet>;
  Mutation: ResolverTypeWrapper<{}>;
  Navn: ResolverTypeWrapper<Navn>;
  NomNivaa: NomNivaa;
  OpprettRessursRequest: OpprettRessursRequest;
  OpprettRessursResponse: ResolverTypeWrapper<OpprettRessursResponse>;
  OrgEnhet: ResolverTypeWrapper<OrgEnhet>;
  OrgEnhetResult: ResolverTypeWrapper<OrgEnhetResult>;
  OrgEnhetSearch: OrgEnhetSearch;
  OrgEnhetSearchFilter: OrgEnhetSearchFilter;
  OrgEnheterSearch: OrgEnheterSearch;
  OrgEnhetsKobling: ResolverTypeWrapper<OrgEnhetsKobling>;
  OrgEnhetsLeder: ResolverTypeWrapper<OrgEnhetsLeder>;
  OrgEnhetsType: OrgEnhetsType;
  Organisering: ResolverTypeWrapper<Organisering>;
  OrgnivSelection: OrgnivSelection;
  PersonResult: ResolverTypeWrapper<PersonResult>;
  PersonSearch: PersonSearch;
  Query: ResolverTypeWrapper<{}>;
  Ressurs: ResolverTypeWrapper<Ressurs>;
  RessursLeder: ResolverTypeWrapper<RessursLeder>;
  RessursOrgTilknytning: ResolverTypeWrapper<RessursOrgTilknytning>;
  RessursResult: ResolverTypeWrapper<RessursResult>;
  RessursSearch: RessursSearch;
  RessursSearchFilter: RessursSearchFilter;
  RessurserSearch: RessurserSearch;
  ResultCode: ResultCode;
  ResultLimit: ResultLimit;
  Retning: Retning;
  SearchResult: ResolverTypeWrapper<ResolversUnionTypes<ResolversTypes>['SearchResult']>;
  Sektor: Sektor;
  SektorSelection: SektorSelection;
  StatusSelection: StatusSelection;
  String: ResolverTypeWrapper<Scalars['String']['output']>;
  Telefon: ResolverTypeWrapper<Telefon>;
  TelefonType: TelefonType;
};

/** Mapping between all available schema types and the resolvers parents */
export type ResolversParentTypes = {
  Boolean: Scalars['Boolean']['output'];
  Date: Scalars['Date']['output'];
  DateTime: Scalars['DateTime']['output'];
  FolkeregisterPerson: FolkeregisterPerson;
  ID: Scalars['ID']['output'];
  JSON: Scalars['JSON']['output'];
  Kode: Kode;
  LederOrgEnhet: LederOrgEnhet;
  Mutation: {};
  Navn: Navn;
  OpprettRessursRequest: OpprettRessursRequest;
  OpprettRessursResponse: OpprettRessursResponse;
  OrgEnhet: OrgEnhet;
  OrgEnhetResult: OrgEnhetResult;
  OrgEnhetSearch: OrgEnhetSearch;
  OrgEnhetSearchFilter: OrgEnhetSearchFilter;
  OrgEnheterSearch: OrgEnheterSearch;
  OrgEnhetsKobling: OrgEnhetsKobling;
  OrgEnhetsLeder: OrgEnhetsLeder;
  Organisering: Organisering;
  PersonResult: PersonResult;
  PersonSearch: PersonSearch;
  Query: {};
  Ressurs: Ressurs;
  RessursLeder: RessursLeder;
  RessursOrgTilknytning: RessursOrgTilknytning;
  RessursResult: RessursResult;
  RessursSearch: RessursSearch;
  RessursSearchFilter: RessursSearchFilter;
  RessurserSearch: RessurserSearch;
  SearchResult: ResolversUnionTypes<ResolversParentTypes>['SearchResult'];
  String: Scalars['String']['output'];
  Telefon: Telefon;
};

export interface DateScalarConfig extends GraphQLScalarTypeConfig<ResolversTypes['Date'], any> {
  name: 'Date';
}

export interface DateTimeScalarConfig extends GraphQLScalarTypeConfig<ResolversTypes['DateTime'], any> {
  name: 'DateTime';
}

export type FolkeregisterPersonResolvers<ContextType = any, ParentType extends ResolversParentTypes['FolkeregisterPerson'] = ResolversParentTypes['FolkeregisterPerson']> = {
  navn?: Resolver<ResolversTypes['Navn'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export interface JsonScalarConfig extends GraphQLScalarTypeConfig<ResolversTypes['JSON'], any> {
  name: 'JSON';
}

export type KodeResolvers<ContextType = any, ParentType extends ResolversParentTypes['Kode'] = ResolversParentTypes['Kode']> = {
  gyldigFom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  gyldigTom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  kode?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  navn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type LederOrgEnhetResolvers<ContextType = any, ParentType extends ResolversParentTypes['LederOrgEnhet'] = ResolversParentTypes['LederOrgEnhet']> = {
  gyldigFom?: Resolver<ResolversTypes['Date'], ParentType, ContextType>;
  gyldigTom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  orgEnhet?: Resolver<ResolversTypes['OrgEnhet'], ParentType, ContextType>;
  organisasjonsenhet?: Resolver<ResolversTypes['OrgEnhet'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type MutationResolvers<ContextType = any, ParentType extends ResolversParentTypes['Mutation'] = ResolversParentTypes['Mutation']> = {
  opprettRessurs?: Resolver<Maybe<ResolversTypes['OpprettRessursResponse']>, ParentType, ContextType, RequireFields<MutationOpprettRessursArgs, 'request'>>;
};

export type NavnResolvers<ContextType = any, ParentType extends ResolversParentTypes['Navn'] = ResolversParentTypes['Navn']> = {
  etternavn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  fornavn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  mellomnavn?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type OpprettRessursResponseResolvers<ContextType = any, ParentType extends ResolversParentTypes['OpprettRessursResponse'] = ResolversParentTypes['OpprettRessursResponse']> = {
  navident?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  personident?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  sektor?: Resolver<ResolversTypes['Sektor'], ParentType, ContextType>;
  visningsetternavn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  visningsfornavn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type OrgEnhetResolvers<ContextType = any, ParentType extends ResolversParentTypes['OrgEnhet'] = ResolversParentTypes['OrgEnhet']> = {
  agressoId?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  agressoOrgenhetType?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  gyldigFom?: Resolver<ResolversTypes['Date'], ParentType, ContextType>;
  gyldigTom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  id?: Resolver<ResolversTypes['ID'], ParentType, ContextType>;
  koblinger?: Resolver<Array<ResolversTypes['OrgEnhetsKobling']>, ParentType, ContextType>;
  leder?: Resolver<Array<ResolversTypes['OrgEnhetsLeder']>, ParentType, ContextType>;
  navn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  nomNivaa?: Resolver<Maybe<ResolversTypes['NomNivaa']>, ParentType, ContextType>;
  orgEnhetsKategori?: Resolver<Maybe<ResolversTypes['NomNivaa']>, ParentType, ContextType>;
  orgEnhetsType?: Resolver<Maybe<ResolversTypes['OrgEnhetsType']>, ParentType, ContextType>;
  orgNiv?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  organiseringer?: Resolver<Array<ResolversTypes['Organisering']>, ParentType, ContextType, Partial<OrgEnhetOrganiseringerArgs>>;
  remedyEnhetId?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  type?: Resolver<Maybe<ResolversTypes['Kode']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type OrgEnhetResultResolvers<ContextType = any, ParentType extends ResolversParentTypes['OrgEnhetResult'] = ResolversParentTypes['OrgEnhetResult']> = {
  code?: Resolver<ResolversTypes['ResultCode'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  nomId?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  orgEnhet?: Resolver<Maybe<ResolversTypes['OrgEnhet']>, ParentType, ContextType>;
  orgNiv?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  organisasjonsenhet?: Resolver<Maybe<ResolversTypes['OrgEnhet']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type OrgEnhetsKoblingResolvers<ContextType = any, ParentType extends ResolversParentTypes['OrgEnhetsKobling'] = ResolversParentTypes['OrgEnhetsKobling']> = {
  gyldigFom?: Resolver<ResolversTypes['Date'], ParentType, ContextType>;
  gyldigTom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  ressurs?: Resolver<ResolversTypes['Ressurs'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type OrgEnhetsLederResolvers<ContextType = any, ParentType extends ResolversParentTypes['OrgEnhetsLeder'] = ResolversParentTypes['OrgEnhetsLeder']> = {
  ressurs?: Resolver<ResolversTypes['Ressurs'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type OrganiseringResolvers<ContextType = any, ParentType extends ResolversParentTypes['Organisering'] = ResolversParentTypes['Organisering']> = {
  gyldigFom?: Resolver<ResolversTypes['Date'], ParentType, ContextType>;
  gyldigTom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  orgEnhet?: Resolver<ResolversTypes['OrgEnhet'], ParentType, ContextType>;
  organisasjonsenhet?: Resolver<ResolversTypes['OrgEnhet'], ParentType, ContextType>;
  retning?: Resolver<ResolversTypes['Retning'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type PersonResultResolvers<ContextType = any, ParentType extends ResolversParentTypes['PersonResult'] = ResolversParentTypes['PersonResult']> = {
  folkeregisterPerson?: Resolver<Maybe<ResolversTypes['FolkeregisterPerson']>, ParentType, ContextType>;
  navIdent?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  navident?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  person?: Resolver<Maybe<ResolversTypes['FolkeregisterPerson']>, ParentType, ContextType>;
  personIdent?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  personident?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type QueryResolvers<ContextType = any, ParentType extends ResolversParentTypes['Query'] = ResolversParentTypes['Query']> = {
  orgEnhet?: Resolver<Maybe<ResolversTypes['OrgEnhet']>, ParentType, ContextType, RequireFields<QueryOrgEnhetArgs, 'where'>>;
  orgEnheter?: Resolver<Array<ResolversTypes['OrgEnhetResult']>, ParentType, ContextType, Partial<QueryOrgEnheterArgs>>;
  organisasjonsenhet?: Resolver<Maybe<ResolversTypes['OrgEnhet']>, ParentType, ContextType, RequireFields<QueryOrganisasjonsenhetArgs, 'where'>>;
  organisasjonsenheter?: Resolver<Array<ResolversTypes['OrgEnhetResult']>, ParentType, ContextType, Partial<QueryOrganisasjonsenheterArgs>>;
  person?: Resolver<Maybe<ResolversTypes['PersonResult']>, ParentType, ContextType, Partial<QueryPersonArgs>>;
  ressurs?: Resolver<Maybe<ResolversTypes['Ressurs']>, ParentType, ContextType, RequireFields<QueryRessursArgs, 'where'>>;
  ressurser?: Resolver<Array<ResolversTypes['RessursResult']>, ParentType, ContextType, Partial<QueryRessurserArgs>>;
  search?: Resolver<Array<ResolversTypes['SearchResult']>, ParentType, ContextType, RequireFields<QuerySearchArgs, 'term'>>;
  searchOrgEnhet?: Resolver<Array<ResolversTypes['OrgEnhet']>, ParentType, ContextType, RequireFields<QuerySearchOrgEnhetArgs, 'term'>>;
  searchRessurs?: Resolver<Array<ResolversTypes['Ressurs']>, ParentType, ContextType, RequireFields<QuerySearchRessursArgs, 'term'>>;
};

export type RessursResolvers<ContextType = any, ParentType extends ResolversParentTypes['Ressurs'] = ResolversParentTypes['Ressurs']> = {
  epost?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  etternavn?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  folkeregisterPerson?: Resolver<Maybe<ResolversTypes['FolkeregisterPerson']>, ParentType, ContextType>;
  fornavn?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  koblinger?: Resolver<Array<ResolversTypes['RessursOrgTilknytning']>, ParentType, ContextType>;
  lederFor?: Resolver<Array<ResolversTypes['LederOrgEnhet']>, ParentType, ContextType>;
  ledere?: Resolver<Array<ResolversTypes['RessursLeder']>, ParentType, ContextType>;
  navIdent?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  navident?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  orgTilknytning?: Resolver<Array<ResolversTypes['RessursOrgTilknytning']>, ParentType, ContextType>;
  person?: Resolver<Maybe<ResolversTypes['FolkeregisterPerson']>, ParentType, ContextType>;
  personIdent?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  personident?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  ressurstype?: Resolver<Array<ResolversTypes['Sektor']>, ParentType, ContextType>;
  sektor?: Resolver<Array<ResolversTypes['Sektor']>, ParentType, ContextType>;
  sluttdato?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  telefon?: Resolver<Array<ResolversTypes['Telefon']>, ParentType, ContextType>;
  visningsNavn?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  visningsnavn?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type RessursLederResolvers<ContextType = any, ParentType extends ResolversParentTypes['RessursLeder'] = ResolversParentTypes['RessursLeder']> = {
  erDagligOppfolging?: Resolver<Maybe<ResolversTypes['Boolean']>, ParentType, ContextType>;
  gyldigFom?: Resolver<ResolversTypes['Date'], ParentType, ContextType>;
  gyldigTom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  ressurs?: Resolver<ResolversTypes['Ressurs'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type RessursOrgTilknytningResolvers<ContextType = any, ParentType extends ResolversParentTypes['RessursOrgTilknytning'] = ResolversParentTypes['RessursOrgTilknytning']> = {
  erDagligOppfolging?: Resolver<Maybe<ResolversTypes['Boolean']>, ParentType, ContextType>;
  gyldigFom?: Resolver<ResolversTypes['Date'], ParentType, ContextType>;
  gyldigTom?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  orgEnhet?: Resolver<ResolversTypes['OrgEnhet'], ParentType, ContextType>;
  organisasjonsenhet?: Resolver<ResolversTypes['OrgEnhet'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type RessursResultResolvers<ContextType = any, ParentType extends ResolversParentTypes['RessursResult'] = ResolversParentTypes['RessursResult']> = {
  code?: Resolver<ResolversTypes['ResultCode'], ParentType, ContextType>;
  id?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  ressurs?: Resolver<Maybe<ResolversTypes['Ressurs']>, ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type SearchResultResolvers<ContextType = any, ParentType extends ResolversParentTypes['SearchResult'] = ResolversParentTypes['SearchResult']> = {
  __resolveType: TypeResolveFn<'OrgEnhet' | 'Ressurs', ParentType, ContextType>;
};

export type TelefonResolvers<ContextType = any, ParentType extends ResolversParentTypes['Telefon'] = ResolversParentTypes['Telefon']> = {
  beskrivelse?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  nummer?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  type?: Resolver<ResolversTypes['TelefonType'], ParentType, ContextType>;
  __isTypeOf?: IsTypeOfResolverFn<ParentType, ContextType>;
};

export type Resolvers<ContextType = any> = {
  Date?: GraphQLScalarType;
  DateTime?: GraphQLScalarType;
  FolkeregisterPerson?: FolkeregisterPersonResolvers<ContextType>;
  JSON?: GraphQLScalarType;
  Kode?: KodeResolvers<ContextType>;
  LederOrgEnhet?: LederOrgEnhetResolvers<ContextType>;
  Mutation?: MutationResolvers<ContextType>;
  Navn?: NavnResolvers<ContextType>;
  OpprettRessursResponse?: OpprettRessursResponseResolvers<ContextType>;
  OrgEnhet?: OrgEnhetResolvers<ContextType>;
  OrgEnhetResult?: OrgEnhetResultResolvers<ContextType>;
  OrgEnhetsKobling?: OrgEnhetsKoblingResolvers<ContextType>;
  OrgEnhetsLeder?: OrgEnhetsLederResolvers<ContextType>;
  Organisering?: OrganiseringResolvers<ContextType>;
  PersonResult?: PersonResultResolvers<ContextType>;
  Query?: QueryResolvers<ContextType>;
  Ressurs?: RessursResolvers<ContextType>;
  RessursLeder?: RessursLederResolvers<ContextType>;
  RessursOrgTilknytning?: RessursOrgTilknytningResolvers<ContextType>;
  RessursResult?: RessursResultResolvers<ContextType>;
  SearchResult?: SearchResultResolvers<ContextType>;
  Telefon?: TelefonResolvers<ContextType>;
};


export const RessursStandaloneFragmentDoc = gql`
    fragment RessursStandalone on Ressurs {
  navident
  fornavn
  etternavn
  visningsnavn
  epost
  sektor
  telefon {
    beskrivelse
    nummer
    type
  }
}
    `;
export const RessursWithLederFragmentDoc = gql`
    fragment RessursWithLeder on Ressurs {
  ...RessursStandalone
  ledere {
    ressurs {
      navident
    }
  }
}
    ${RessursStandaloneFragmentDoc}`;
export const RessursWithKoblingFragmentDoc = gql`
    fragment RessursWithKobling on Ressurs {
  ...RessursStandalone
  koblinger {
    organisasjonsenhet {
      id
      navn
    }
  }
}
    ${RessursStandaloneFragmentDoc}`;
export const RessursWithLederAndKoblingFragmentDoc = gql`
    fragment RessursWithLederAndKobling on Ressurs {
  ...RessursWithLeder
  ...RessursWithKobling
}
    ${RessursWithLederFragmentDoc}
${RessursWithKoblingFragmentDoc}`;
export const OrgenhetStandaloneWithLederFragmentDoc = gql`
    fragment OrgenhetStandaloneWithLeder on OrgEnhet {
  id
  navn
  gyldigFom
  gyldigTom
  leder {
    ressurs {
      ...RessursStandalone
      koblinger {
        organisasjonsenhet {
          id
          navn
        }
      }
    }
  }
}
    ${RessursStandaloneFragmentDoc}`;
export const OpprettRessursDocument = gql`
    mutation opprettRessurs($request: OpprettRessursRequest!) {
  opprettRessurs(request: $request) {
    navident
    personident
    visningsfornavn
    visningsetternavn
    sektor
  }
}
    `;
export type OpprettRessursMutationFn = Apollo.MutationFunction<OpprettRessursMutation, OpprettRessursMutationVariables>;

/**
 * __useOpprettRessursMutation__
 *
 * To run a mutation, you first call `useOpprettRessursMutation` within a React component and pass it any options that fit your needs.
 * When your component renders, `useOpprettRessursMutation` returns a tuple that includes:
 * - A mutate function that you can call at any time to execute the mutation
 * - An object with fields that represent the current status of the mutation's execution
 *
 * @param baseOptions options that will be passed into the mutation, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options-2;
 *
 * @example
 * const [opprettRessursMutation, { data, loading, error }] = useOpprettRessursMutation({
 *   variables: {
 *      request: // value for 'request'
 *   },
 * });
 */
export function useOpprettRessursMutation(baseOptions?: Apollo.MutationHookOptions<OpprettRessursMutation, OpprettRessursMutationVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useMutation<OpprettRessursMutation, OpprettRessursMutationVariables>(OpprettRessursDocument, options);
      }
export type OpprettRessursMutationHookResult = ReturnType<typeof useOpprettRessursMutation>;
export type OpprettRessursMutationResult = Apollo.MutationResult<OpprettRessursMutation>;
export type OpprettRessursMutationOptions = Apollo.BaseMutationOptions<OpprettRessursMutation, OpprettRessursMutationVariables>;
export const HentAlleRessurserDocument = gql`
    query HentAlleRessurser {
  ressurser(where: {}) {
    ressurs {
      navident
    }
  }
}
    `;

/**
 * __useHentAlleRessurserQuery__
 *
 * To run a query within a React component, call `useHentAlleRessurserQuery` and pass it any options that fit your needs.
 * When your component renders, `useHentAlleRessurserQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useHentAlleRessurserQuery({
 *   variables: {
 *   },
 * });
 */
export function useHentAlleRessurserQuery(baseOptions?: Apollo.QueryHookOptions<HentAlleRessurserQuery, HentAlleRessurserQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<HentAlleRessurserQuery, HentAlleRessurserQueryVariables>(HentAlleRessurserDocument, options);
      }
export function useHentAlleRessurserLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<HentAlleRessurserQuery, HentAlleRessurserQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<HentAlleRessurserQuery, HentAlleRessurserQueryVariables>(HentAlleRessurserDocument, options);
        }
export type HentAlleRessurserQueryHookResult = ReturnType<typeof useHentAlleRessurserQuery>;
export type HentAlleRessurserLazyQueryHookResult = ReturnType<typeof useHentAlleRessurserLazyQuery>;
export type HentAlleRessurserQueryResult = Apollo.QueryResult<HentAlleRessurserQuery, HentAlleRessurserQueryVariables>;
export const HentRessursMedNavidentDocument = gql`
    query HentRessursMedNavident($navident: String) {
  ressurs(where: {navident: $navident}) {
    navident
    fornavn
    etternavn
    epost
    sektor
    visningsnavn
    sluttdato
    telefon {
      beskrivelse
      nummer
      type
    }
    ledere {
      erDagligOppfolging
      ressurs {
        navident
        visningsnavn
        fornavn
        etternavn
        orgTilknytning {
          orgEnhet {
            navn
            id
          }
        }
      }
    }
    lederFor {
      orgEnhet {
        id
        navn
      }
    }
    orgTilknytning {
      erDagligOppfolging
      orgEnhet {
        navn
        id
        leder {
          ressurs {
            navident
            fornavn
            etternavn
          }
        }
      }
    }
  }
}
    `;

/**
 * __useHentRessursMedNavidentQuery__
 *
 * To run a query within a React component, call `useHentRessursMedNavidentQuery` and pass it any options that fit your needs.
 * When your component renders, `useHentRessursMedNavidentQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useHentRessursMedNavidentQuery({
 *   variables: {
 *      navident: // value for 'navident'
 *   },
 * });
 */
export function useHentRessursMedNavidentQuery(baseOptions?: Apollo.QueryHookOptions<HentRessursMedNavidentQuery, HentRessursMedNavidentQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<HentRessursMedNavidentQuery, HentRessursMedNavidentQueryVariables>(HentRessursMedNavidentDocument, options);
      }
export function useHentRessursMedNavidentLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<HentRessursMedNavidentQuery, HentRessursMedNavidentQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<HentRessursMedNavidentQuery, HentRessursMedNavidentQueryVariables>(HentRessursMedNavidentDocument, options);
        }
export type HentRessursMedNavidentQueryHookResult = ReturnType<typeof useHentRessursMedNavidentQuery>;
export type HentRessursMedNavidentLazyQueryHookResult = ReturnType<typeof useHentRessursMedNavidentLazyQuery>;
export type HentRessursMedNavidentQueryResult = Apollo.QueryResult<HentRessursMedNavidentQuery, HentRessursMedNavidentQueryVariables>;
export const HentAlleOrganisasjonsenheterDocument = gql`
    query HentAlleOrganisasjonsenheter {
  organisasjonsenheter(where: {}) {
    id
    organisasjonsenhet {
      navn
      leder {
        ressurs {
          navident
          personident
        }
      }
    }
    code
  }
}
    `;

/**
 * __useHentAlleOrganisasjonsenheterQuery__
 *
 * To run a query within a React component, call `useHentAlleOrganisasjonsenheterQuery` and pass it any options that fit your needs.
 * When your component renders, `useHentAlleOrganisasjonsenheterQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useHentAlleOrganisasjonsenheterQuery({
 *   variables: {
 *   },
 * });
 */
export function useHentAlleOrganisasjonsenheterQuery(baseOptions?: Apollo.QueryHookOptions<HentAlleOrganisasjonsenheterQuery, HentAlleOrganisasjonsenheterQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<HentAlleOrganisasjonsenheterQuery, HentAlleOrganisasjonsenheterQueryVariables>(HentAlleOrganisasjonsenheterDocument, options);
      }
export function useHentAlleOrganisasjonsenheterLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<HentAlleOrganisasjonsenheterQuery, HentAlleOrganisasjonsenheterQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<HentAlleOrganisasjonsenheterQuery, HentAlleOrganisasjonsenheterQueryVariables>(HentAlleOrganisasjonsenheterDocument, options);
        }
export type HentAlleOrganisasjonsenheterQueryHookResult = ReturnType<typeof useHentAlleOrganisasjonsenheterQuery>;
export type HentAlleOrganisasjonsenheterLazyQueryHookResult = ReturnType<typeof useHentAlleOrganisasjonsenheterLazyQuery>;
export type HentAlleOrganisasjonsenheterQueryResult = Apollo.QueryResult<HentAlleOrganisasjonsenheterQuery, HentAlleOrganisasjonsenheterQueryVariables>;
export const HentPersonDocument = gql`
    query hentPerson($personident: String!) {
  person(where: {personident: $personident}) {
    navident
    personident
    person {
      navn {
        fornavn
        mellomnavn
        etternavn
      }
    }
  }
}
    `;

/**
 * __useHentPersonQuery__
 *
 * To run a query within a React component, call `useHentPersonQuery` and pass it any options that fit your needs.
 * When your component renders, `useHentPersonQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useHentPersonQuery({
 *   variables: {
 *      personident: // value for 'personident'
 *   },
 * });
 */
export function useHentPersonQuery(baseOptions: Apollo.QueryHookOptions<HentPersonQuery, HentPersonQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<HentPersonQuery, HentPersonQueryVariables>(HentPersonDocument, options);
      }
export function useHentPersonLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<HentPersonQuery, HentPersonQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<HentPersonQuery, HentPersonQueryVariables>(HentPersonDocument, options);
        }
export type HentPersonQueryHookResult = ReturnType<typeof useHentPersonQuery>;
export type HentPersonLazyQueryHookResult = ReturnType<typeof useHentPersonLazyQuery>;
export type HentPersonQueryResult = Apollo.QueryResult<HentPersonQuery, HentPersonQueryVariables>;
export const SearchRessursDocument = gql`
    query searchRessurs($term: String!) {
  search(term: $term) {
    ... on Ressurs {
      navident
      visningsnavn
    }
  }
}
    `;

/**
 * __useSearchRessursQuery__
 *
 * To run a query within a React component, call `useSearchRessursQuery` and pass it any options that fit your needs.
 * When your component renders, `useSearchRessursQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useSearchRessursQuery({
 *   variables: {
 *      term: // value for 'term'
 *   },
 * });
 */
export function useSearchRessursQuery(baseOptions: Apollo.QueryHookOptions<SearchRessursQuery, SearchRessursQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<SearchRessursQuery, SearchRessursQueryVariables>(SearchRessursDocument, options);
      }
export function useSearchRessursLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<SearchRessursQuery, SearchRessursQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<SearchRessursQuery, SearchRessursQueryVariables>(SearchRessursDocument, options);
        }
export type SearchRessursQueryHookResult = ReturnType<typeof useSearchRessursQuery>;
export type SearchRessursLazyQueryHookResult = ReturnType<typeof useSearchRessursLazyQuery>;
export type SearchRessursQueryResult = Apollo.QueryResult<SearchRessursQuery, SearchRessursQueryVariables>;
export const HentHierarkiDocument = gql`
    query HentHierarki($id: String!) {
  enhet: orgEnhet(where: {id: $id}) {
    id
    remedyEnhetId
    navn
    gyldigFom
    gyldigTom
    type {
      kode
    }
    leder {
      ressurs {
        ...RessursStandalone
        orgTilknytning {
          orgEnhet {
            id
            navn
          }
        }
      }
    }
  }
  ressursKoblinger: organisasjonsenhet(where: {id: $id}) {
    koblinger {
      ressurs {
        ...RessursStandalone
        ledere {
          ressurs {
            ...RessursStandalone
          }
        }
      }
    }
  }
  enhetsKoblinger: organisasjonsenhet(where: {id: $id}) {
    underenheter: organiseringer(retning: under) {
      organisasjonsenhet {
        ...OrgenhetStandaloneWithLeder
      }
    }
    overenheter: organiseringer(retning: over) {
      organisasjonsenhet {
        ...OrgenhetStandaloneWithLeder
        organiseringer(retning: over) {
          organisasjonsenhet {
            ...OrgenhetStandaloneWithLeder
            organiseringer(retning: over) {
              organisasjonsenhet {
                ...OrgenhetStandaloneWithLeder
                organiseringer(retning: over) {
                  organisasjonsenhet {
                    ...OrgenhetStandaloneWithLeder
                    organiseringer(retning: over) {
                      organisasjonsenhet {
                        ...OrgenhetStandaloneWithLeder
                        organiseringer(retning: over) {
                          organisasjonsenhet {
                            ...OrgenhetStandaloneWithLeder
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}
    ${RessursStandaloneFragmentDoc}
${OrgenhetStandaloneWithLederFragmentDoc}`;

/**
 * __useHentHierarkiQuery__
 *
 * To run a query within a React component, call `useHentHierarkiQuery` and pass it any options that fit your needs.
 * When your component renders, `useHentHierarkiQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useHentHierarkiQuery({
 *   variables: {
 *      id: // value for 'id'
 *   },
 * });
 */
export function useHentHierarkiQuery(baseOptions: Apollo.QueryHookOptions<HentHierarkiQuery, HentHierarkiQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<HentHierarkiQuery, HentHierarkiQueryVariables>(HentHierarkiDocument, options);
      }
export function useHentHierarkiLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<HentHierarkiQuery, HentHierarkiQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<HentHierarkiQuery, HentHierarkiQueryVariables>(HentHierarkiDocument, options);
        }
export type HentHierarkiQueryHookResult = ReturnType<typeof useHentHierarkiQuery>;
export type HentHierarkiLazyQueryHookResult = ReturnType<typeof useHentHierarkiLazyQuery>;
export type HentHierarkiQueryResult = Apollo.QueryResult<HentHierarkiQuery, HentHierarkiQueryVariables>;
export const HentLederFornavidentDocument = gql`
    query HentLederFornavident($navident: String) {
  ressurs(where: {navident: $navident}) {
    navident
    ledere {
      ressurs {
        navident
        visningsnavn
        fornavn
        etternavn
        koblinger {
          organisasjonsenhet {
            navn
            id
          }
        }
      }
    }
  }
}
    `;

/**
 * __useHentLederFornavidentQuery__
 *
 * To run a query within a React component, call `useHentLederFornavidentQuery` and pass it any options that fit your needs.
 * When your component renders, `useHentLederFornavidentQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useHentLederFornavidentQuery({
 *   variables: {
 *      navident: // value for 'navident'
 *   },
 * });
 */
export function useHentLederFornavidentQuery(baseOptions?: Apollo.QueryHookOptions<HentLederFornavidentQuery, HentLederFornavidentQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<HentLederFornavidentQuery, HentLederFornavidentQueryVariables>(HentLederFornavidentDocument, options);
      }
export function useHentLederFornavidentLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<HentLederFornavidentQuery, HentLederFornavidentQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<HentLederFornavidentQuery, HentLederFornavidentQueryVariables>(HentLederFornavidentDocument, options);
        }
export type HentLederFornavidentQueryHookResult = ReturnType<typeof useHentLederFornavidentQuery>;
export type HentLederFornavidentLazyQueryHookResult = ReturnType<typeof useHentLederFornavidentLazyQuery>;
export type HentLederFornavidentQueryResult = Apollo.QueryResult<HentLederFornavidentQuery, HentLederFornavidentQueryVariables>;
export const HentRessursDocument = gql`
    query HentRessurs($navident: String) {
  ressurs(where: {navident: $navident}) {
    ...RessursWithKobling
    sektor
    ledere {
      ressurs {
        ...RessursStandalone
        orgTilknytning {
          orgEnhet {
            navn
            id
          }
        }
      }
    }
  }
}
    ${RessursWithKoblingFragmentDoc}
${RessursStandaloneFragmentDoc}`;

/**
 * __useHentRessursQuery__
 *
 * To run a query within a React component, call `useHentRessursQuery` and pass it any options that fit your needs.
 * When your component renders, `useHentRessursQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useHentRessursQuery({
 *   variables: {
 *      navident: // value for 'navident'
 *   },
 * });
 */
export function useHentRessursQuery(baseOptions?: Apollo.QueryHookOptions<HentRessursQuery, HentRessursQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<HentRessursQuery, HentRessursQueryVariables>(HentRessursDocument, options);
      }
export function useHentRessursLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<HentRessursQuery, HentRessursQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<HentRessursQuery, HentRessursQueryVariables>(HentRessursDocument, options);
        }
export type HentRessursQueryHookResult = ReturnType<typeof useHentRessursQuery>;
export type HentRessursLazyQueryHookResult = ReturnType<typeof useHentRessursLazyQuery>;
export type HentRessursQueryResult = Apollo.QueryResult<HentRessursQuery, HentRessursQueryVariables>;
export const SearchOrgenheterDocument = gql`
    query SearchOrgenheter($term: String!, $filter: OrgEnhetSearchFilter) {
  searchOrgEnhet(term: $term, filter: $filter) {
    id
    navn
    remedyEnhetId
    leder {
      ressurs {
        visningsnavn
      }
    }
  }
}
    `;

/**
 * __useSearchOrgenheterQuery__
 *
 * To run a query within a React component, call `useSearchOrgenheterQuery` and pass it any options that fit your needs.
 * When your component renders, `useSearchOrgenheterQuery` returns an object from Apollo Client that contains loading, error, and data properties
 * you can use to render your UI.
 *
 * @param baseOptions options that will be passed into the query, supported options are listed on: https://www.apollographql.com/docs/react/api/react-hooks/#options;
 *
 * @example
 * const { data, loading, error } = useSearchOrgenheterQuery({
 *   variables: {
 *      term: // value for 'term'
 *      filter: // value for 'filter'
 *   },
 * });
 */
export function useSearchOrgenheterQuery(baseOptions: Apollo.QueryHookOptions<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>(SearchOrgenheterDocument, options);
      }
export function useSearchOrgenheterLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>(SearchOrgenheterDocument, options);
        }
export type SearchOrgenheterQueryHookResult = ReturnType<typeof useSearchOrgenheterQuery>;
export type SearchOrgenheterLazyQueryHookResult = ReturnType<typeof useSearchOrgenheterLazyQuery>;
export type SearchOrgenheterQueryResult = Apollo.QueryResult<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>;