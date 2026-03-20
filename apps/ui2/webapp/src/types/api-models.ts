/* eslint-disable */
/* tslint:disable */
// @ts-nocheck
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

/** @default "##default" */
export enum UserRoles {
  ROLE_WRITE = "ROLE_WRITE",
  ROLE_ADMIN = "ROLE_ADMIN",
  ROLE_PILOT = "ROLE_PILOT",
  ROLE_WRITE_UTL_KONSULENT = "ROLE_WRITE_UTL_KONSULENT",
  ROLE_WRITE_VIKAFOSSEN = "ROLE_WRITE_VIKAFOSSEN",
  ROLE_WRITE_ROBOT = "ROLE_WRITE_ROBOT",
  ROLE_READONLY = "ROLE_READONLY",
  ROLE_EXTENDED_READONLY = "ROLE_EXTENDED_READONLY",
}

/** @default "##default" */
export enum Tilknytningtype {
  ANNET = "ANNET",
  ARBEIDSTRENING = "ARBEIDSTRENING",
  EKSTERN_SAMARBEIDSPARTNER = "EKSTERN_SAMARBEIDSPARTNER",
  FAST = "FAST",
  HELFO = "HELFO",
  INGEN = "INGEN",
  JOBBSPESIALIST = "JOBBSPESIALIST",
  KONSULENT = "KONSULENT",
  LAERLING = "LAERLING",
  MIDLERTIDIG = "MIDLERTIDIG",
  PENSJONIST = "PENSJONIST",
  PRAKSISARBEID = "PRAKSISARBEID",
  RIKSREVISJONEN = "RIKSREVISJONEN",
  SKATTEETATEN = "SKATTEETATEN",
  STATSFORVALTEREN = "STATSFORVALTEREN",
  STUDENT = "STUDENT",
  UFORUTSETT_BEHOV = "UFORUTSETT_BEHOV",
  UTDANNINGSSTILLINGER = "UTDANNINGSSTILLINGER",
  VIKAR = "VIKAR",
}

/** @default "##default" */
export enum TelefonType {
  NAV_TJENESTE_TELEFON = "NAV_TJENESTE_TELEFON",
  NAV_KONTOR_TELEFON = "NAV_KONTOR_TELEFON",
  PRIVAT_TELEFON = "PRIVAT_TELEFON",
}

/** @default "##default" */
export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
}

/** @default "##default" */
export enum OrgEnhetsType {
  ARBEIDSLIVSSENTER = "ARBEIDSLIVSSENTER",
  NAV_ARBEID_OG_YTELSER = "NAV_ARBEID_OG_YTELSER",
  ARBEIDSRAADGIVNING = "ARBEIDSRAADGIVNING",
  DIREKTORAT = "DIREKTORAT",
  DIR = "DIR",
  FYLKE = "FYLKE",
  NAV_FAMILIE_OG_PENSJONSYTELSER = "NAV_FAMILIE_OG_PENSJONSYTELSER",
  HJELPEMIDLER_OG_TILRETTELEGGING = "HJELPEMIDLER_OG_TILRETTELEGGING",
  KLAGEINSTANS = "KLAGEINSTANS",
  NAV_KONTAKTSENTER = "NAV_KONTAKTSENTER",
  KONTROLL_KONTROLLENHET = "KONTROLL_KONTROLLENHET",
  NAV_KONTOR = "NAV_KONTOR",
  TILTAK = "TILTAK",
  NAV_OKONOMITJENESTE = "NAV_OKONOMITJENESTE",
}

/** @default "##default" */
export enum NomNivaa {
  LINJEENHET = "LINJEENHET",
  DRIFTSENHET = "DRIFTSENHET",
  ARBEIDSOMRAADE = "ARBEIDSOMRAADE",
}

/** @default "##default" */
export enum IdentType {
  ROBOT = "ROBOT",
  STANDARD = "STANDARD",
  VIKAFOSSEN = "VIKAFOSSEN",
  UTENLANDSK_KONSULENT = "UTENLANDSK_KONSULENT",
  UKJENT = "UKJENT",
}

/** @default "##default" */
export enum AnsettelsesType {
  FAST = "FAST",
  MIDLERTIDIG = "MIDLERTIDIG",
}

