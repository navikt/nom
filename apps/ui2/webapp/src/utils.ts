import {
  endOfDay,
  format,
  formatISO,
  isAfter,
  isBefore,
  isSameDay,
  isToday,
  isWithinInterval,
} from "date-fns";
import { maxTime, minTime } from "date-fns/constants";

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

export function dateWithoutTimezone(date: Date) {
  return new Date(formatDateWithoutTimezone(date));
}

export function todayWithoutTimezone() {
  return dateWithoutTimezone(new Date());
}

export function formatDisplayDate(date?: string | Date) {
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

export function isTodayOrBefore(date: Date) {
  return isToday(date) || isBefore(date, todayWithoutTimezone());
}

export function isTodayOrAfter(date: Date) {
  return isToday(date) || isAfter(date, todayWithoutTimezone());
}

export function isBeforeToday(date: Date) {
  return !isTodayOrAfter(date);
}

export function isAfterToday(date: Date) {
  return !isTodayOrBefore(date);
}

export function isSameDateOrAfter(checkDate: Date, compareDate: Date) {
  return isSameDay(checkDate, compareDate) || isAfter(checkDate, compareDate);
}

export function isSameDateOrBefore(checkDate: Date, compareDate: Date) {
  return isSameDay(checkDate, compareDate) || isBefore(checkDate, compareDate);
}

export function earliestDate(date1: Date, date2: Date) {
  return isSameDateOrBefore(date1, date2) ? date1 : date2;
}

export function latestDate(date1: Date, date2: Date) {
  return isSameDateOrAfter(date1, date2) ? date1 : date2;
}
