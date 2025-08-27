import { GraphQLResolveInfo, GraphQLScalarType, GraphQLScalarTypeConfig } from 'graphql';
import { gql } from '@apollo/client';
import * as Apollo from '@apollo/client/react';
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
  /** @deprecated Ubrukt felt */
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
  Arbeidslivssenter = 'ARBEIDSLIVSSENTER',
  Arbeidsraadgivning = 'ARBEIDSRAADGIVNING',
  Dir = 'DIR',
  Direktorat = 'DIREKTORAT',
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
  ressurserWithMetadata: Array<RessursWithMetadataResult>;
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
  startdato?: Maybe<Scalars['Date']['output']>;
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
  telefonNummer?: InputMaybe<Scalars['String']['input']>;
};

export type RessursSearchFilter = {
  limit?: ResultLimit;
  sektorSelection?: SektorSelection;
  statusSelection?: StatusSelection;
};

export type RessursWithMetadataResult = {
  __typename?: 'RessursWithMetadataResult';
  code: ResultCode;
  endretAv?: Maybe<Scalars['String']['output']>;
  endretDato?: Maybe<Scalars['Date']['output']>;
  endringEvent?: Maybe<Scalars['String']['output']>;
  id: Scalars['String']['output'];
  opprettetAv?: Maybe<Scalars['String']['output']>;
  opprettetDato?: Maybe<Scalars['Date']['output']>;
  ressurs?: Maybe<Ressurs>;
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
  Navn: ResolverTypeWrapper<Navn>;
  NomNivaa: NomNivaa;
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
  RessursWithMetadataResult: ResolverTypeWrapper<RessursWithMetadataResult>;
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
  Navn: Navn;
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
  RessursWithMetadataResult: RessursWithMetadataResult;
  RessurserSearch: RessurserSearch;
  SearchResult: ResolversUnionTypes<ResolversParentTypes>['SearchResult'];
  String: Scalars['String']['output'];
  Telefon: Telefon;
};

export type OneOfDirectiveArgs = { };

export type OneOfDirectiveResolver<Result, Parent, ContextType = any, Args = OneOfDirectiveArgs> = DirectiveResolverFn<Result, Parent, ContextType, Args>;

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

export type NavnResolvers<ContextType = any, ParentType extends ResolversParentTypes['Navn'] = ResolversParentTypes['Navn']> = {
  etternavn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  fornavn?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  mellomnavn?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
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
  ressurserWithMetadata?: Resolver<Array<ResolversTypes['RessursWithMetadataResult']>, ParentType, ContextType>;
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
  startdato?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
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

export type RessursWithMetadataResultResolvers<ContextType = any, ParentType extends ResolversParentTypes['RessursWithMetadataResult'] = ResolversParentTypes['RessursWithMetadataResult']> = {
  code?: Resolver<ResolversTypes['ResultCode'], ParentType, ContextType>;
  endretAv?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  endretDato?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
  endringEvent?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  id?: Resolver<ResolversTypes['String'], ParentType, ContextType>;
  opprettetAv?: Resolver<Maybe<ResolversTypes['String']>, ParentType, ContextType>;
  opprettetDato?: Resolver<Maybe<ResolversTypes['Date']>, ParentType, ContextType>;
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
  Navn?: NavnResolvers<ContextType>;
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
  RessursWithMetadataResult?: RessursWithMetadataResultResolvers<ContextType>;
  SearchResult?: SearchResultResolvers<ContextType>;
  Telefon?: TelefonResolvers<ContextType>;
};

export type DirectiveResolvers<ContextType = any> = {
  oneOf?: OneOfDirectiveResolver<any, any, ContextType>;
};


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
export function useSearchOrgenheterQuery(baseOptions: Apollo.QueryHookOptions<SearchOrgenheterQuery, SearchOrgenheterQueryVariables> & ({ variables: SearchOrgenheterQueryVariables; skip?: boolean; } | { skip: boolean; }) ) {
        const options = {...defaultOptions, ...baseOptions}
        return Apollo.useQuery<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>(SearchOrgenheterDocument, options);
      }
export function useSearchOrgenheterLazyQuery(baseOptions?: Apollo.LazyQueryHookOptions<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>) {
          const options = {...defaultOptions, ...baseOptions}
          return Apollo.useLazyQuery<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>(SearchOrgenheterDocument, options);
        }
export function useSearchOrgenheterSuspenseQuery(
  baseOptions: Apollo.SuspenseQueryHookOptions<
    SearchOrgenheterQuery,
    SearchOrgenheterQueryVariables
  > & { variables: SearchOrgenheterQueryVariables }
) {
  const options = { ...defaultOptions, ...baseOptions };
  return Apollo.useSuspenseQuery<
    SearchOrgenheterQuery,
    SearchOrgenheterQueryVariables
  >(SearchOrgenheterDocument, options);
}
export type SearchOrgenheterQueryHookResult = ReturnType<typeof useSearchOrgenheterQuery>;
export type SearchOrgenheterLazyQueryHookResult = ReturnType<typeof useSearchOrgenheterLazyQuery>;
export type SearchOrgenheterSuspenseQueryHookResult = ReturnType<typeof useSearchOrgenheterSuspenseQuery>;
export type SearchOrgenheterQueryResult = Apollo.QueryResult<SearchOrgenheterQuery, SearchOrgenheterQueryVariables>;