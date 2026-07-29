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

type UtilRequiredKeys<T, K extends keyof T> = Omit<T, K> & Required<Pick<T, K>>;

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

export type RessursEvent = UtilRequiredKeys<Event, "type"> & {
  type: string;
};

export type OrgtilknytningEvent = UtilRequiredKeys<Event, "type"> & {
  type: string;
};

export type OrganiseringEvent = UtilRequiredKeys<Event, "type"> & {
  type: string;
};

export type OrgEnhetLederEvent = UtilRequiredKeys<Event, "type"> & {
  type: string;
};

export type OrgEnhetEvent = UtilRequiredKeys<Event, "type"> & {
  type: string;
};

export interface Event {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

export interface Data {
  type: string;
}

export interface FysiskBrukersted {
  requestId?: string;
  brukerstedNr?: string;
  brukerstedNavn?: string;
}

export interface ErrorState {
  /** @format uuid */
  errorId?: string;
  errorStateFid?: string;
  errorStateType?: string;
  errorMessage?: string;
  /** @format date-time */
  createdAt?: string;
  navident?: string;
}

export interface InputMetadata {
  inputInterfaceName?: string;
  inputInterfaceMethodName?: string;
  inputInterfaceMethodArguments?: any;
  origination?: Origination;
}

export type KorrigerGyldigFomEventV1 = OrgEnhetEvent;

export interface Metadata {
  inputMetadata?: InputMetadata;
  /** @format date-time */
  createdTimestamp?: string;
}

export type NomNivaaEndretEventV1 = OrgEnhetEvent;

export type OrgEnhetAvvikletEventV1 = OrgEnhetEvent;

export type OrgEnhetEndretNavnEventV1 = OrgEnhetEvent;

export type OrgEnhetKorrigertEventV1 = OrgEnhetEvent;

export type OrgEnhetLederOpprettetEventV1 = OrgEnhetLederEvent;

export type OrgEnhetLederSlettetEventV1 = OrgEnhetLederEvent;

export type OrgEnhetLederSluttdatoOppdatertEventV1 = OrgEnhetLederEvent;

export type OrgEnhetLederStartdatoOppdatertEventV1 = OrgEnhetLederEvent;

export type OrgEnhetOpprettetEventV1 = OrgEnhetEvent;

export interface OrgEnhetTypeEndretEventData {
  orgEnhetsType?: OrgEnhetsType;
}

export type OrgEnhetTypeEndretEventV1 = OrgEnhetEvent & {
  data?: OrgEnhetTypeEndretEventData;
};

export type OrganiseringAvsluttetEventV1 = OrganiseringEvent;

export type OrganiseringEndretEventV1 = OrganiseringEvent;

export type OrganiseringEndretNavnEventV1 = OrganiseringEvent;

export type OrganiseringOpprettetEventV1 = OrganiseringEvent;

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

export interface TilgangsenhetEndretEventData {
  tilgangsenhetId?: string;
}

export type TilgangsenhetEndretEventV1 = OrgEnhetEvent & {
  data?: TilgangsenhetEndretEventData;
};

export interface NomIntegrasjonEntity {
  entryId?: string;
  entryStatus?: string;
  entryOperasjon?: string;
  entryBestiller?: string;
  fornavn?: string;
  etternavn?: string;
  navId?: string;
  fodselsnummer?: string;
  /** @format int32 */
  sektor?: number;
  enhetsId?: string;
  fysiskBrukerstedId?: string;
  /** @format int64 */
  tiltredelsesdato?: number;
  /** @format int64 */
  utlopsdato?: number;
  mobil?: string;
  direkteTelefon?: string;
  masseflytting?: string;
}
