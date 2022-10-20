export const sortItems = (a: string, b: string) => {
  if (a.localeCompare(b, "no") < 0) {
    return -1;
  } else if (a.localeCompare(b, "no") > 0) {
    return 1;
  }
  return 0;
};

export function structuredClone<T>(arg: T): T {
  // @typescript-eslint/ban-ts-comment
  const cloneFunc = window.structuredClone;
  if (cloneFunc) {
    return cloneFunc(arg);
  } else {
    return JSON.parse(JSON.stringify(arg));
  }
}

export function funcIsNotDuplicateByKey<T>(extractKey: (it: T) => string) {
  return (it: T, idx: number, arrCtx: T[]) =>
    isNotDuplicate<T>(it, idx, arrCtx, extractKey);
}

export function isNotDuplicate<T>(
  it: T,
  idx: number,
  arrCtx: T[],
  extractKey: (it: T) => string
): boolean {
  const seen = arrCtx.slice(0, idx).map(extractKey);
  const wasDuplicate = seen.includes(extractKey(it));
  if (wasDuplicate) {
    console.warn(
      { arrCtx, dupKey: extractKey(it), idx },
      "Duplicate removed: Key was seen more than once -> "
    );
  }
  return !wasDuplicate;
}

/**
 * Very primitive function for checking if we are in a dev or production environment by checking if the hostname includes "localhost" or ".dev."
 */
export function isInDev() {
  return window.location.hostname.match(/localhost|\.dev\./);
}
