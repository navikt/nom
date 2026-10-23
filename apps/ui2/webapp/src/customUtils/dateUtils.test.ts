import { addDays, subDays } from "date-fns";
import { describe, expect, test } from "vitest";

import {
  dateIsAfter,
  dateIsAfterToday,
  dateIsBefore,
  dateIsBeforeToday,
  dateIsSame,
  dateIsSameOrAfter,
  dateIsSameOrBefore,
  dateIsToday,
  dateIsTodayOrAfter,
  dateIsTodayOrBefore,
  dateWithoutTimezone,
  earliestDate,
  latestDate,
} from "~/customUtils/dateUtils";

describe("dateWithoutTimezone()", () => {
  test("Fulle Date-objekter håndteres riktig", () => {
    const now = new Date();

    const input = dateWithoutTimezone(now);

    const expected = new Date(
      Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()),
    );

    expect(input).toEqual(expected);
  });

  test("Tid blir fjernet", () => {
    const testDate = new Date("2024-01-01T08:24:00");

    const input = dateWithoutTimezone(testDate);

    const expected = new Date("2024-01-01");

    expect(input).toEqual(expected);
  });

  test("Negativ tidssone blir normalisert", () => {
    const testDate = new Date("2024-01-01T00:24:00-02:00");

    const input = dateWithoutTimezone(testDate);

    const expected = new Date("2024-01-01");

    expect(input).toEqual(expected);
  });

  test("Positiv tidssone blir normalisert", () => {
    const testDate = new Date("2024-01-01T00:24:00+02:00");

    const input = dateWithoutTimezone(testDate);

    const expected = new Date("2023-12-31");

    expect(input).toEqual(expected);
  });

  test("Dato nær midnatt håndteres riktig", () => {
    const testDate = new Date("2024-01-01T23:59:59");

    const input = dateWithoutTimezone(testDate);

    const expected = new Date("2024-01-01");

    expect(input).toEqual(expected);
  });

  test("Skuddår håndteres riktig", () => {
    const testDate = new Date("2024-02-29T08:24:00");

    const input = dateWithoutTimezone(testDate);

    const expected = new Date("2024-02-29");

    expect(input).toEqual(expected);
  });

  test("Tidssone -12:00 blir normalisert", () => {
    const testDate = new Date("2024-01-01T00:24:00-12:00");

    const input = dateWithoutTimezone(testDate);

    const expected = new Date("2024-01-01");

    expect(input).toEqual(expected);
  });

  test("Tidssone +14:00 blir normalisert", () => {
    const testDate = new Date("2024-01-01T00:24:00+14:00");

    const input = dateWithoutTimezone(testDate);

    const expected = new Date("2023-12-31");

    expect(input).toEqual(expected);
  });
});

