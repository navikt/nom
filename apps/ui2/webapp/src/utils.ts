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

export function dateWithoutTimezone(date: Date | string) {
  return new Date(formatDateWithoutTimezone(new Date(date)));
}

export function todayWithoutTimezone() {
  return dateWithoutTimezone(new Date());
}

export function formatDisplayDate(date?: Date | string) {
  if (!date) {
    return "";
  }
  return format(dateWithoutTimezone(date), DATE_FORMATS.no);
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

export function isPeriodActive(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return (
    isTodayOrBefore(gyldigFom) && (!gyldigTom || isTodayOrAfter(gyldigTom))
  );
}

export function isPeriodPast(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return isBeforeToday(gyldigFom) && gyldigTom && isBeforeToday(gyldigFom);
}

export function isPeriodFuture(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return isAfterToday(gyldigFom) && (!gyldigTom || isAfterToday(gyldigFom));
}

export function isPeriodActiveOrPast(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return (
    isPeriodActive(gyldigFom, gyldigTom) || isPeriodPast(gyldigFom, gyldigTom)
  );
}

export function isPeriodActiveOrFuture(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return (
    isPeriodActive(gyldigFom, gyldigTom) || isPeriodFuture(gyldigFom, gyldigTom)
  );
}

export function isTodayOrBefore(date: Date | string) {
  const providedDateWithoutTimezone = dateWithoutTimezone(date);

  return (
    isToday(providedDateWithoutTimezone) ||
    isBefore(providedDateWithoutTimezone, todayWithoutTimezone())
  );
}

export function isTodayOrAfter(date: Date | string) {
  const providedDateWithoutTimezone = dateWithoutTimezone(date);

  return (
    isToday(providedDateWithoutTimezone) ||
    isAfter(providedDateWithoutTimezone, todayWithoutTimezone())
  );
}

export function isBeforeDate(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return isBefore(
    dateWithoutTimezone(checkDate),
    dateWithoutTimezone(compareDate),
  );
}

export function isAfterDate(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return isAfter(
    dateWithoutTimezone(checkDate),
    dateWithoutTimezone(compareDate),
  );
}

export function isBeforeToday(date: Date | string) {
  return !isTodayOrAfter(dateWithoutTimezone(date));
}

export function isAfterToday(date: Date | string) {
  return !isTodayOrBefore(dateWithoutTimezone(date));
}

export function isSameDate(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return isSameDay(
    dateWithoutTimezone(new Date(checkDate)),
    dateWithoutTimezone(new Date(compareDate)),
  );
}

export function isSameDateOrAfter(checkDate: Date, compareDate: Date) {
  return isSameDate(checkDate, compareDate) || isAfter(checkDate, compareDate);
}

export function isSameDateOrBefore(checkDate: Date, compareDate: Date) {
  return isSameDate(checkDate, compareDate) || isBefore(checkDate, compareDate);
}

export function earliestDateInList(dates: (Date | string | undefined)[]) {
  const dateDates = dates.map((date) =>
    date ? dateWithoutTimezone(new Date(date)) : undefined,
  );

  const sortedDates = dateDates.sort((a, b) => {
    if (!a && !b) {
      return 0;
    }

    if (!a) {
      return 1;
    }
    if (!b) {
      return -1;
    }

    if (isSameDay(a, b)) {
      return 0;
    }

    return isSameDateOrBefore(a, b) ? -1 : 1;
  });

  return sortedDates[0];
}

export function earliestDate(date1: Date, date2: Date) {
  return isSameDateOrBefore(date1, date2) ? date1 : date2;
}

export function latestDate(date1: Date, date2: Date) {
  return isSameDateOrAfter(date1, date2) ? date1 : date2;
}

export function promiseIsFulfilled<T>(
  promise: PromiseSettledResult<T>,
): promise is PromiseFulfilledResult<T> {
  return promise.status === "fulfilled";
}

export function promiseIsRejected<T>(
  promise: PromiseSettledResult<T>,
): promise is PromiseRejectedResult {
  return promise.status === "rejected";
}
