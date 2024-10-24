/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

export interface RessursSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

/** @default "STANDARD" */
export enum IdentType {
  ROBOT = "ROBOT",
  STANDARD = "STANDARD",
  VIKAFOSSEN = "VIKAFOSSEN",
  UTENLANDSK_KONSULENT = "UTENLANDSK_KONSULENT",
  UKJENT = "UKJENT",
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
  identType: IdentType;
}

export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
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
  erDagligOppfolging: boolean;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  orgtilknytningType?: Tilknytningtype;
}

export enum Tilknytningtype {
  KONSULENT = "KONSULENT",
  EKSTERN_SAMARBEIDSPARTNER = "EKSTERN_SAMARBEIDSPARTNER",
  INGEN = "INGEN",
}

export interface OrgenhetStateSearchParams {
  /**
   * @format int32
   * @min 0
   */
  fromOffset?: number;
  filterText?: string;
}

export enum NomNivaa {
  LINJEENHET = "LINJEENHET",
  DRIFTSENHET = "DRIFTSENHET",
  ARBEIDSOMRAADE = "ARBEIDSOMRAADE",
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
  inputInterfaceMethodArguments?: object;
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

export enum EntityType {
  ORGENHET = "ORGENHET",
  RESSURS = "RESSURS",
}

export interface HendelseDTO {
  id?: string;
  /** @format date-time */
  timestamp?: string;
  entityType?: EntityType;
  hendelseType?: HendelseType;
}

export enum HendelseType {
  NY = "NY",
  ENDRET = "ENDRET",
  FJERNET = "FJERNET",
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

export interface OrgtilknytningMedOrgEnhet {
  navident?: string;
  uniqueAgressoId?: string;
  orgEnhetId?: string;
  gyldigFom?: string;
  gyldigTom?: string;
  orgEnhetGyldigFom?: string;
  orgEnhetGyldigTom?: string;
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
