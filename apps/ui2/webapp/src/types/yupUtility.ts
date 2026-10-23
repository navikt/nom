import { addYears } from "date-fns";
import * as yup from "yup";

import { dateIsBefore } from "~/customUtils/dateUtils";
import { Sektor, Tilknytningtype } from "~/types/api-models";
import {
  dateWithoutTimezone,
  isSameDateOrAfter,
  isTodayOrAfter,
  todayWithoutTimezone,
} from "~/utils";

declare module "yup" {
  interface StringSchema {
    futureOrPresent(): StringSchema;
    optionalPhoneNumber(): StringSchema;
    max1YearInTheFuture(): StringSchema;
  }
}

yup.addMethod(yup.string, "futureOrPresent", function () {
  return this.test(
    "futureOrPresent",
    "Kan ikke være i fortiden",
    function (dateString) {
      if (!dateString) {
        return true;
      }

      return isTodayOrAfter(new Date(dateString));
    },
  );
});

yup.addMethod(yup.string, "optionalPhoneNumber", function () {
  return this.test(
    "optional-phone-number",
    "Ikke gyldig telefonnummer",
    function (nummer) {
      if (nummer?.length === 0) {
        return true;
      }

      const nummerUtenSpaces = nummer?.replaceAll(" ", "");

      // Enten 8 siffer langt, ellers kreves "+" or "00" på starten
      const telefonnummerRegex = /^(\d{8}|(\+|00)\d{5,21})$/;

      return telefonnummerRegex.test(nummerUtenSpaces ?? "");
    },
  );
});

yup.addMethod(yup.string, "max1YearInTheFuture", function () {
  return this.test(
    "ensure-max-1-year",
    "Må være under ett år frem i tid fra dagens dato",
    function (dateString) {
      if (!dateString) {
        return true;
      }

      return !isSameDateOrAfter(
        dateWithoutTimezone(dateString),
        dateWithoutTimezone(addYears(todayWithoutTimezone(), 1)),
      );
    },
  );
});

export const validateFormDate = (
  value: string | undefined | null,
  context: yup.TestContext<yup.AnyObject>,
  sektor: Sektor,
  tilknytningtype: Tilknytningtype,
  alltidObligatorisk: boolean,
) => {
  const obligatoriskTom =
    sektor === Sektor.EKSTERN &&
    tilknytningtype !== Tilknytningtype.HELFO &&
    tilknytningtype !== Tilknytningtype.SKATTEETATEN;

  const maxDate = dateWithoutTimezone(
    addYears(todayWithoutTimezone(), obligatoriskTom ? 1 : 6),
  );

  if ((obligatoriskTom || alltidObligatorisk) && !value) {
    return context.createError({
      message: "Obligatorisk",
    });
  }

  if (value && !dateIsBefore(dateWithoutTimezone(value), maxDate)) {
    return context.createError({
      message: buildDateErrorMessage(obligatoriskTom, alltidObligatorisk),
    });
  }

  return true;
};

function buildDateErrorMessage(
  obligatoriskTom: boolean,
  alltidObligatorisk: boolean,
): string {
  const base = `Må være under ${obligatoriskTom ? "ett" : "seks"} år frem i tid fra dagens dato`;

  if (obligatoriskTom) {
    return `${base} for eksterne medarbeidere`;
  }

  if (!alltidObligatorisk) {
    return `${base}, eller ikke satt`;
  }

  return base;
}
