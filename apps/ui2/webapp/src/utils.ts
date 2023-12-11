import {
  endOfDay,
  format,
  formatISO,
  isAfter,
  isBefore,
  isWithinInterval,
  maxTime,
  minTime,
} from "date-fns";

import type { Visningsnavn } from "~/types/api-models";

/**
 * Very primitive function for checking if we are in a dev or production environment by checking if the hostname includes "localhost" or ".dev."
 */
export function isInDevelopment() {
  return window.location.hostname.match(/localhost|\.dev\./);
}

/**
 * Best effort made to capitalize a name.
 * Every letter after a space or "-" is capitalized.
 */
export function capitalizeName(name: string) {
  let nameInProgress = "";

  for (const currentChar of name.toLowerCase()) {
    const lastChar = nameInProgress.slice(-1);
    nameInProgress =
      lastChar === " " || lastChar === "-" || lastChar === ""
        ? nameInProgress + currentChar.toUpperCase()
        : nameInProgress + currentChar;
  }

  return nameInProgress;
}

export function formatVisningsnavn(visningsNavn?: Visningsnavn) {
  if (!visningsNavn) {
    return "";
  }
  return [
    visningsNavn.fornavn,
    visningsNavn.mellomnavn ?? "",
    visningsNavn.etternavn,
  ].join(" ");
}

export function formatTlf(nr: string) {
  const temporaryNumber =
    nr.startsWith("47") && nr.length === 10 ? nr.slice(2) : nr;
  const out = [];
  for (let k = 0; k < 8; k += 2) {
    out.push(temporaryNumber.slice(k, k + 2));
  }
  return out.join(" ");
}

export const DATE_FORMATS = {
  no: "dd.MM.yyyy",
  noLong: "dd.MM.yyyy HH:MM",
};

export function formatDateWithoutTimezone(date: Date) {
  return formatISO(date, { representation: "date" });
}

export function formatDisplayDate(date?: string) {
  if (!date) {
    return "";
  }
  return format(new Date(date), DATE_FORMATS.no);
}

export function formatDisplayDatetime(dateTime?: string) {
  if (!dateTime) {
    return "";
  }
  return format(new Date(dateTime), DATE_FORMATS.noLong);
}

export function isGyldighetsPeriodeActive(
  gyldigFom: string | undefined,
  gyldigTom: string | undefined,
) {
  const gyldigFomAsDate = gyldigFom
    ? endOfDay(new Date(gyldigFom))
    : new Date(minTime);
  const gyldigTomAsDate = gyldigTom
    ? endOfDay(new Date(gyldigTom))
    : new Date(maxTime);

  const endOfToday = endOfDay(new Date());

  try {
    return isWithinInterval(endOfToday, {
      start: gyldigFomAsDate,
      end: gyldigTomAsDate,
    });
  } catch {
    return false;
  }
}

export function isBeforePresent(date: Date) {
  const endOfDate = endOfDay(date);
  const endOfToday = endOfDay(new Date());

  return isBefore(endOfDate, endOfToday);
}

export function isAfterPresent(date: Date) {
  const endOfDate = endOfDay(date);
  const endOfToday = endOfDay(new Date());

  return isAfter(endOfDate, endOfToday);
}