export interface AvvikleOrgEnhetRequest {
  /**
   * @format date
   * @default "##default"
   * @example "2026-12-31"
   */
  gyldigTom: string;
}

export interface AvvikleRessursRequest {
  /**
   * @format date
   * @default "##default"
   */
  sluttDato: string;
}

export interface BrukerSkrivetilgangRessursRequest {
  /** @default "##default" */
  bruker: string;
  /** @default "##default" */
  ressurs: string;
}

export interface BrukerSkrivetilgangRessurserRequest {
  /** @default "##default" */
  bruker: string;
  /** @default "##default" */
  ressurser: string[];
}

export interface EndreOrgtilknytningRequest {
  /** @default false */
  erDagligOppfolging: boolean;
  /** @default "##default" */
  fid: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigFom: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigTom?: string;
  /** @default "##default" */
  sektor?: Sektor;
  /** @default "##default" */
  tilknytningtype?: Tilknytningtype;
}

export interface EndreTilknytningtypeRequest {
  tilknytningtype?: Tilknytningtype;
}

export interface ExcelExportRequest {
  navidenter?: string[];
  topOrgEnhetId?: string;
}

export interface FeiletFlytting {
  /** @default "##default" */
  feilmelding: string;
  /** @default "##default" */
  orgtilknytningFid: string;
}

export interface FolkeregisterPerson {
  /** @default "##default" */
  navn: RessursNavn;
}

export interface GetPersonRequest {
  /** @default "##default" */
  identType?: IdentType;
  /** @default "##default" */
  personident: string;
}

export interface GlobalStatistikkResponse {
  /** @format int32 */
  antallFylkekontor?: number;
  /** @format int32 */
  antallHjelpemidlerOgTilrettelegging?: number;
  /** @format int32 */
  antallKlageinstanser?: number;
  /** @format int32 */
  antallKontrollKonrollenheter?: number;
  /** @format int32 */
  antallLovNoder?: number;
  /** @format int32 */
  antallNavArbeidOgYtelser?: number;
  /** @format int32 */
  antallNavFamilieOgPensjonsytelser?: number;
  /** @format int32 */
  antallNavKontaktsenter?: number;
  /** @format int32 */
  antallNavKontorer?: number;
  ressursStatistikk?: RessursStatistikk;
}

export interface IOrgenhet {
  agressoId?: string;
  agressoOrgenhetType?: string;
  fid?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  id?: string;
  navn?: string;
  nomNivaa?: NomNivaa;
  orgEnhetsType?: OrgEnhetsType;
  orgNiv?: string;
  overordnetOrgEnhetFid?: string;
  uniqueAgressoId?: string;
}

export interface Jobbtelefon {
  nummer?: string;
  type?: TelefonType;
}

export interface JsonNode {
  array?: boolean;
  bigDecimal?: boolean;
  bigInteger?: boolean;
  binary?: boolean;
  boolean?: boolean;
  container?: boolean;
  double?: boolean;
  embeddedValue?: boolean;
  empty?: boolean;
  float?: boolean;
  floatingPointNumber?: boolean;
  int?: boolean;
  integralNumber?: boolean;
  long?: boolean;
  missingNode?: boolean;
  nodeType?:
    | "ARRAY"
    | "BINARY"
    | "BOOLEAN"
    | "MISSING"
    | "NULL"
    | "NUMBER"
    | "OBJECT"
    | "POJO"
    | "STRING";
  null?: boolean;
  number?: boolean;
  object?: boolean;
  pojo?: boolean;
  short?: boolean;
  string?: boolean;
  /** @deprecated */
  textual?: boolean;
  valueNode?: boolean;
}

export interface LederTilgangForOrgenhetRequest {
  /** @default "##default" */
  delegertTilNavident: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigTom?: string;
  /** @default "##default" */
  orgenhetId: string;
}

export interface LederTilgangState {
  active?: boolean;
  /** @default "##default" */
  av: string;
  /** @default "##default" */
  fid: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigFom: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigTom?: string;
  /** @default "##default" */
  orgenhetId: string;
  /** @default "##default" */
  til: string;
}

