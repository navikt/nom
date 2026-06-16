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

export enum UserRoles {
  ROLE_WRITE = "ROLE_WRITE",
  ROLE_ADMIN = "ROLE_ADMIN",
  ROLE_PILOT = "ROLE_PILOT",
  ROLE_WRITE_UTL_KONSULENT = "ROLE_WRITE_UTL_KONSULENT",
  ROLE_WRITE_VIKAFOSSEN = "ROLE_WRITE_VIKAFOSSEN",
  ROLE_WRITE_ROBOT = "ROLE_WRITE_ROBOT",
  ROLE_READONLY = "ROLE_READONLY",
  ROLE_EXTENDED_READONLY = "ROLE_EXTENDED_READONLY",
  ROLE_KOSTNADSTED_MAPPING_ADMIN = "ROLE_KOSTNADSTED_MAPPING_ADMIN",
  ROLE_EXTERNAL_FUNDS_RECIPIENT,
}

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

export enum TelefonType {
  NAV_TJENESTE_TELEFON = "NAV_TJENESTE_TELEFON",
  NAV_KONTOR_TELEFON = "NAV_KONTOR_TELEFON",
  PRIVAT_TELEFON = "PRIVAT_TELEFON",
}

export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
}

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

export enum NomNivaa {
  LINJEENHET = "LINJEENHET",
  DRIFTSENHET = "DRIFTSENHET",
  ARBEIDSOMRAADE = "ARBEIDSOMRAADE",
}

export enum IdentType {
  ROBOT = "ROBOT",
  STANDARD = "STANDARD",
  VIKAFOSSEN = "VIKAFOSSEN",
  UTENLANDSK_KONSULENT = "UTENLANDSK_KONSULENT",
  UKJENT = "UKJENT",
}

export enum AnsettelsesType {
  FAST = "FAST",
  MIDLERTIDIG = "MIDLERTIDIG",
}

export interface AvvikleOrgEnhetRequest {
  /**
   * @format date
   * @example "2026-12-31"
   */
  gyldigTom: string;
}

export interface AvvikleRessursRequest {
  /** @format date */
  sluttDato: string;
}

export interface BrukerSkrivetilgangRessursRequest {
  bruker: string;
  ressurs: string;
}

export interface BrukerSkrivetilgangRessurserRequest {
  bruker: string;
  ressurser: string[];
}

export interface EndreOrgtilknytningRequest {
  erDagligOppfolging: boolean;
  /** @minLength 1 */
  fid: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  /**
   * @uniqueItems true
   * @example ["2700","2900"]
   */
  optionalFysiskBrukersteder?: string[];
  /** @example "2700" */
  primaryFysiskBrukerstedNummer?: string;
  sektor?: Sektor;
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
  feilmelding: string;
  orgtilknytningFid: string;
}

export interface FolkeregisterPerson {
  navn: RessursNavn;
}

export interface GetPersonRequest {
  identType?: IdentType;
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
  /** @example "ab123c" */
  id: string;
  /** @example "Nav Lokal Kontor" */
  navn: string;
}

export interface LederTilgangForOrgenhetRequest {
  delegertTilNavident: string;
  /** @format date */
  gyldigTom?: string;
  orgenhetId: string;
}

export interface LederTilgangState {
  active?: boolean;
  av: string;
  fid: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  orgenhetId: string;
  til: string;
}

export interface LeggTilOrgtilknytningRequest {
  erDagligOppfolging: boolean;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  /**
   * @uniqueItems true
   * @example ["2700","2900"]
   */
  optionalFysiskBrukersteder?: string[];
  orgEnhetId: string;
  /** @example "2700" */
  primaryFysiskBrukerstedNummer?: string;
  sektor: Sektor;
  tilknytningtype?: Tilknytningtype;
}

export interface MasseFlyttingRequest {
  beholdTilganger: boolean;
  /** @format date */
  gyldigFomFlyttedato?: string;
  nyOrgenhetId: string;
  orgtilknytningerFid: string[];
}

export interface MasseFlyttingResponse {
  /** @format int32 */
  antallFlyttet: number;
  feiledeFlyttinger: FeiletFlytting[];
}

export interface MinimalRessurs {
  navident: string;
  visningsnavn: string;
}

