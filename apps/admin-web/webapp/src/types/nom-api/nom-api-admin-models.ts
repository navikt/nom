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

export enum IdentType {
  ROBOT = "ROBOT",
  STANDARD = "STANDARD",
  VIKAFOSSEN = "VIKAFOSSEN",
  UTENLANDSK_KONSULENT = "UTENLANDSK_KONSULENT",
  UKJENT = "UKJENT",
}

export interface DuplikatInfo {
  fodselsnrPrefix?: string;
  ressnr?: string;
}

export interface MatchStatistikk {
  /** @format int64 */
  ingenMatch?: number;
  /** @format int64 */
  kunFnrRessnrMismatch?: number;
  /** @format int64 */
  kunRessnrFnrMismatch?: number;
  /** @format int64 */
  ressnrOgFnr?: number;
}

export interface OrgtilknytningInfo {
  /** @format int32 */
  antall?: number;
  harAktiveEllerFremtidige?: boolean;
  sektorer?: Sektor[];
  stater?: OrgtilknytningState[];
  tilknytningstyper?: Tilknytningtype[];
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

export interface OtSlettingStatistikk {
  /** @format int64 */
  antallOter?: number;
  /** @format int64 */
  antallRessurser?: number;
}

export interface OtStatistikk {
  /** @format int64 */
  antallMedAktiveEllerFremtidige?: number;
  /** @format int64 */
  antallMedKunHistoriske?: number;
  /** @format int64 */
  antallUtenOrgtilknytning?: number;
}

export interface Ressurs {
  additionalFysiskeBrukerstederNummer?: string[];
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

export interface UpdateTelnrRessursRequest {
  navident: string;
  telefonummer?: string;
}

export interface UtbetMottakerInnlesningResultat {
  /** @format int64 */
  antallDuplikater?: number;
  /** @format int64 */
  antallTotalt?: number;
  /** @format int64 */
  antallUnike?: number;
  duplikater?: DuplikatInfo[];
  filnavn?: string;
  ikkeOppdaterbare?: OtStatistikk;
  matchStatistikk?: MatchStatistikk;
  mottakere?: UtbetMottakerRad[];
  oppdaterbare?: OtStatistikk;
  otSletting?: OtSlettingStatistikk;
}

export interface UtbetMottakerRad {
  alder?: string;
  ansettelsestypeKode?: string;
  ansettelsestypeTekst?: string;
  duplikat?: boolean;
  fodselsnrPrefix?: string;
  kjonn?: string;
  matchType?:
    | "RESSNR_OG_FNR"
    | "KUN_RESSNR_FNR_MISMATCH"
    | "KUN_FNR_RESSNR_MISMATCH"
    | "INGEN_MATCH";
  navident?: string;
  navn?: string;
  oppdaterbar?: boolean;
  orgenhet?: string;
  orgenhetTekst?: string;
  orgtilknytninger?: OrgtilknytningInfo;
  otFidTilSletting?: string[];
  ressnr?: string;
  resstypeKode?: string;
  resstypeTekst?: string;
  sektor?: Sektor;
  startdatoNav?: string;
  stillingsnummerTekst?: string;
  stkode?: string;
}
