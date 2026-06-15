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

export interface OrgtilknytningerUtenforHierarkiResponse {
  /** @default "" */
  alternativOrgenhetId?: string;
  /** @default "" */
  alternativOrgenhetNavn?: string;
  /** @default "" */
  alternativOrgenhetRemedyEnhet?: string;
  /** @default "" */
  faktiskOrgenhetId: string;
  /** @default "" */
  faktiskOrgenhetNavn: string;
  /** @default "" */
  faktiskOrgenhetRemedyEnhet: string;
  /** @default "" */
  navident: string;
  /** @default "" */
  orgtilknytningFid: string;
  /**
   * @format date
   * @default ""
   */
  orgtilknytningGyldigFom: string;
  /**
   * @format date
   * @default ""
   */
  orgtilknytningGyldigTom?: string;
  /** @default "" */
  orgtilknytningSektor?: Sektor;
  /** @default "" */
  orgtilknytningType?: Tilknytningtype;
  /** @default "" */
  ressursSektor: Sektor;
  /** @default "" */
  visningsnavn: string;
}

export interface Ressurs {
  etternavn?: string;
  fornavn?: string;
  gjeldendeSektor?: Sektor;
  gjeldendeTilknytningtype?: Tilknytningtype;
  identType?: IdentType;
  /** @deprecated */
  navIdent?: string;
  navident?: string;
  /** @deprecated */
  personIdent?: string;
  personident?: string;
  primaryFysiskBrukersted?: string;
  primaryTelefon?: string;
  /** @deprecated */
  sektor?: Sektor[];
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  telefon?: Telefon[];
  /** @deprecated */
  visningsNavn?: string;
  visningsnavn?: string;
}

export interface RessursWithMetadataResult {
  code?: "OK" | "NOT_FOUND" | "NOT_IN_FILTER" | "ERROR";
  endretAv?: string;
  /** @format date-time */
  endretDato?: string;
  endringEvent?: string;
  id?: string;
  opprettetAv?: string;
  /** @format date-time */
  opprettetDato?: string;
  ressurs?: Ressurs;
}

export interface StandardResponse {
  /** @default "" */
  message: string;
}

export interface Telefon {
  beskrivelse?: string;
  nummer?: string;
  type?: TelefonType;
}

export interface TilgangsenhetEndring {
  orgEnhetId?: string;
  tilgangsenhetId?: string;
}

export interface U4Diff {
  /** @format int32 */
  id?: number;
  kilde?: "STILLINGER" | "RESSURS";
  navident: string;
  nomDol?: boolean;
  /** @format date */
  nomGyldigFom?: string;
  /** @format date */
  nomGyldigTom?: string;
  nomOrgenhetId?: string;
  nomOrgenhetNavn?: string;
  nomOrgtilknytning?: string;
  nomOtSektor?: Sektor;
  nomSektor?: Sektor;
  type?:
    | "MANGLENDE_U4_PERIODE"
    | "EKSTRA_U4_PERIODE"
    | "FORSKJELL_I_U4_PERIODE"
    | "MANGLENDE_STATLIG_I_U4"
    | "ULIK_SEKTOR_RESSURS";
  u4Dol?: boolean;
  /** @format date */
  u4GyldigFom?: string;
  /** @format date */
  u4GyldigTom?: string;
  u4OrgenhetId?: string;
  u4OrgenhetNavn?: string;
  u4RessursAnsettelsestypeBesk?: string;
  u4RessursType?: string;
  u4SektorPaaRessurs?: Sektor;
  u4SektorPaaStilling?: Sektor;
  u4StillingAnsettelsestypeBesk?: string;
  u4StillingPeriode?: string;
}
