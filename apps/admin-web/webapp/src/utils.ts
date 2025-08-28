import { format, formatISO, isAfter, isBefore, isToday } from "date-fns";

import { exportSanityCheckToExcelOptions } from "~/api/queries.ts";
import type { Visningsnavn } from "~/types.ts";

export const DATE_FORMATS = {
  no: "dd.MM.yyyy",
  noLong: "dd.MM.yyyy HH:mm",
};

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

export function formatDisplayDatetime(dateTime?: string) {
  if (!dateTime) {
    return "";
  }
  return format(new Date(dateTime), DATE_FORMATS.noLong);
}

export function formatDateWithoutTimezone(date: Date) {
  return formatISO(date, { representation: "date" });
}

export function dateWithoutTimezone(date: Date) {
  return new Date(formatDateWithoutTimezone(date));
}

export function todayWithoutTimezone() {
  return dateWithoutTimezone(new Date());
}

export function isTodayOrBefore(date: Date) {
  const noTzDate = dateWithoutTimezone(date);

  return isToday(noTzDate) || isBefore(noTzDate, todayWithoutTimezone());
}

export function isTodayOrAfter(date: Date) {
  const noTzDate = dateWithoutTimezone(date);

  return isToday(noTzDate) || isAfter(noTzDate, todayWithoutTimezone());
}

export const exportSanityCheckToExcel = async (
  exportType: "orgEnhet" | "ressurs" | "orgtilknytning" | "RemedyCompare",
  exportName: string | undefined,
) => {
  await exportSanityCheckToExcelOptions(exportType, exportName);
};
