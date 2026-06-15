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
  ROLE_READONLY = "ROLE_READONLY",
  ROLE_EXTENDED_READONLY = "ROLE_EXTENDED_READONLY",
}

/** @default null */
export enum HendelseType {
  NY = "NY",
  ENDRET = "ENDRET",
  FJERNET = "FJERNET",
}

/** @default null */
export enum EntityType {
  ORGENHET = "ORGENHET",
  RESSURS = "RESSURS",
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
export enum IdentType {
  ROBOT = "ROBOT",
  STANDARD = "STANDARD",
  VIKAFOSSEN = "VIKAFOSSEN",
  UTENLANDSK_KONSULENT = "UTENLANDSK_KONSULENT",
  UKJENT = "UKJENT",
}

export interface OrganiseringEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export interface OrgEnhetEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export interface OrgEnhetLederEventWithNavidentDTO {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  navident?: string;
  type: string;
}

export interface OrgtilknytningEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export interface RessursEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export interface Data {
  type: string;
}

export interface AuthorizationEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export interface RessursSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export interface Jobbtelefon {
  nummer?: string;
  type?: TelefonType;
}

export interface RessursState {
  /** @default "" */
  fid: string;
  /** @default "" */
  personident: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  sektor: Sektor;
  /** @default "" */
  visningsnavn: Visningsnavn;
  /** @default "" */
  jobbtelefon?: Jobbtelefon;
  /** @default "" */
  primaryTelefonnummer?: string;
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
  primaryFysiskBrukersted?: string;
  /** @default "" */
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  /** @default "" */
  sistEndretAv?: string;
  /** @default "" */
  tilknytningtype?: Tilknytningtype;
}

export interface Visningsnavn {
  /** @default "" */
  fornavn: string;
  /** @default "" */
  mellomnavn?: string;
  /** @default "" */
  etternavn: string;
}

export interface OrgtilknytningStateSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export interface OrgtilknytningState {
  /** @default "" */
  fid: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  orgEnhetId: string;
  /** @default "" */
  sektor?: Sektor;
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
  /** @default "" */
  orgtilknytningType?: Tilknytningtype;
  /** @default "" */
  primaryFysiskBrukerstedNummer?: string;
  /** @default "" */
  additionalFysiskeBrukerstederNummer?: string[];
  /** @default "" */
  masseflyttetFraFid?: string;
  /** @default "" */
  sistEndretAv?: string;
}

export interface OrgenhetStateSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export interface OrgEnhetState {
  fid?: string;
  id?: string;
  navn?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  overordnetOrgEnhetFid?: string;
  lederFid?: string;
  orgEnhetsType?: OrgEnhetsType;
  nomNivaa?: NomNivaa;
  uniqueAgressoId?: string;
  agressoId?: string;
  agressoOrgNivaa?: string;
  agressoOrgenhetType?: string;
  tilgangsenhetId?: string;
}

export interface OrgenhetLederStateSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export interface OrgEnhetLederState {
  fid?: string;
  navident?: string;
  orgEnhetId?: string;
  /** @format date */
  startdato?: string;
  /** @format date */
  sluttdato?: string;
  sistEndretAv?: string;
}

export interface OrganiseringStateSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export interface OrganiseringState {
  fid?: string;
  id?: string;
  navn?: string;
  idOver?: string;
  navnOver?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
}

export interface MasteredRessursDTO {
  navident?: string;
  /** @format date-time */
  opprettet?: string;
}

export interface EventSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export type DelegertAuthorizationAvsluttetEventV1 = AuthorizationEvent;

export type DelegertAuthorizationFornyetEventV1 = AuthorizationEvent;

export type DelegertAuthorizationOpprettetEventV1 = AuthorizationEvent;

export interface InputMetadata {
  inputInterfaceName?: string;
  inputInterfaceMethodName?: string;
  inputInterfaceMethodArguments?: any;
  origination?: Origination;
}

export interface Metadata {
  inputMetadata?: InputMetadata;
  /** @format date-time */
  createdTimestamp?: string;
}

export interface Origination {
  initiatedBy?: string;
  initiatedFrom?: string;
}

export type RessursFysiskeBrukerstederOppdatertEventV1 = RessursEvent;

export type RessursJobbtelefonOppdatertEventV1 = RessursEvent;

export type RessursOpprettetEventV1 = RessursEvent;

export type RessursPersonaliaOppdatertV1 = RessursEvent;

export type RessursPersonidentOppdatertV1 = RessursEvent;

export type RessursPrimaryTelefonnummerOppdatertEventV1 = RessursEvent;

export type RessursSektorOppdatertV1 = RessursEvent;

export type RessursSluttdatoOppdatertEventV1 = RessursEvent;

export type RessursSluttetEventV1 = RessursEvent;

export type RessursStartDatoOppdatertV1 = RessursEvent;

export type RessursTekniskSlettetEventV1 = RessursEvent;

export type RessursTilknytningtypeOppdatertV1 = RessursEvent;

export interface DeadLetterDTO {
  fid?: string;
  /** @format date-time */
  timestamp?: string;
  headers?: Record<string, string>;
  event?: any;
}

export type OrgtilknytningAvsluttetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDagligOppfolgingEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDatoEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDatoEndretEventV2 = OrgtilknytningEvent;

export type OrgtilknytningFjernetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningFysiskBrukerstedEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningOpprettetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningSektorEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningTilknytningtypeEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningTypeEndretEventV1 = OrgtilknytningEvent;

export type KorrigerGyldigFomEventV1 = OrgEnhetEvent;

export type NomNivaaEndretEventV1 = OrgEnhetEvent;

export type OrgEnhetAvvikletEventV1 = OrgEnhetEvent;

export type OrgEnhetEndretNavnEventV1 = OrgEnhetEvent;

export type OrgEnhetKorrigertEventV1 = OrgEnhetEvent;

export type OrgEnhetOpprettetEventV1 = OrgEnhetEvent;

export interface OrgEnhetTypeEndretEventData {
  orgEnhetsType?: OrgEnhetsType;
}

export type OrgEnhetTypeEndretEventV1 = OrgEnhetEvent & {
  data?: OrgEnhetTypeEndretEventData;
};

export interface TilgangsenhetEndretEventData {
  tilgangsenhetId?: string;
}

export type TilgangsenhetEndretEventV1 = OrgEnhetEvent & {
  data?: TilgangsenhetEndretEventData;
};

export type OrganiseringAvsluttetEventV1 = OrganiseringEvent;

export type OrganiseringEndretEventV1 = OrganiseringEvent;

export type OrganiseringEndretNavnEventV1 = OrganiseringEvent;

export type OrganiseringOpprettetEventV1 = OrganiseringEvent;

export interface RessursHendelseKafkaDTO {
  /** @default "" */
  key: string;
  /**
   * @format date-time
   * @default ""
   */
  timestamp: string;
  /** @default "" */
  fid: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  visningsnavn: Visningsnavn;
  /**
   * @format date
   * @default ""
   */
  startdato: string;
  /**
   * @format date
   * @default ""
   */
  sluttdato?: string;
  /** @default "" */
  sektor: Sektor;
  /** @default "" */
  orgtilknytning: RessursHendelseOrgtilknytningKafkaDTO;
  /** @default "" */
  eventtype: string;
}

export interface RessursHendelseOrgtilknytningKafkaDTO {
  /** @default "" */
  fid: string;
  /** @default "" */
  orgEnhetId: string;
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
  /** @default false */
  dagligOppfolging: boolean;
}

export interface OrgtilknytningKafkaDTO {
  /** @default "" */
  fid: string;
  /** @default "" */
  orgenhetId: string;
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
  /** @default false */
  dagligOppfolging: boolean;
  /** @default "" */
  sektor?: Sektor;
  /** @default "" */
  tilknytningType?: Tilknytningtype;
}

export interface RessursStateKafkaDTO {
  /** @default "" */
  key: string;
  /**
   * @format date-time
   * @default ""
   */
  timestamp: string;
  /** @default "" */
  personident: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  visningsnavn: Visningsnavn;
  /** @default "" */
  identType?: IdentType;
  /** @default "" */
  primaryTelefonnummer?: string;
  /** @default "" */
  primaryFysiskBrukersted?: string;
  /** @default "" */
  sektor: Sektor;
  /** @default "" */
  orgtilknytninger?: OrgtilknytningKafkaDTO[];
  /**
   * @format date
   * @default ""
   */
  startdato?: string;
  /**
   * @format date
   * @default ""
   */
  sluttdato?: string;
}

export interface OrgEnhetStateDTO {
  /** @default "" */
  key: string;
  /**
   * @format date-time
   * @default ""
   */
  timestamp: string;
  /** @default "" */
  id: string;
  /** @default "" */
  navn: string;
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
  nomNivaa?: NomNivaa;
  /** @default "" */
  orgEnhetsType?: OrgEnhetsType;
}

export interface HendelseDTO {
  id?: string;
  /** @format date-time */
  timestamp?: string;
  entityType?: EntityType;
  hendelseType?: HendelseType;
}

export interface SessionInfoResponse {
  /** @default "" */
  navident: string;
  /** @default "" */
  name: string;
  /** @default "" */
  roles: UserRoles[];
}

export interface EnhetSanityCheckResponse {
  /** @default "" */
  navn: string;
  /** @default "" */
  id: string;
  /** @default "" */
  agressoId: string;
}

export interface RessursSomHarSluttetOgErLederForEnhetSanityCheckResponse {
  /** @default "" */
  navident: string;
  /** @default "" */
  visningsnavn: string;
  /**
   * @format date
   * @default ""
   */
  sluttdato: string;
  /** @default "" */
  enheter: EnhetSanityCheckResponse[];
}

export interface RobotOrgtilknytning {
  /** @default "" */
  orgenhetId: string;
  /** @default "" */
  orgenhetNavn: string;
  /**
   * @format date
   * @default ""
   */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
}

export interface RobotRessurs {
  /** @default "" */
  navident: string;
  /** @default "" */
  visningsnavn: string;
  /** @default "" */
  sektor: Sektor;
  /** @format date */
  startdato?: string;
  /** @format date */
  sluttdato?: string;
  /** @default "" */
  orgtilknytninger: RobotOrgtilknytning[];
}

export interface RessursOrgtilknytningKoblingInfo {
  /** @format int32 */
  totaltAntallRessurser?: number;
  /** @format int32 */
  antallAktiveOrgtilknytninger?: number;
  /** @format int32 */
  ressurserMedAktiveOrgtilknytninger?: number;
  /** @format int32 */
  ressurserUtenAktiveOrgtilknytninger?: number;
  /** @format int32 */
  ressurserMedAktiveOrgtilknytningerForEnhetSomErInaktivEllerUtenOrganisering?: number;
  /** @format int32 */
  ressursMedTilknytningerUtoverEnhetGyldigPeriode?: number;
}

export interface RessursOrgtilknytningUlikSluttdato {
  /** @default "" */
  navident: string;
  /** @default "" */
  orgEnhetId: string;
  /** @default "" */
  remedyEnhetId: string;
  /**
   * @format date
   * @default ""
   */
  ressursSluttdato: string;
  /**
   * @format date
   * @default ""
   */
  orgtilknytningSluttdato: string;
}

export interface RessursMedFlereDOL {
  /** @default "" */
  navident: string;
  /** @default "" */
  otErMedOverlapp: string;
}

export interface RessursMedEnhet {
  navident?: string;
  ressursFid?: string;
  visningsnavn?: string;
  /** @format date */
  startdato?: string;
  /** @format date */
  sluttdato?: string;
  sektor?: Sektor;
  orgEnhetId?: string;
  orgEnhetNavn?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  uniqueAgressoId?: string;
}

export interface OverlappendeOrgtilknytning {
  /** @default "" */
  navident1: string;
  /** @default "" */
  orgEnhetId1: string;
  /** @default "" */
  fid1: string;
  /**
   * @format date
   * @default ""
   */
  gyldigFom1: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom1?: string;
  /** @default false */
  erDagligOppfolging1: boolean;
  /** @default "" */
  navident2: string;
  /** @default "" */
  orgEnhetId2: string;
  /** @default "" */
  fid2: string;
  /**
   * @format date
   * @default ""
   */
  gyldigFom2: string;
  /**
   * @format date
   * @default ""
   */
  gyldigTom2?: string;
  /** @default false */
  erDagligOppfolging2: boolean;
}

export interface OrgtilknytningMedOrgEnhet {
  navident?: string;
  uniqueAgressoId?: string;
  orgEnhetId?: string;
  gyldigFom?: string;
  gyldigTom?: string;
  orgEnhetGyldigFom?: string;
  orgEnhetGyldigTom?: string;
}

export interface DuplikatOrgtilknytning {
  /** @default "" */
  fid1: string;
  /** @default "" */
  fid2: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  orgEnhetId: string;
  /** @default "" */
  sektor?: Sektor;
  /** @default false */
  erDagligOppfolging: boolean;
  /**
   * @format date
   * @default ""
   */
  gyldigFom: string;
  /** @default "" */
  orgtilknytningType?: Tilknytningtype;
}

export interface LoevnodeMedFeilAntallArbOmr {
  /** @default "" */
  orgenhetId: string;
  /** @default "" */
  orgenhetNavn: string;
  /**
   * @format int32
   * @default ""
   */
  antallArbOmr: number;
}

export interface OrgEnhetArbeidsomraadeInfo {
  /**
   * @format int32
   * @default ""
   */
  antallLoevnoder: number;
  /** @default "" */
  loevnoderMedFeil: LoevnodeMedFeilAntallArbOmr[];
}

export interface OrgEnhetOrganisering {
  orgEnhetId?: string;
  uniqueAgressoId?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
}

export interface OrgEnhetStartOgSluttdato {
  /** @format int32 */
  totaltAntallEnheter?: number;
  /** @format int32 */
  antallEnheterMedStartdato?: number;
  /** @format int32 */
  antallEnheterMedSluttdato?: number;
  /** @format int32 */
  antallEnheterMedSluttdatoFoerStartdato?: number;
}

export interface EnheterOgOrganisering {
  /** @format int32 */
  aktiveEnheter?: number;
  /** @format int32 */
  aktiveOrganiseringer?: number;
  /** @format int32 */
  antallOverEnheter?: number;
  /** @format int32 */
  antallUnderEnheter?: number;
  /** @format int32 */
  aktiveEnheterUtenOverEnheter?: number;
  /** @format int32 */
  aktiveEnheterUtenUnderEnheter?: number;
  /** @format int32 */
  aktiveEnheterMedUnderEnheterUtenOverEnheter?: number;
  /** @format int32 */
  aktiveEnheterUtenNoenOrganisering?: number;
}

export interface ImportedRemedyUser {
  navident?: string;
  fornavn?: string;
  etternavn?: string;
  enhetsId?: string;
  enhetsNavn?: string;
  sektor?: Sektor;
  aktiv?: boolean;
  permisjon?: boolean;
  /** @format date */
  sluttDato?: string;
  /** @format date */
  utlopsDato?: string;
  /** @format date */
  regDato?: string;
  visningsnavn?: string;
}

export interface RemedyCompareResult {
  mastretINom?: boolean;
  ressursState?: RessursState;
  nomRemedyEnhet?: string;
  epost?: string;
  importedRemedyUser?: ImportedRemedyUser;
}
