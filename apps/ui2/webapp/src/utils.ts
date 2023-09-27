import {
  format,
  formatISO,
  isWithinInterval,
  maxTime,
  minTime,
} from "date-fns";

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
};

export function formatDateWithoutTimezone(date: Date) {
  return formatISO(date, { representation: "date" });
}

export function formatDisplayDate(date: string) {
  return format(new Date(date), DATE_FORMATS.no);
}

export function isGyldighetsPeriodeActive(
  gyldigFom: string | undefined,
  gyldigTom: string | undefined,
) {
  const gyldigFomAsDate = gyldigFom ? new Date(gyldigFom) : new Date(minTime);
  const gyldigTomAsDate = gyldigTom ? new Date(gyldigTom) : new Date(maxTime);

  return isWithinInterval(new Date(), {
    start: gyldigFomAsDate,
    end: gyldigTomAsDate,
  });
}