export interface LeggTilOrgtilknytningRequest {
  /** @default false */
  erDagligOppfolging: boolean;
  /**
   * @format date
   * @default "##default"
   */
  gyldigFom: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigTom?: string;
  /** @default "##default" */
  orgEnhetId: string;
  /** @default "##default" */
  sektor: Sektor;
  /** @default "##default" */
  tilknytningtype?: Tilknytningtype;
}

export interface MasseFlyttingRequest {
  /** @default false */
  beholdTilganger: boolean;
  /**
   * @format date
   * @default "##default"
   */
  gyldigFomFlyttedato?: string;
  /** @default "##default" */
  nyOrgenhetId: string;
  /** @default "##default" */
  orgtilknytningerFid: string[];
}

export interface MasseFlyttingResponse {
  /**
   * @format int32
   * @default "##default"
   */
  antallFlyttet: number;
  /** @default "##default" */
  feiledeFlyttinger: FeiletFlytting[];
}

export interface MinimalRessurs {
  /** @default "##default" */
  navident: string;
  /** @default "##default" */
  visningsnavn: string;
}

export interface OppdaterOrgEnhetRequest {
  /**
   * @default "##default"
   * @example 1234
   */
  firesiffer?: string;
  /**
   * @format date
   * @default "##default"
   * @example "2026-01-31"
   */
  gyldigFom?: string;
  /**
   * @format date
   * @default "##default"
   * @example "2026-03-31"
   */
  gyldigTom?: string | null;
  /**
   * @default "##default"
   * @example "ab123c"
   */
  id?: string;
  /**
   * @default "##default"
   * @example "A123456"
   */
  lederRessursNavident?: string;
  /**
   * @default "##default"
   * @example "Nav Lokal Kontor"
   */
  navn?: string;
  /**
   * @default "##default"
   * @example "DIREKTORAT"
   */
  orgEnhetsType?: OrgEnhetsType;
}

export interface OppdaterOrgtilknytningerForRessursRequest {
  /** @default "##default" */
  eksisterendeOrgtilknytninger: EndreOrgtilknytningRequest[];
  /** @default "##default" */
  nyeOrgtilknytninger: LeggTilOrgtilknytningRequest[];
  /** @default false */
  setRessursSluttdato: boolean;
  /** @default false */
  setRessursStartdato: boolean;
}

export interface OpprettOrgEnhetRequest {
  /**
   * @minLength 1
   * @default "##default"
   * @example 1234
   */
  firesiffer: string;
  /**
   * @format date
   * @default "##default"
   * @example "2026-01-31"
   */
  gyldigFom: string;
  /**
   * @minLength 1
   * @default "##default"
   * @example "A123456"
   */
  lederRessursNavident: string;
  /**
   * @minLength 1
   * @default "##default"
   * @example "Nav Lokal kontor"
   */
  navn: string;
  /**
   * @default "##default"
   * @example "DIREKTORAT"
   */
  orgEnhetsType: OrgEnhetsType;
  /**
   * @minLength 1
   * @default "##default"
   * @example "de456f"
   */
  overordnetOrgEnhetId: string;
}

export interface OpprettOrgEnhetResponse {
  fid?: string;
  firesiffer?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  id?: string;
  lederRessursNavident?: string;
  lederRessursNavn?: string;
  navn?: string;
  orgEnhetsType?: OrgEnhetsType;
  overordnetOrgEnhetId?: string;
  overordnetOrgEnhetNavn?: string;
}

export interface OpprettRessursRequest {
  /** @default "##default" */
  ansettelsesType: AnsettelsesType;
  /** @default "##default" */
  fysiskBrukerstedNr: string;
  /** @default "##default" */
  identType?: IdentType;
  /** @default "##default" */
  orgEnhetId: string;
  /** @default "##default" */
  personident: string;
  /** @default "##default" */
  primaryTelefonnummer?: string;
  /** @default "##default" */
  sektor: Sektor;
  /**
   * @format date
   * @default "##default"
   */
  sluttDato?: string;
  /**
   * @format date
   * @default "##default"
   */
  startDato: string;
  /** @default "##default" */
  tilknytningtype: Tilknytningtype;
  /** @default "##default" */
  visningsetternavn: string;
  /** @default "##default" */
  visningsfornavn: string;
  /** @default "##default" */
  visningsmellomnavn?: string;
}

