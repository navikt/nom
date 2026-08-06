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

/** @default null */
export enum UserRoles {
  ROLE_WRITE = "ROLE_WRITE",
  ROLE_ADMIN = "ROLE_ADMIN",
  ROLE_PILOT = "ROLE_PILOT",
  ROLE_WRITE_UTL_KONSULENT = "ROLE_WRITE_UTL_KONSULENT",
  ROLE_WRITE_VIKAFOSSEN = "ROLE_WRITE_VIKAFOSSEN",
  ROLE_WRITE_ROBOT = "ROLE_WRITE_ROBOT",
  ROLE_READONLY = "ROLE_READONLY",
  ROLE_EXTENDED_READONLY = "ROLE_EXTENDED_READONLY",
  ROLE_EXTERNAL_FUNDS_RECIPIENT = "ROLE_EXTERNAL_FUNDS_RECIPIENT",
  ROLE_KOSTNADSTED_MAPPING_ADMIN = "ROLE_KOSTNADSTED_MAPPING_ADMIN",
}

/** @default null */
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

/** @default null */
export enum TelefonType {
  NAV_TJENESTE_TELEFON = "NAV_TJENESTE_TELEFON",
  NAV_KONTOR_TELEFON = "NAV_KONTOR_TELEFON",
  PRIVAT_TELEFON = "PRIVAT_TELEFON",
}

/** @default null */
export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
}

/** @default null */
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

/** @default null */
export enum NomNivaa {
  LINJEENHET = "LINJEENHET",
  DRIFTSENHET = "DRIFTSENHET",
  ARBEIDSOMRAADE = "ARBEIDSOMRAADE",
}

/** @default null */
export enum IdentType {
  ROBOT = "ROBOT",
  STANDARD = "STANDARD",
  VIKAFOSSEN = "VIKAFOSSEN",
  UTENLANDSK_KONSULENT = "UTENLANDSK_KONSULENT",
  UKJENT = "UKJENT",
}

/** @default null */
export enum AnsettelsesType {
  FAST = "FAST",
  MIDLERTIDIG = "MIDLERTIDIG",
}

export interface AvvikleOrgEnhetRequest {
  /**
   * @format date
   * @default ""
   * @example "2026-12-31"
   */
  gyldigTom: string;
}

export interface AvvikleRessursRequest {
  /**
   * @format date
   * @default ""
   */
  sluttDato: string;
}

export interface BrukerSkrivetilgangRessursRequest {
  /** @default "" */
  bruker: string;
  /** @default "" */
  ressurs: string;
}

export interface BrukerSkrivetilgangRessurserRequest {
  /** @default "" */
  bruker: string;
  /** @default "" */
  ressurser: string[];
}

export interface EndreOrgtilknytningRequest {
  /** @default false */
  erDagligOppfolging: boolean;
  /**
   * @minLength 1
   * @default ""
   */
  fid: string;
  /**
   * @format date
   * @default ""
   */
  gyldigFom: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom?: string;
  /**
   * @uniqueItems true
   * @default ""
   * @example ["2700","2900"]
   */
  optionalFysiskBrukersteder?: string[];
  /**
   * @default ""
   * @example "2700"
   */
  primaryFysiskBrukerstedNummer?: string;
  /** @default "" */
  sektor?: Sektor;
  /** @default "" */
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
  /** @default "" */
  feilmelding: string;
  /** @default "" */
  orgtilknytningFid: string;
}

export interface FolkeregisterPerson {
  /** @default "" */
  navn: RessursNavn;
}

