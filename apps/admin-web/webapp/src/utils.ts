import { format } from "date-fns";

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