export interface OpprettRessursResponse {
  /** @default "##default" */
  navident: string;
  /** @default "##default" */
  personident: string;
  /** @default "##default" */
  sektor: Sektor;
  /** @default "##default" */
  visningsetternavn: string;
  /** @default "##default" */
  visningsfornavn: string;
}

export interface OrgEnhet {
  /** @default "##default" */
  agressoId: string;
  /** @default "##default" */
  agressoOrgenhetType: string;
  /** @default "##default" */
  fid: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigFom: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigTom?: string;
  /** @default "##default" */
  id: string;
  /** @default "##default" */
  leder: MinimalRessurs;
  /** @default "##default" */
  ledere: RessursState[];
  /** @default "##default" */
  navn: string;
  /** @default "##default" */
  nomNivaa?: NomNivaa;
  /** @default "##default" */
  orgEnhetsType?: OrgEnhetsType;
  /** @default "##default" */
  orgNiv: string;
  /** @default "##default" */
  overordnetEnhetId?: string;
  /** @default "##default" */
  remedyEnhetId?: string;
  /** @default "##default" */
  ressursKoblinger: OrgtilknytningState[];
  /** @default "##default" */
  underordnedeOrgEnhetId: string[];
}

export interface OrgEnhetLederResponse {
  fid?: string;
  navident?: string;
  orgEnhetId?: string;
  orgEnhetNavn?: string;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  visningsnavn?: Visningsnavn;
}

export interface OrgEnhetMedLedernavn {
  /**
   * @format date
   * @default "##default"
   */
  gyldigFom: string;
  /** @default "##default" */
  id: string;
  /** @default "##default" */
  ledere: string[];
  /** @default "##default" */
  navn: string;
  /** @default "##default" */
  remedyEnhetId?: string;
}

export interface OrgEnhetStatistikkResponse {
  /** @format int32 */
  antallArbeidslivssenter?: number;
  /** @format int32 */
  antallArbeidsraadgivning?: number;
  /** @format int32 */
  antallFylkekontor?: number;
  /** @format int32 */
  antallHjelpemidlerOgTilrettelegging?: number;
  /** @format int32 */
  antallKlageinstanser?: number;
  /** @format int32 */
  antallKontrollKonrollenheter?: number;
  /** @format int32 */
  antallLovNoder?: number;
  /** @format int32 */
  antallNavArbeidOgYtelser?: number;
  /** @format int32 */
  antallNavFamilieOgPensjonsytelser?: number;
  /** @format int32 */
  antallNavKontaktsenter?: number;
  /** @format int32 */
  antallNavKontorer?: number;
  /** @format int32 */
  antallTiltak?: number;
  ressursStatistikk?: RessursStatistikk;
}

export interface OrgtilknytningState {
  /** @default false */
  erDagligOppfolging: boolean;
  /** @default "##default" */
  fid: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigFom: string;
  /**
   * @format date
   * @default "##default"
   */
  gyldigTom?: string;
  /** @default "##default" */
  masseflyttetFraFid?: string;
  /** @default "##default" */
  navident: string;
  /** @default "##default" */
  orgEnhetId: string;
  /** @default "##default" */
  orgtilknytningType?: Tilknytningtype;
  /** @default "##default" */
  sektor?: Sektor;
  /** @default "##default" */
  sistEndretAv?: string;
}

export interface Person {
  /** @default "##default" */
  folkeregisterPerson: FolkeregisterPerson;
  /** @default "##default" */
  navident?: string;
  /** @default "##default" */
  personident: string;
}

export interface ReadTopicRequest {
  /**
   * @format int64
   * @default "##default"
   */
  fromOffset: number;
  /**
   * @format int32
   * @default "##default"
   */
  maxRecords: number;
  /**
   * @format int32
   * @default "##default"
   */
  partition?: number;
}