export interface OppdaterOrgtilknytningerForRessursRequest {
  eksisterendeOrgtilknytninger: EndreOrgtilknytningRequest[];
  nyeOrgtilknytninger: LeggTilOrgtilknytningRequest[];
  setRessursSluttdato: boolean;
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
   * @example "2026-01-31"
   */
  gyldigFom: string;
  /**
   * @minLength 1
   * @example "A123456"
   */
  lederRessursNavident: string;
  /**
   * @minLength 1
   * @example "Nav Lokal kontor"
   */
  navn: string;
  /** @example "DIREKTORAT" */
  orgEnhetsType: OrgEnhetsType;
  /**
   * @minLength 1
   * @example "de456f"
   */
  overordnetOrgEnhetId: string;
  /**
   * @minLength 1
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
  ansettelsesType: AnsettelsesType;
  fysiskBrukerstedNr: string;
  identType?: IdentType;
  orgEnhetId: string;
  personident: string;
  primaryTelefonnummer?: string;
  sektor: Sektor;
  /** @format date */
  sluttDato?: string;
  /** @format date */
  startDato: string;
  tilknytningtype: Tilknytningtype;
  visningsetternavn: string;
  visningsfornavn: string;
  visningsmellomnavn?: string;
}

export interface OpprettRessursResponse {
  navident: string;
  personident: string;
  sektor: Sektor;
  visningsetternavn: string;
  visningsfornavn: string;
}

export interface OrgEnhet {
  agressoId: string;
  agressoOrgenhetType: string;
  fid: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  id: string;
  leder: MinimalRessurs;
  ledere: RessursState[];
  navn: string;
  nomNivaa?: NomNivaa;
  orgEnhetsType?: OrgEnhetsType;
  orgNiv: string;
  overordnetEnhetId?: string;
  remedyEnhetId?: string;
  ressursKoblinger: OrgtilknytningState[];
  tilgangsenhetId?: string;
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
  /** @format date */
  gyldigFom: string;
  id: string;
  ledere: string[];
  navn: string;
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
  additionalFysiskeBrukerstederNummer?: string[];
  erDagligOppfolging: boolean;
  fid: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  masseflyttetFraFid?: string;
  navident: string;
  orgEnhetId: string;
  orgtilknytningType?: Tilknytningtype;
  primaryFysiskBrukerstedNummer?: string;
  sektor?: Sektor;
  sistEndretAv?: string;
}

export interface OvrigeEndringerOrgEnhetRequest {
  /**
   * @format date
   * @example "2025-01-01"
   */
  gyldigFom?: string;
  /** @example "ab123c" */
  id: string;
  /** @example "ARBEIDSOMRAADE" */
  nomNivaa?: NomNivaa;
  /** @example "DIREKTORAT" */
  orgEnhetsType?: OrgEnhetsType;
  /** @example "1234" */
  tilgangsenhetId?: string;
}

export interface Person {
  folkeregisterPerson: FolkeregisterPerson;
  navident?: string;
  personident: string;
}

export interface ReadTopicRequest {
  /** @format int64 */
  fromOffset: number;
  /** @format int32 */
  maxRecords: number;
  /** @format int32 */
  partition?: number;
}

export interface Ressurs {
  epost?: string;
  fid: string;
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  isNomMastered: boolean;
  jobbtelefon?: Jobbtelefon;
  navident: string;
  personident: string;
  primaryFysiskBrukersted?: string;
  primaryTelefonnummer?: string;
  sektor: Sektor;
  sistEndretAv?: string;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  tilknytningtype?: Tilknytningtype;
  visningsnavn: Visningsnavn;
}

export interface RessursFilter {
  sektor?: Sektor;
  /** @format date */
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
  erDagligOppfolging: boolean;
  leder: RessursState;
  lederForNavident: string;
}

export interface RessursNavn {
  etternavn: string;
  fornavn: string;
  mellomnavn?: string;
}

export interface RessursState {
  fid: string;
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  jobbtelefon?: Jobbtelefon;
  navident: string;
  personident: string;
  primaryFysiskBrukersted?: string;
  primaryTelefonnummer?: string;
  sektor: Sektor;
  sistEndretAv?: string;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  tilknytningtype?: Tilknytningtype;
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
  name: string;
  navident: string;
  roles: UserRoles[];
}

export interface SetOrgEnhetsTypeRequest {
  nomId: string;
  orgEnhetsType?: OrgEnhetsType;
}

export interface StandardResponse {
  message: string;
}

export interface UpdateFysiskBrukerstedForOrgtilknytningRequest {
  orgtilknytningerFid: string[];
  tilFysiskBrukerstedNr: string;
}

export interface UpdateFysiskeBrukerstederRequest {
  /** @uniqueItems true */
  fysiskeBrukersteder: string[];
  primaryFysiskBrukersted: string;
}

export interface UpdatePrimaryFysiskBrukerstedForRessurserRequest {
  navidenter: string[];
  newPrimaryFysiskBrukersted: string;
}

export interface UpdatePrimaryFysiskBrukerstedRequest {
  newPrimaryFysiskBrukersted: string;
}

export interface UpdatePrimarytelefonnummer {
  nummer: string;
}

export interface UpdateVisningsnavnRequest {
  etternavn?: string;
  fornavn?: string;
  mellomnavn?: string;
}

export interface Visningsnavn {
  etternavn: string;
  fornavn: string;
  mellomnavn?: string;
}
