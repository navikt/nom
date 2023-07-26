import { format, formatISO } from "date-fns";

export const sortItems = (a: string, b: string) => {
  if (a.localeCompare(b, "no") < 0) {
    return -1;
  } else if (a.localeCompare(b, "no") > 0) {
    return 1;
  }
  return 0;
};

export function functionIsNotDuplicateByKey<T>(extractKey: (it: T) => string) {
  return (it: T, index: number, arrayContext: T[]) =>
    isNotDuplicate<T>(it, index, arrayContext, extractKey);
}

export function isNotDuplicate<T>(
  it: T,
  index: number,
  arrayContext: T[],
  extractKey: (it: T) => string,
): boolean {
  const seen = arrayContext
    .slice(0, index)
    .map((element) => extractKey(element));
  const wasDuplicate = seen.includes(extractKey(it));
  if (wasDuplicate) {
    // eslint-disable-next-line no-console
    console.warn(
      { arrCtx: arrayContext, dupKey: extractKey(it), idx: index },
      "Duplicate removed: Key was seen more than once -> ",
    );
  }
  return !wasDuplicate;
}

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