export interface Ressurs {
  epost?: string;
  /** @default "##default" */
  fid: string;
  /** @default "##default" */
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  /** @default false */
  isNomMastered: boolean;
  /** @default "##default" */
  jobbtelefon?: Jobbtelefon;
  /** @default "##default" */
  navident: string;
  /** @default "##default" */
  personident: string;
  /** @default "##default" */
  primaryFysiskBrukersted?: string;
  /** @default "##default" */
  primaryTelefonnummer?: string;
  /** @default "##default" */
  sektor: Sektor;
  /** @default "##default" */
  sistEndretAv?: string;
  /**
   * @format date
   * @default "##default"
   */
  sluttdato?: string;
  /**
   * @format date
   * @default "##default"
   */
  startdato?: string;
  /** @default "##default" */
  tilknytningtype?: Tilknytningtype;
  /** @default "##default" */
  visningsnavn: Visningsnavn;
}

export interface RessursFilter {
  /** @default "##default" */
  sektor?: Sektor;
  /**
   * @format date
   * @default "##default"
   */
  sluttdato?: string;
}

export interface RessursKontaktperson {
  etternavn?: string;
  fornavn?: string;
  jobbtelefon?: string;
  kontaktpersonNavn?: string;
  kontaktpersonPrioritet?: string;
  kontaktpersonRelasjon?: string;
  kontaktpersonTelefonnr?: string;
  navident?: string;
  privattelefon?: string;
}

export interface RessursLeder {
  /** @default false */
  erDagligOppfolging: boolean;
  /** @default "##default" */
  leder: RessursState;
  /** @default "##default" */
  lederForNavident: string;
}

export interface RessursNavn {
  /** @default "##default" */
  etternavn: string;
  /** @default "##default" */
  fornavn: string;
  /** @default "##default" */
  mellomnavn?: string;
}

export interface RessursState {
  /** @default "##default" */
  fid: string;
  /** @default "##default" */
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  /** @default "##default" */
  jobbtelefon?: Jobbtelefon;
  /** @default "##default" */
  navident: string;
  /** @default "##default" */
  personident: string;
  /** @default "##default" */
  primaryFysiskBrukersted?: string;
  /** @default "##default" */
  primaryTelefonnummer?: string;
  /** @default "##default" */
  sektor: Sektor;
  /** @default "##default" */
  sistEndretAv?: string;
  /**
   * @format date
   * @default "##default"
   */
  sluttdato?: string;
  /**
   * @format date
   * @default "##default"
   */
  startdato?: string;
  /** @default "##default" */
  tilknytningtype?: Tilknytningtype;
  /** @default "##default" */
  visningsnavn: Visningsnavn;
}

export interface RessursStatistikk {
  /** @format int64 */
  antallAktiveRessurser?: number;
  /** @format int64 */
  antallEksterneRessurser?: number;
  /** @format int64 */
  antallKommunaleRessurser?: number;
  /** @format int64 */
  antallStatligeRessurser?: number;
}

export interface SessionInfoResponse {
  /** @default "##default" */
  name: string;
  /** @default "##default" */
  navident: string;
  /** @default "##default" */
  roles: UserRoles[];
}

export interface SetOrgEnhetsTypeRequest {
  /** @default "##default" */
  nomId: string;
  /** @default "##default" */
  orgEnhetsType?: OrgEnhetsType;
}

export interface StandardResponse {
  /** @default "##default" */
  message: string;
}

export interface UpdateFysiskeBrukerstederRequest {
  /**
   * @uniqueItems true
   * @default "##default"
   */
  fysiskeBrukersteder: string[];
  /** @default "##default" */
  primaryFysiskBrukersted: string;
}

export interface UpdatePrimaryFysiskBrukerstedForRessurserRequest {
  /** @default "##default" */
  navidenter: string[];
  /** @default "##default" */
  newPrimaryFysiskBrukersted: string;
}

export interface UpdatePrimaryFysiskBrukerstedRequest {
  /** @default "##default" */
  newPrimaryFysiskBrukersted: string;
}

export interface UpdatePrimarytelefonnummer {
  /** @default "##default" */
  nummer: string;
}

export interface UpdateVisningsnavnRequest {
  etternavn?: string;
  fornavn?: string;
  mellomnavn?: string;
}

export interface Visningsnavn {
  /** @default "##default" */
  etternavn: string;
  /** @default "##default" */
  fornavn: string;
  /** @default "##default" */
  mellomnavn?: string;
}
