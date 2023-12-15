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

export interface AvvikleRessursRequest {
  /** @format date */
  sluttDato: string;
}

export interface EndreOrgtilknytningDatoRequest {
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
}

export interface FolkeregisterPerson {
  navn: RessursNavn;
}

export interface GetPersonRequest {
  personident: string;
}

export type JsonNode = object;

export interface LeggTilOrgtilknytningRequest {
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  navident: string;
  orgEnhetId: string;
}

export enum NomNivaa {
  LINJEENHET = "LINJEENHET",
  DRIFTSENHET = "DRIFTSENHET",
  ARBEIDSOMRAADE = "ARBEIDSOMRAADE",
}

export interface OpprettRessursRequest {
  ansettelsesType: AnsettelsesType;
  fysiskBrukerstedNr: string;
  orgEnhetId?: string;
  personident: string;
  sektor: Sektor;
  /** @format date */
  sluttDato?: string;
  /** @format date */
  startDato: string;
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
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  id: string;
  ledere: RessursState[];
  navn: string;
  nomNivaa?: NomNivaa;
  orgEnhetsType?: OrgEnhetsType;
  orgNiv: string;
  overordnetEnhetId?: string;
  remedyEnhetId?: string;
  ressursKoblinger: OrgtilknytningState[];
  underordnedeOrgEnhetId: string[];
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

export interface OrgtilknytningState {
  erDagligOppfolging: boolean;
  fid: string;
  /** @format date */
  gyldigFom: string;
  /** @format date */
  gyldigTom?: string;
  navident: string;
  orgEnhetId: string;
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
  navident: string;
  personident: string;
  primaryFysiskBrukersted?: string;
  sektor: Sektor;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  telefoner: Telefon[];
  visningsnavn: Visningsnavn;
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
  navident: string;
  personident: string;
  primaryFysiskBrukersted?: string;
  sektor: Sektor;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  visningsnavn: Visningsnavn;
}

export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
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

export interface Telefon {
  nummer: string;
  type: TelefonType;
}

export enum TelefonType {
  NAV_TJENESTE_TELEFON = "NAV_TJENESTE_TELEFON",
  NAV_KONTOR_TELEFON = "NAV_KONTOR_TELEFON",
  PRIVAT_TELEFON = "PRIVAT_TELEFON",
}

export interface UpdateFysiskeBrukerstederRequest {
  /** @uniqueItems true */
  fysiskeBrukersteder: string[];
  primaryFysiskBrukersted: string;
}

export interface UpdateVisningsnavnRequest {
  etternavn?: string;
  fornavn?: string;
  mellomnavn?: string;
}

export enum UserRoles {
  ROLE_WRITE = "ROLE_WRITE",
  ROLE_ADMIN = "ROLE_ADMIN",
}

export interface Visningsnavn {
  etternavn: string;
  fornavn: string;
  mellomnavn?: string;
}