export interface GetPersonRequest {
  /** @default "" */
  identType?: IdentType;
  /** @default "" */
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
  tilgangsenhetId?: string;
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

export interface KorrigerOrgEnhetNavnRequest {
  /**
   * @default ""
   * @example "ab123c"
   */
  id: string;
  /**
   * @default ""
   * @example "Nav Lokal Kontor"
   */
  navn: string;
}

export interface LederTilgangForOrgenhetRequest {
  /** @default "" */
  delegertTilNavident: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom?: string;
  /** @default "" */
  orgenhetId: string;
}

export interface LederTilgangState {
  active?: boolean;
  /** @default "" */
  av: string;
  /** @default "" */
  fid: string;
  /**
   * @format date
   * @default ""
   */
  gyldigFom: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom?: string;
  /** @default "" */
  orgenhetId: string;
  /** @default "" */
  til: string;
}

export interface LeggTilOrgtilknytningRequest {
  /** @default false */
  erDagligOppfolging: boolean;
  /**
   * @format date
   * @default ""
   */
  gyldigFom: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom?: string;
  /**
   * @uniqueItems true
   * @default ""
   * @example ["2700","2900"]
   */
  optionalFysiskBrukersteder?: string[];
  /** @default "" */
  orgEnhetId: string;
  /**
   * @default ""
   * @example "2700"
   */
  primaryFysiskBrukerstedNummer?: string;
  /** @default "" */
  sektor: Sektor;
  /** @default "" */
  tilknytningtype?: Tilknytningtype;
}

export interface MasseFlyttingRequest {
  /** @default false */
  beholdTilganger: boolean;
  /**
   * @format date
   * @default ""
   */
  gyldigFomFlyttedato?: string;
  /** @default "" */
  nyOrgenhetId: string;
  /** @default "" */
  orgtilknytningerFid: string[];
}

export interface MasseFlyttingResponse {
  /**
   * @format int32
   * @default ""
   */
  antallFlyttet: number;
  /** @default "" */
  feiledeFlyttinger: FeiletFlytting[];
}

export interface MinimalRessurs {
  /** @default "" */
  navident: string;
  /** @default "" */
  visningsnavn: string;
}

export interface OppdaterOrgtilknytningerForRessursRequest {
  /** @default "" */
  eksisterendeOrgtilknytninger: EndreOrgtilknytningRequest[];
  /** @default "" */
  nyeOrgtilknytninger: LeggTilOrgtilknytningRequest[];
  /** @default false */
  setRessursSluttdato: boolean;
  /** @default false */
  setRessursStartdato: boolean;
}

export interface OpprettOrgEnhetLederRequest {
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  navident?: string;
  orgEnhetId?: string;
}

export interface OpprettOrgEnhetRequest {
  /**
   * @format date
   * @default ""
   * @example "2026-01-31"
   */
  gyldigFom: string;
  /**
   * @minLength 1
   * @default ""
   * @example "A123456"
   */
  lederRessursNavident: string;
  /**
   * @minLength 1
   * @default ""
   * @example "Nav Lokal kontor"
   */
  navn: string;
  /**
   * @default ""
   * @example "DIREKTORAT"
   */
  orgEnhetsType: OrgEnhetsType;
  /**
   * @minLength 1
   * @default ""
   * @example "de456f"
   */
  overordnetOrgEnhetId: string;
  /**
   * @minLength 1
   * @default ""
   * @example "1234"
   */
  tilgangsenhetId: string;
}

export interface OpprettOrgEnhetResponse {
  fid?: string;
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
  tilgangsenhetId?: string;
}

export interface OpprettRessursRequest {
  /** @default "" */
  ansettelsesType: AnsettelsesType;
  /** @default "" */
  fysiskBrukerstedNr: string;
  /** @default "" */
  identType?: IdentType;
  /** @default "" */
  orgEnhetId: string;
  /** @default "" */
  personident: string;
  /** @default "" */
  primaryTelefonnummer?: string;
  /** @default "" */
  sektor: Sektor;
  /**
   * @format date
   * @default ""
   */
  sluttDato?: string;
  /**
   * @format date
   * @default ""
   */
  startDato: string;
  /** @default "" */
  tilknytningtype: Tilknytningtype;
  /** @default "" */
  visningsetternavn: string;
  /** @default "" */
  visningsfornavn: string;
  /** @default "" */
  visningsmellomnavn?: string;
}

export interface OpprettRessursResponse {
  /** @default "" */
  navident: string;
  /** @default "" */
  personident: string;
  /** @default "" */
  sektor: Sektor;
  /** @default "" */
  visningsetternavn: string;
  /** @default "" */
  visningsfornavn: string;
}

export interface OrgEnhet {
  /** @default "" */
  agressoId: string;
  /** @default "" */
  agressoOrgenhetType: string;
  /** @default "" */
  fid: string;
  /**
   * @format date
   * @default ""
   */
  gyldigFom: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom?: string;
  /** @default "" */
  id: string;
  /** @default "" */
  leder: MinimalRessurs;
  /** @default "" */
  ledere: RessursState[];
  /** @default "" */
  navn: string;
  /** @default "" */
  nomNivaa?: NomNivaa;
  /** @default "" */
  orgEnhetsType?: OrgEnhetsType;
  /** @default "" */
  orgNiv: string;
  /** @default "" */
  overordnetEnhetId?: string;
  /** @default "" */
  remedyEnhetId?: string;
  /** @default "" */
  ressursKoblinger: OrgtilknytningState[];
  /** @default "" */
  tilgangsenhetId?: string;
  /** @default "" */
  underordnedeOrgEnhetId: string[];
}

export interface OrgEnhetKostnadsstedResponse {
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  kostnadsstedId?: string;
  orgenhetId?: string;
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
   * @default ""
   */
  gyldigFom: string;
  /** @default "" */
  id: string;
  /** @default "" */
  ledere: string[];
  /** @default "" */
  navn: string;
  /** @default "" */
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
  /** @default "" */
  additionalFysiskeBrukerstederNummer?: string[];
  /** @default false */
  erDagligOppfolging: boolean;
  /** @default "" */
  fid: string;
  /**
   * @format date
   * @default ""
   */
  gyldigFom: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom?: string;
  /** @default "" */
  masseflyttetFraFid?: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  orgEnhetId: string;
  /** @default "" */
  orgtilknytningType?: Tilknytningtype;
  /** @default "" */
  primaryFysiskBrukerstedNummer?: string;
  /** @default "" */
  sektor?: Sektor;
  /** @default "" */
  sistEndretAv?: string;
}

export interface OvrigeEndringerOrgEnhetRequest {
  /**
   * @format date
   * @default ""
   * @example "2025-01-01"
   */
  gyldigFom?: string;
  /**
   * @default ""
   * @example "ab123c"
   */
  id: string;
  /**
   * @default ""
   * @example "ARBEIDSOMRAADE"
   */
  nomNivaa?: NomNivaa;
  /**
   * @default ""
   * @example "DIREKTORAT"
   */
  orgEnhetsType?: OrgEnhetsType;
  /**
   * @default ""
   * @example "1234"
   */
  tilgangsenhetId?: string;
}

export interface Person {
  /** @default "" */
  folkeregisterPerson: FolkeregisterPerson;
  /** @default "" */
  navident?: string;
  /** @default "" */
  personident: string;
}

export interface ReadTopicRequest {
  /**
   * @format int64
   * @default ""
   */
  fromOffset: number;
  /**
   * @format int32
   * @default ""
   */
  maxRecords: number;
  /**
   * @format int32
   * @default ""
   */
  partition?: number;
}

export interface Ressurs {
  epost?: string;
  /** @default "" */
  fid: string;
  /** @default "" */
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  /** @default false */
  isNomMastered: boolean;
  /** @default "" */
  jobbtelefon?: Jobbtelefon;
  /** @default "" */
  navident: string;
  /** @default "" */
  personident: string;
  /** @default "" */
  primaryFysiskBrukersted?: string;
  /** @default "" */
  primaryTelefonnummer?: string;
  /** @default "" */
  sektor: Sektor;
  /** @default "" */
  sistEndretAv?: string;
  /**
   * @format date
   * @default ""
   */
  sluttdato?: string;
  /**
   * @format date
   * @default ""
   */
  startdato?: string;
  /** @default "" */
  tilknytningtype?: Tilknytningtype;
  /** @default "" */
  visningsnavn: Visningsnavn;
}

export interface RessursFilter {
  /** @default "" */
  sektor?: Sektor;
  /**
   * @format date
   * @default ""
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
  /** @default "" */
  leder: RessursState;
  /** @default "" */
  lederForNavident: string;
}

export interface RessursNavn {
  /** @default "" */
  etternavn: string;
  /** @default "" */
  fornavn: string;
  /** @default "" */
  mellomnavn?: string;
}

export interface RessursState {
  /** @default "" */
  fid: string;
  /** @default "" */
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  /** @default "" */
  jobbtelefon?: Jobbtelefon;
  /** @default "" */
  navident: string;
  /** @default "" */
  personident: string;
  /** @default "" */
  primaryFysiskBrukersted?: string;
  /** @default "" */
  primaryTelefonnummer?: string;
  /** @default "" */
  sektor: Sektor;
  /** @default "" */
  sistEndretAv?: string;
  /**
   * @format date
   * @default ""
   */
  sluttdato?: string;
  /**
   * @format date
   * @default ""
   */
  startdato?: string;
  /** @default "" */
  tilknytningtype?: Tilknytningtype;
  /** @default "" */
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
  /** @default "" */
  name: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  roles: UserRoles[];
}

export interface SetOrgEnhetsTypeRequest {
  /** @default "" */
  nomId: string;
  /** @default "" */
  orgEnhetsType?: OrgEnhetsType;
}

export interface StandardResponse {
  /** @default "" */
  message: string;
}

export interface UpdateFysiskBrukerstedForOrgtilknytningRequest {
  /** @default "" */
  orgtilknytningerFid: string[];
  /** @default "" */
  tilFysiskBrukerstedNr: string;
}

export interface UpdateFysiskeBrukerstederRequest {
  /**
   * @uniqueItems true
   * @default ""
   */
  fysiskeBrukersteder: string[];
  /** @default "" */
  primaryFysiskBrukersted: string;
}

export interface UpdatePrimaryFysiskBrukerstedForRessurserRequest {
  /** @default "" */
  navidenter: string[];
  /** @default "" */
  newPrimaryFysiskBrukersted: string;
}

export interface UpdatePrimaryFysiskBrukerstedRequest {
  /** @default "" */
  newPrimaryFysiskBrukersted: string;
}

export interface UpdatePrimarytelefonnummer {
  /** @default "" */
  nummer: string;
}

export interface UpdateVisningsnavnRequest {
  etternavn?: string;
  fornavn?: string;
  mellomnavn?: string;
}

export interface Visningsnavn {
  /** @default "" */
  etternavn: string;
  /** @default "" */
  fornavn: string;
  /** @default "" */
  mellomnavn?: string;
}
