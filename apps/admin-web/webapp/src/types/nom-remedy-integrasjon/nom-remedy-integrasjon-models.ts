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

export interface Data {
  type: string;
}

export interface Event {
  fid?: string;
  data?: Data;
  metadata?: Metadata;
  additionalData?: Data;
  type: string;
}

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

export type OrgtilknytningAvsluttetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDagligOppfolgingEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDatoEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningDatoEndretEventV2 = OrgtilknytningEvent;

export type OrgtilknytningEvent = UtilRequiredKeys<Event, "type"> & {
  type: string;
};

export type OrgtilknytningFjernetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningOpprettetEventV1 = OrgtilknytningEvent;

export type OrgtilknytningSektorEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningTilknytningtypeEndretEventV1 = OrgtilknytningEvent;

export type OrgtilknytningTypeEndretEventV1 = OrgtilknytningEvent;

export interface Origination {
  initiatedBy?: string;
  initiatedFrom?: string;
}

export type RessursEvent = UtilRequiredKeys<Event, "type"> & {
  type: string;
};

export type RessursJobbtelefonOppdatertEventV1 = RessursEvent;

export type RessursSluttdatoOppdatertEventV1 = RessursEvent;

export type RessursSluttetEventV1 = RessursEvent;

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
