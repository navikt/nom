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

export enum AnsettelsesType {
  FAST = "FAST",
  MIDLERTIDIG = "MIDLERTIDIG",
}

export interface OpprettRessursRequest {
  personident: string;
  visningsfornavn: string;
  visningsmellomnavn?: string;
  visningsetternavn: string;
  sektor: Sektor;
  orgEnhetId?: string;
  /** @format date */
  startDato: string;
  /** @format date */
  sluttDato?: string;
  ansettelsesType: AnsettelsesType;
}

export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
}

export interface OpprettRessursResponse {
  personident: string;
  navident: string;
  visningsfornavn: string;
  visningsetternavn: string;
  sektor: Sektor;
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

export interface SetOrgEnhetsTypeRequest {
  nomId: string;
  orgEnhetsType?: OrgEnhetsType;
}

export interface StandardResponse {
  message?: string;
}

export interface SessionInfoResponse {
  navident: string;
  name: string;
  roles: UserRoles[];
}

export enum UserRoles {
  ROLE_WRITE = "ROLE_WRITE",
  ROLE_ADMIN = "ROLE_ADMIN",
}

export interface RessursState {
  fid?: string;
  personident?: string;
  navident?: string;
  sektor?: Sektor;
  visningsnavn?: Visningsnavn;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
}

export interface Visningsnavn {
  fornavn?: string;
  mellomnavn?: string;
  etternavn?: string;
}

export interface FolkeregisterPerson {
  navn: RessursNavn;
}

export interface Person {
  personident: string;
  navident?: string;
  folkeregisterPerson: FolkeregisterPerson;
}

export interface RessursNavn {
  fornavn: string;
  mellomnavn?: string;
  etternavn: string;
}
