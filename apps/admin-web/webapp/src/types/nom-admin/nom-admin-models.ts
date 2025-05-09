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
  ROLE_READONLY = "ROLE_READONLY",
  ROLE_EXTENDED_READONLY = "ROLE_EXTENDED_READONLY",
}

export enum HendelseType {
  NY = "NY",
  ENDRET = "ENDRET",
  FJERNET = "FJERNET",
}

export enum EntityType {
  ORGENHET = "ORGENHET",
  RESSURS = "RESSURS",
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

export enum Tilknytningtype {
  ANNET = "ANNET",
  ARBEIDSTRENING = "ARBEIDSTRENING",
  EKSTERN_SAMARBEIDSPARTNER = "EKSTERN_SAMARBEIDSPARTNER",
  FAST = "FAST",
  INGEN = "INGEN",
  JOBBSPESIALIST = "JOBBSPESIALIST",
  KONSULENT = "KONSULENT",
  LAERLING = "LAERLING",
  MIDLERTIDIG = "MIDLERTIDIG",
  PENSJONIST = "PENSJONIST",
  PRAKSISARBEID = "PRAKSISARBEID",
  STUDENT = "STUDENT",
  UFORUTSETT_BEHOV = "UFORUTSETT_BEHOV",
  UTDANNINGSSTILLINGER = "UTDANNINGSSTILLINGER",
  VIKAR = "VIKAR",
}

export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
}

export enum IdentType {
  ROBOT = "ROBOT",
  STANDARD = "STANDARD",
  VIKAFOSSEN = "VIKAFOSSEN",
  UTENLANDSK_KONSULENT = "UTENLANDSK_KONSULENT",
  UKJENT = "UKJENT",
}

export interface RessursSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export interface RessursState {
  fid: string;
  personident: string;
  navident: string;
  sektor: Sektor;
  visningsnavn: Visningsnavn;
  primaryTelefonnummer?: string;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  primaryFysiskBrukersted?: string;
  fysiskeBrukersteder: string[];
  /** @default "STANDARD" */
  identType: IdentType;
  sistEndretAv?: string;
}

export interface Visningsnavn {
  fornavn: string;
  mellomnavn?: string;
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
  fid: string;
  navident: string;
  orgEnhetId: string;
  sektor?: Sektor;
  erDagligOppfolging: boolean;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  orgtilknytningType?: Tilknytningtype;
  masseflyttetFraFid?: string;
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

export interface AuthorizationEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export interface Data {
  type: string;
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

export interface RessursEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export type RessursSluttdatoOppdatertEventV1 = RessursEvent;

export type RessursSluttetEventV1 = RessursEvent;

export interface DeadLetterDTO {
  fid?: string;
  header?: string;
  orgtilknytningState?: string;
}

export type OrgtilknytningAvsluttetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDagligOppfolgingEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDatoEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDatoEndretEventV2 = OrgtilknytningEvent;

export interface OrgtilknytningEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export type OrgtilknytningFjernetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningOpprettetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningSektorEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningTilknytningtypeEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningTypeEndretEventV1 = OrgtilknytningEvent;

export type NomNivaaEndretEventV1 = OrgEnhetEvent;

export type OrgEnhetAvvikletEventV1 = OrgEnhetEvent;

export interface OrgEnhetEvent {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export type OrgEnhetKorrigertEventV1 = OrgEnhetEvent;

export type OrgEnhetOpprettetEventV1 = OrgEnhetEvent;

export interface OrgEnhetTypeEndretEventData {
  orgEnhetsType?: OrgEnhetsType;
}

export type OrgEnhetTypeEndretEventV1 = OrgEnhetEvent & {
  data?: OrgEnhetTypeEndretEventData;
};

export interface RessursHendelseKafkaDTO {
  key: string;
  /** @format date-time */
  timestamp: string;
  fid: string;
  navident: string;
  visningsnavn: Visningsnavn;
  /** @format date */
  startdato: string;
  /** @format date */
  sluttdato?: string;
  sektor: Sektor;
  orgtilknytning: RessursHendelseOrgtilknytningKafkaDTO;
}

export interface RessursHendelseOrgtilknytningKafkaDTO {
  fid: string;
  orgEnhetId: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  dagligOppfolging: boolean;
}

export interface OrgtilknytningKafkaDTO {
  fid: string;
  orgenhetId: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  dagligOppfolging: boolean;
}

export interface RessursStateKafkaDTO {
  key: string;
  /** @format date-time */
  timestamp: string;
  personident: string;
  navident: string;
  visningsnavn: Visningsnavn;
  primaryTelefonnummer?: string;
  sektor: Sektor;
  orgtilknytninger?: OrgtilknytningKafkaDTO[];
  /** @format date */
  startdato?: string;
  /** @format date */
  sluttdato?: string;
}

export interface OrgEnhetStateDTO {
  key: string;
  /** @format date-time */
  timestamp: string;
  id: string;
  navn: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  nomNivaa?: NomNivaa;
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
  navident: string;
  name: string;
  roles: UserRoles[];
}

export interface RobotOrgtilknytning {
  orgenhetId: string;
  orgenhetNavn: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
}

export interface RobotRessurs {
  navident: string;
  visningsnavn: string;
  sektor: Sektor;
  /** @format date */
  startdato?: string;
  /** @format date */
  sluttdato?: string;
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
  navident: string;
  orgEnhetId: string;
  /** @format date */
  ressursSluttdato: string;
  /** @format date */
  orgtilknytningSluttdato: string;
}

export interface RessursMedFlereDOL {
  navident: string;
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
  navident1: string;
  orgEnhetId1: string;
  fid1: string;
  /** @format date */
  gyldigFom1: string;
  /** @format date */
  gyldigTom1?: string;
  erDagligOppfolging1: boolean;
  navident2: string;
  orgEnhetId2: string;
  fid2: string;
  /** @format date */
  gyldigFom2: string;
  /** @format date */
  gyldigTom2?: string;
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
  fid1: string;
  fid2: string;
  navident: string;
  orgEnhetId: string;
  sektor?: Sektor;
  erDagligOppfolging: boolean;
  /** @format date */
  gyldigFom: string;
  orgtilknytningType?: Tilknytningtype;
}

export interface LoevnodeMedFeilAntallArbOmr {
  orgenhetId: string;
  orgenhetNavn: string;
  /** @format int32 */
  antallArbOmr: number;
}

export interface OrgEnhetArbeidsomraadeInfo {
  /** @format int32 */
  antallLoevnoder: number;
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
