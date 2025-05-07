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
