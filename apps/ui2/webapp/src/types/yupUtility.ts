import { addYears, isAfter, isBefore } from "date-fns";
import * as yup from "yup";

declare module "yup" {
  interface StringSchema {
    futureOrPresent(): StringSchema;
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

      return isAfter(new Date(dateString), new Date());
    },
  );
});

yup.addMethod(yup.string, "max1YearInTheFuture", function () {
  return this.test(
    "ensure-max-1-year",
    "Kan ikke settes lenger enn 1 år frem i tid",
    function (dateString) {
      if (!dateString) {
        return true;
      }

      return isBefore(new Date(dateString), addYears(new Date(), 1));
    },
  );
});
