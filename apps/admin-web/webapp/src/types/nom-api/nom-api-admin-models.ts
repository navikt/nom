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

export interface EnhetsOrganisasjonSanityCheckResponse {
  agressoId?: string;
  id?: string;
  navn?: string;
}

export interface EnhtetMedRessursSomHarSluttetOgErLederSanityCheckResponse {
  enhetsOrganisasjonSanityCheckResponse?: EnhetsOrganisasjonSanityCheckResponse[];
  navident?: string;
  /** @format date */
  sluttdato?: string;
  visningsnavn?: string;
}

export interface OrgtilknytningState {
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
  sektor?: Sektor;
}

export interface OrgtilknytningerUtenforHierarkiResponse {
  alternativOrgenhetId?: string;
  alternativOrgenhetNavn?: string;
  alternativOrgenhetRemedyEnhet?: string;
  faktiskOrgenhetId: string;
  faktiskOrgenhetNavn: string;
  faktiskOrgenhetRemedyEnhet: string;
  navident: string;
  orgtilknytningFid: string;
  /** @format date */
  orgtilknytningGyldigFom: string;
  /** @format date */
  orgtilknytningGyldigTom?: string;
  orgtilknytningSektor?: Sektor;
  orgtilknytningType?: Tilknytningtype;
  ressursSektor: Sektor;
  visningsnavn: string;
}

export interface RobotOrgtilknytningSanityCheckResponse {
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  orgenhetId?: string;
  orgenhetNavn?: string;
}

export interface RobotRessursSanityCheckResponse {
  navident?: string;
  robotOrgtilknytningSanityCheckResponse?: RobotOrgtilknytningSanityCheckResponse[];
  sektor?: Sektor;
  /** @format date */
  sluttdato?: string;
  /** @format date */
  startdato?: string;
  visningsnavn?: string;
}

export enum Sektor {
  NAV_STATLIG = "NAV_STATLIG",
  NAV_KOMMUNAL = "NAV_KOMMUNAL",
  EKSTERN = "EKSTERN",
}

export enum Tilknytningtype {
  ARBEIDSTRENING = "ARBEIDSTRENING",
  ANNET = "ANNET",
  EKSTERN_SAMARBEIDSPARTNER = "EKSTERN_SAMARBEIDSPARTNER",
  FAST = "FAST",
  INGEN = "INGEN",
  JOBBSPESIALIST = "JOBBSPESIALIST",
  KONSULENT = "KONSULENT",
  LAERLING = "LAERLING",
  MIDLERTIDIG = "MIDLERTIDIG",
  PENSJONIST = "PENSJONIST",
  STUDENT = "STUDENT",
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

export interface UpdateTelnrRessursRequest {
  navident: string;
  telefonummer?: string;
}
