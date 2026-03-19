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

import type { OrgEnhet, Visningsnavn } from "~/types/api-models";

/**
 * Very primitive function for checking if we are in a dev or production environment by checking if the hostname includes "localhost" or ".dev."
 */
export function isInDevelopment() {
  const match = globalThis.location.hostname.match(/localhost|\.dev\./);

  return !!match;
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

/**
 * Best effort made to capitalize the name of a fysiskBrukersted.
 * The same as capitalizeName() but not capitalizing words like "vei" and "gate".
 */
export function capitalizeNameFysiskBrukersted(name: string) {
  const litenForbokstavOrd = [
    "vei",
    "gate",
    "plass",
    "alle",
    "allé",
    "sti",
    "terrasse",
    "bakke",
    "bru",
    "brygge",
    "tun",
    "kai",
    "park",
    "hage",
    "torg",
    "bygg",
    "etg",
    "etasje",
  ];

  let storeForbokstaver = capitalizeName(
    name.toLowerCase().replaceAll("allè", "allé"),
  );

  // Bruker RegEx som finner tilfeller av ordene som skal ha liten forbokstav
  // som står alene og bytter dem ut med samme ord med liten forbokstav
  for (const word of litenForbokstavOrd) {
    const wordRegex = new RegExp(
      `(^|[\\s-])${capitalizeName(word)}(?=\\s|$|-)`,
      "g",
    );

    storeForbokstaver = storeForbokstaver.replace(
      wordRegex,
      (_, prefix) => `${prefix}${word}`,
    );
  }

  return storeForbokstaver;
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

export function orgEnhetLederNavn(orgEnhet: OrgEnhet) {
  return formatVisningsnavn(orgEnhet.ledere[0].visningsnavn);
}

export function formatTlf(nummer: string) {
  let temporaryNumber = nummer;

  if (nummer.startsWith("47") && nummer.length === 10) {
    temporaryNumber = nummer.slice(2);
  } else if (nummer.startsWith("+47") && nummer.length === 11) {
    temporaryNumber = nummer.slice(3);
  } else if (nummer.startsWith("0047") && nummer.length === 12) {
    temporaryNumber = nummer.slice(4);
  }

  if (/^\d{8}$/.test(temporaryNumber)) {
    const out = [];
    for (let k = 0; k < 8; k += 2) {
      out.push(temporaryNumber.slice(k, k + 2));
    }

    return out.join(" ");
  }

  return nummer;
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

export function formatDisplayNumber(number: number | string) {
  if (!number) return "";

  const cleanedNumber = number.toString().replaceAll(/\D/g, "");

  return cleanedNumber.replaceAll(/\B(?=(\d{3})+(?!\d))/g, " ");
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

export function isTodayOrBefore(date: Date | string) {
  const providedDateWithoutTimezone = dateWithoutTimezone(date);

  return (
    isToday(providedDateWithoutTimezone) ||
    isBefore(providedDateWithoutTimezone, todayWithoutTimezone())
  );
}

export function isTodayOrAfter(date: Date | string | undefined) {
  if (!date) {
    return true;
  }

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

export function isSameDateOrAfter(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return (
    isSameDate(checkDate, compareDate) ||
    isAfter(dateWithoutTimezone(checkDate), dateWithoutTimezone(compareDate))
  );
}

export function isSameDateOrBefore(checkDate: Date, compareDate: Date) {
  return (
    isSameDate(checkDate, compareDate) ||
    isBefore(dateWithoutTimezone(checkDate), dateWithoutTimezone(compareDate))
  );
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

export function navToppnode() {
  return isInDevelopment() ? "sa312u" : "py437s";
}

export function isHelfoOrgEnhet(orgEnhetId: string) {
  const helfoId = isInDevelopment() ? "mu405g" : "je305h";

  return orgEnhetId === helfoId;
}

export function isSkatteetatenOrgEnhet(orgEnhetId: string) {
  const skatteetatenId = isInDevelopment() ? "ja464g" : "ne938h";

  return orgEnhetId === skatteetatenId;
}

export function isVikafossenOrgEnhet(orgEnhetId: string) {
  const vikafossenId = isInDevelopment() ? "ru151s" : "ru664y";

  return orgEnhetId === vikafossenId;
}

export function isRiksrevisjonenOrgEnhet(orgEnhetId: string) {
  const riksrevisjonenId = isInDevelopment() ? "ge146b" : "ke602d";

  return orgEnhetId === riksrevisjonenId;
}

export function isEksternSamarbeidspartnerOrgEnhet(orgEnhetId: string) {
  return (
    isHelfoOrgEnhet(orgEnhetId) ||
    isSkatteetatenOrgEnhet(orgEnhetId) ||
    isVikafossenOrgEnhet(orgEnhetId) ||
    isRiksrevisjonenOrgEnhet(orgEnhetId)
  );
}

export const skjulteOrgtilknytninger = [
  "orgtilknytning-01JR805KB6RNN7TDFP1NC4J1XG",
];

export function isDecember() {
  return new Date().getMonth() === 11;
}
