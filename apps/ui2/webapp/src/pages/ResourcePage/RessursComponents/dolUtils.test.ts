import { describe, expect, test } from "vitest";

import { findPeriodGaps } from "~/pages/ResourcePage/RessursComponents/dolUtils";

describe("findPeriodGaps", () => {
  test("Sjekk at ingen orgtilknytninger gir ingen gaps", () => {
    expect(findPeriodGaps([])).toEqual([]);
  });
});
