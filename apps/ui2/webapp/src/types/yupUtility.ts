import { addYears, isBefore } from "date-fns";
import * as yup from "yup";

import { isPresentOrAfter } from "~/utils";

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

      return isPresentOrAfter(new Date(dateString));
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

      return isBefore(new Date(dateString), addYears(new Date(), 1));
    },
  );
});

yup.addMethod(yup.string, "optionalPhoneNumber", function () {
  return this.test(
    "optional-phone-number",
    "Må være 8 siffer",
    function (nummer) {
      if (nummer?.length === 0) {
        return true;
      }

      const enkelTelefonnummerRegex = /^\d{8}$/;

      return enkelTelefonnummerRegex.test(nummer ?? "");
    },
  );
});