describe("dateIsSame(Date, Date)", () => {
  test("To av samme dato behandles som likt", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-01");

    const input = dateIsSame(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("To ulike datoer behandles som ulike", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-02");

    const input = dateIsSame(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("String som dato", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = "2024-01-01";

    const input = dateIsSame(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Ulike klokkeslett på samme dato behandles som like", () => {
    const testDate1 = new Date("2024-01-01T10:00:00");
    const testDate2 = new Date("2024-01-01T11:00:00");

    const input = dateIsSame(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Fulle Date-objekter behandles riktig", () => {
    const testDate1 = new Date();
    const testDate2 = subDays(new Date("2024-01-01T11:00:00"), 1);

    const input = dateIsSame(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Datoer i forskjellige tidssoner behandles som like", () => {
    const testDate1 = new Date("2024-01-01T08:00:00+02:00");
    const testDate2 = new Date("2024-01-01T09:00:00-02:00");

    const input = dateIsSame(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("dateIsSameOrBefore(Date, Date)", () => {
  test("Like datoer -> true", () => {
    const testDate1 = new Date("2024-01-10");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsSameOrBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> true", () => {
    const testDate1 = new Date("2024-01-09");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsSameOrBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> false", () => {
    const testDate1 = new Date("2024-01-11");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsSameOrBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Ulike klokkeslett på samme dato -> true", () => {
    const testDate1 = new Date("2024-01-10T07:00:00");
    const testDate2 = new Date("2024-01-10T09:00:00");

    const input = dateIsSameOrBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("dateIsSameOrAfter(Date, Date)", () => {
  test("Like datoer -> true", () => {
    const testDate1 = new Date("2024-01-10");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsSameOrAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> false", () => {
    const testDate1 = new Date("2024-01-09");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsSameOrAfter(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> true", () => {
    const testDate1 = new Date("2024-01-11");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsSameOrAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Ulike klokkeslett på samme dato -> true", () => {
    const testDate1 = new Date("2024-01-10T07:00:00");
    const testDate2 = new Date("2024-01-10T09:00:00");

    const input = dateIsSameOrAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("dateIsBefore(Date, Date)", () => {
  test("Like datoer -> false", () => {
    const testDate1 = new Date("2024-01-10");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> true", () => {
    const testDate1 = new Date("2024-01-09");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> false", () => {
    const testDate1 = new Date("2024-01-11");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("dateIsAfter(Date, Date)", () => {
  test("Like datoer -> false", () => {
    const testDate1 = new Date("2024-01-10");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsAfter(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> false", () => {
    const testDate1 = new Date("2024-01-09");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsAfter(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> true", () => {
    const testDate1 = new Date("2024-01-11");
    const testDate2 = new Date("2024-01-10");

    const input = dateIsAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("dateIsToday(Date)", () => {
  test("I dag -> true", () => {
    const testDate = new Date();

    const input = dateIsToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> false", () => {
    const testDate = subDays(new Date(), 1);

    const input = dateIsToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> false", () => {
    const testDate = addDays(new Date(), 1);

    const input = dateIsToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("dateIsTodayOrBefore(Date)", () => {
  test("I dag -> true", () => {
    const testDate = new Date();

    const input = dateIsTodayOrBefore(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> true", () => {
    const testDate = subDays(new Date(), 1);

    const input = dateIsTodayOrBefore(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> false", () => {
    const testDate = addDays(new Date(), 1);

    const input = dateIsTodayOrBefore(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("dateIsTodayOrAfter(Date)", () => {
  test("I dag -> true", () => {
    const testDate = new Date();

    const input = dateIsTodayOrAfter(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> false", () => {
    const testDate = subDays(new Date(), 1);

    const input = dateIsTodayOrAfter(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> true", () => {
    const testDate = addDays(new Date(), 1);

    const input = dateIsTodayOrAfter(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("dateIsBeforeToday(Date)", () => {
  test("I dag -> false", () => {
    const testDate = new Date();

    const input = dateIsBeforeToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> true", () => {
    const testDate = subDays(new Date(), 1);

    const input = dateIsBeforeToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> false", () => {
    const testDate = addDays(new Date(), 1);

    const input = dateIsBeforeToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("dateIsAfterToday(Date)", () => {
  test("I dag -> false", () => {
    const testDate = new Date();

    const input = dateIsAfterToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Tidligere dato -> false", () => {
    const testDate = subDays(new Date(), 1);

    const input = dateIsAfterToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Senere dato -> true", () => {
    const testDate = addDays(new Date(), 1);

    const input = dateIsAfterToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("earliestDate(Date, Date)", () => {
  test("Like", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-01");

    const input = earliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });

  test("Like datoer, ulikt klokkeslett", () => {
    const testDate1 = new Date("2024-01-01T08:00:00");
    const testDate2 = new Date("2024-01-01T09:00:00");

    const input = earliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });

  test("Ulik dato", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-02");

    const input = earliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });

  test("Ulik dato motsatt", () => {
    const testDate1 = new Date("2024-01-02");
    const testDate2 = new Date("2024-01-01");

    const input = earliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate2);

    expect(input).toEqual(expected);
  });

  test("String som dato", () => {
    const testDate1 = "2024-01-02";
    const testDate2 = new Date("2024-01-01");

    const input = earliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate2);

    expect(input).toEqual(expected);
  });
});

describe("latestDate(Date, Date)", () => {
  test("Like", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-01");

    const input = latestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });

  test("Like datoer, ulikt klokkeslett", () => {
    const testDate1 = new Date("2024-01-01T08:00:00");
    const testDate2 = new Date("2024-01-01T09:00:00");

    const input = latestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });

  test("Ulik dato", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-02");

    const input = latestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate2);

    expect(input).toEqual(expected);
  });

  test("Ulik dato motsatt", () => {
    const testDate1 = new Date("2024-01-02");
    const testDate2 = new Date("2024-01-01");

    const input = latestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });

  test("String som dato", () => {
    const testDate1 = "2024-01-02";
    const testDate2 = new Date("2024-01-01");

    const input = latestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });
});
