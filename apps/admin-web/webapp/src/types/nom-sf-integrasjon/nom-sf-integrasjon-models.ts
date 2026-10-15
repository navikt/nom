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

export interface Leder {
  navident?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
}

export interface OverOrgEnhet {
  id?: string;
  nivaa?: NomNivaa;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
}

export interface SfOrgEnhet {
  id?: string;
  navn?: string;
  nivaa?: NomNivaa;
  erLovnode?: boolean;
  type?: OrgEnhetsType;
  tilgangsenhet?: string;
  kostnadssted?: string;
  /** @format date */
  gyldigFom?: string;
  /** @format date */
  gyldigTom?: string;
  ledere?: Leder[];
  overOrgEnheter?: OverOrgEnhet[];
}

export interface SfOutboxResponse {
  /** @format int64 */
  sporingsId: number;
  sfStatus: "IKKE_SENDT" | "SENDT" | "FEILET" | "AVVIST" | "ERSTATTET";
  feilmelding?: string;
  orgEnhetId: string;
  /** @uniqueItems true */
  endretAv?: string[];
  /** @uniqueItems true */
  endringshendelser?: string[];
  /** Orgenheten slik den ligger i outboxen. Avviste rader kan ha JSON som ikke følger SfOrgEnhet. */
  orgenhet: SfOrgEnhet;
}
