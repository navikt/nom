import { addYears } from "date-fns";
import * as yup from "yup";

import {
  dateWithoutTimezone,
  isSameDateOrAfter,
  isTodayOrAfter,
  todayWithoutTimezone,
} from "~/utils";

declare module "yup" {
  interface StringSchema {
    futureOrPresent(): StringSchema;
    max1YearInTheFuture(): StringSchema;
    optionalPhoneNumber(): StringSchema;
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
