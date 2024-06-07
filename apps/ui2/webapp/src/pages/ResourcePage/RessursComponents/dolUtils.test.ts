import { addMonths, subMonths } from "date-fns";
import { describe, expect, test } from "vitest";

import {
  dolValidering,
  findPeriodGaps,
  findPeriodOverlaps,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgtilknytningEtterValidering } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import { formatDateWithoutTimezone, todayWithoutTimezone } from "~/utils";

describe("findPeriodGaps", () => {
  test("Sjekk at ingen orgtilknytninger gir ingen gaps", () => {
    expect(findPeriodGaps([])).toEqual([]);
  });

  test("Sjekk at én orgtilknytning gir ingen gaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periodObject = [period1];

    expect(findPeriodGaps(periodObject)).toEqual([]);
  });

  test("Sjekk at to perioder med opphold gir riktig gap", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const period2 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodGaps(periodObject)).toEqual([
      { fom: new Date("2023-02-01"), tom: new Date("2023-02-28") },
    ]);
  });

  test("Sjekk at rekkefølge ikke har noe å si", () => {
    const period1 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const period2 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodGaps(periodObject)).toEqual([
      { fom: new Date("2023-02-01"), tom: new Date("2023-02-28") },
    ]);
  });

  test("Sjekk at perioder inntil hverandre ikke gir gaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const period2 = {
      fom: new Date("2023-01-11"),
      tom: new Date("2023-01-20"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodGaps(periodObject)).toEqual([]);
  });

  test("Sjekk at én dags opphold gir riktig gap", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const period2 = {
      fom: new Date("2023-01-12"),
      tom: new Date("2023-01-20"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodGaps(periodObject)).toEqual([
      {
        fom: new Date("2023-01-11"),
        tom: new Date("2023-01-11"),
      },
    ]);
  });

  test("Sjekk at overlappende perioder ikke påvirker gaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const period2 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const period3 = {
      fom: new Date("2022-12-01"),
      tom: new Date("2023-01-10"),
    };

    const periodObject = [period1, period2, period3];

    expect(findPeriodGaps(periodObject)).toEqual([
      { fom: new Date("2023-02-01"), tom: new Date("2023-02-28") },
    ]);
  });

  test("Sjekk at flere perioder uten opphold gir ingen gaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const period2 = {
      fom: new Date("2023-01-20"),
      tom: new Date("2023-01-31"),
    };

    const period3 = {
      fom: new Date("2022-01-05"),
      tom: new Date("2023-01-25"),
    };

    const periodObject = [period1, period2, period3];

    expect(findPeriodGaps(periodObject)).toEqual([]);
  });

  test("Sjekk at flere opphold mellom perioder gir flere gaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const period2 = {
      fom: new Date("2023-01-20"),
      tom: new Date("2023-01-30"),
    };

    const period3 = {
      fom: new Date("2023-02-10"),
      tom: new Date("2023-02-20"),
    };

    const periodObject = [period1, period2, period3];

    expect(findPeriodGaps(periodObject)).toEqual([
      { fom: new Date("2023-01-11"), tom: new Date("2023-01-19") },
      { fom: new Date("2023-01-31"), tom: new Date("2023-02-09") },
    ]);
  });

  test("Sjekk at flere perioder inntil hverandre ikke gir gaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const period2 = {
      fom: new Date("2023-01-11"),
      tom: new Date("2023-01-20"),
    };

    const period3 = {
      fom: new Date("2023-01-21"),
      tom: new Date("2023-01-31"),
    };

    const periodObject = [period1, period2, period3];

    expect(findPeriodGaps(periodObject)).toEqual([]);
  });

  test("Sjekk at langt opphold fungerer som forventet", () => {
    const period1 = {
      fom: new Date("2021-01-01"),
      tom: new Date("2021-01-31"),
    };

    const period2 = {
      fom: new Date("2024-01-01"),
      tom: new Date("2024-01-31"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodGaps(periodObject)).toEqual([
      {
        fom: new Date("2021-02-01"),
        tom: new Date("2023-12-31"),
      },
    ]);
  });

  test("Sjekk at udefinert slutt behandles som å vare på ubestemt tid", () => {
    const period1 = {
      fom: new Date("2023-02-01"),
      tom: new Date("2023-02-28"),
    };

    const period2 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const period3 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const periodObject = [period1, period2, period3];

    expect(findPeriodGaps(periodObject)).toEqual([]);
  });

  test("Sjekk at to perioder uten slutt gir ingen gaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const period2 = {
      fom: new Date("2023-02-01"),
      tom: undefined,
    };

    const periodObject = [period1, period2];

    expect(findPeriodGaps(periodObject)).toEqual([]);
  });
});

describe("findPeriodOverlaps", () => {
  test("Sjekk at ingen orgtilknytninger gir ingen overlaps", () => {
    expect(findPeriodOverlaps([])).toEqual([]);
  });

  test("Sjekk at én orgtilknytning gir ingen overlaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periodObject = [period1];

    expect(findPeriodOverlaps(periodObject)).toEqual([]);
  });

  test("Sjekk at perioder med overlapp gir riktig overlap", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const period2 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-25"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      { fom: new Date("2023-01-10"), tom: new Date("2023-01-15") },
    ]);
  });

  test("Sjekk at to like perioder har gir overlapp", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const period2 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-01-01"),
        tom: new Date("2023-01-31"),
      },
    ]);
  });

  test("Sjekk at rekkefølge ikke har noe å si", () => {
    const period1 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-25"),
    };

    const period2 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      { fom: new Date("2023-01-10"), tom: new Date("2023-01-15") },
    ]);
  });

  test("Sjekk at perioder inntil hverandre ikke gir overlap", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const period2 = {
      fom: new Date("2023-01-16"),
      tom: new Date("2023-01-25"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodOverlaps(periodObject)).toEqual([]);
  });

  test("Sjekk at to perioder som slutter og starter på samme dag gir overlap", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const period2 = {
      fom: new Date("2023-01-15"),
      tom: new Date("2023-01-25"),
    };

    const periodObject = [period1, period2];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-01-15"),
        tom: new Date("2023-01-15"),
      },
    ]);
  });

  test("Sjekk at opphold ikke påvirker overlapp", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-20"),
    };

    const period2 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-15"),
    };

    const period3 = {
      fom: new Date("2023-02-01"),
      tom: new Date("2023-02-20"),
    };

    const period4 = {
      fom: new Date("2023-02-10"),
      tom: new Date("2023-02-15"),
    };

    const periodObject = [period1, period2, period3, period4];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-01-10"),
        tom: new Date("2023-01-15"),
      },
      {
        fom: new Date("2023-02-10"),
        tom: new Date("2023-02-15"),
      },
    ]);
  });

  test("Sjekk at flere tilfeller av overlapp blir slått sammen", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const period2 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-25"),
    };

    const period3 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-20"),
    };

    const periodObject = [period1, period2, period3];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-01-05"),
        tom: new Date("2023-01-20"),
      },
    ]);
  });

  test("Sjekk at flere separate overlapp gir riktig overlaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const period2 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-10"),
    };

    const period3 = {
      fom: new Date("2023-01-15"),
      tom: new Date("2023-01-20"),
    };

    const periodObject = [period1, period2, period3];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-01-05"),
        tom: new Date("2023-01-10"),
      },
      {
        fom: new Date("2023-01-15"),
        tom: new Date("2023-01-20"),
      },
    ]);
  });

  test("Sjekk at mer komplekse overlapp gir riktig overlaps", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const period2 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-10"),
    };

    const period3 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-15"),
    };

    const period4 = {
      fom: new Date("2023-01-20"),
      tom: new Date("2023-02-15"),
    };

    const period5 = {
      fom: new Date("2023-02-10"),
      tom: new Date("2023-02-25"),
    };

    const periodObject = [period1, period2, period3, period4, period5];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-01-05"),
        tom: new Date("2023-01-15"),
      },
      {
        fom: new Date("2023-01-20"),
        tom: new Date("2023-01-31"),
      },
      {
        fom: new Date("2023-02-10"),
        tom: new Date("2023-02-15"),
      },
    ]);
  });

  test("Sjekk at udefinert slutt behandles som å vare på ubestemt tid", () => {
    const period1 = {
      fom: new Date("2023-02-01"),
      tom: new Date("2023-02-28"),
    };

    const period2 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const periodObject = [period1, period2];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-02-01"),
        tom: new Date("2023-02-28"),
      },
    ]);
  });

  test("Sjekk at to perioder uten slutt gir overlap uten slutt", () => {
    const period1 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const period2 = {
      fom: new Date("2023-02-01"),
      tom: undefined,
    };

    const periodObject = [period1, period2];

    expect(findPeriodOverlaps(periodObject)).toEqual([
      {
        fom: new Date("2023-02-01"),
        tom: undefined,
      },
    ]);
  });
});

describe("dolValidation", () => {
  test("Sjekk at ingen orgtilknytninger gir ingen gaps/overlaps", () => {
    expect(dolValidering([])).toEqual({ dolGaps: [], dolOverlaps: [] });
  });

  test("Sjekk at enkeltperiode med DOL ikke gir ingen gaps/overlaps", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-01-31",
    };

    const orgtilknytninger = [orgtilknytning1];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [],
    });
  });

  test("Sjekk at to helt overlappende perioder gir forventet overlap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-01-31",
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-01-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [
        { fom: new Date("2025-01-01"), tom: new Date("2025-01-31") },
      ],
    });
  });

  test("Sjekk at enkeltperiode uten DOL gir forventet gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-01-31",
    };

    const orgtilknytninger = [orgtilknytning1];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2025-01-01"), tom: new Date("2025-01-31") }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at enkeltperiode uten DOL og uten sluttdato gir forventet gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: undefined,
    };

    const orgtilknytninger = [orgtilknytning1];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2025-01-01"), tom: undefined }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode før DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-02-28",
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2025-02-01",
      gyldigTom: "2025-02-28",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2025-01-01"), tom: new Date("2025-01-31") }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode etter DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-02-28",
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2025-02-01",
      gyldigTom: "2025-03-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2025-01-01"), tom: new Date("2025-01-31") }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at perioder før og etter DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-02-28",
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2025-02-01",
      gyldigTom: "2025-03-31",
    };
    const orgtilknytning3: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "2",
      gyldigFom: "2025-03-01",
      gyldigTom: "2025-04-30",
    };

    const orgtilknytninger = [
      orgtilknytning1,
      orgtilknytning2,
      orgtilknytning3,
    ];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [
        { fom: new Date("2025-01-01"), tom: new Date("2025-01-31") },
        { fom: new Date("2025-04-01"), tom: new Date("2025-04-30") },
      ],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode lenger enn DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: "2025-04-30",
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2025-02-01",
      gyldigTom: "2025-03-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [
        { fom: new Date("2025-01-01"), tom: new Date("2025-01-31") },
        { fom: new Date("2025-04-01"), tom: new Date("2025-04-30") },
      ],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode uten sluttdato lenger enn DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: undefined,
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2025-02-01",
      gyldigTom: "2025-03-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [
        { fom: new Date("2025-01-01"), tom: new Date("2025-01-31") },
        { fom: new Date("2025-04-01"), tom: undefined },
      ],
      dolOverlaps: [],
    });
  });

  test("Sjekk at DOL- og ikke-DOL-perioder uten sluttdato gir ingen gaps", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "0",
      gyldigFom: "2025-01-01",
      gyldigTom: undefined,
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "1",
      gyldigFom: "2025-02-01",
      gyldigTom: undefined,
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [],
    });
  });

  test("Sjekk at perioder før dagens dato ignoreres", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2023-01-01",
      gyldigTom: "2023-01-31",
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "1",
      gyldigFom: "2023-02-01",
      gyldigTom: "2023-02-28",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [],
    });
  });

  test("Sjekk at overlapp før dagens dato ignoreres", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "0",
      gyldigFom: "2023-01-01",
      gyldigTom: "2023-01-31",
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2023-01-01",
      gyldigTom: "2023-01-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [],
    });
  });

  test("Sjekk at perioder som starter før dagens dato behandles som at de starter på dagens dato", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: formatDateWithoutTimezone(
        subMonths(todayWithoutTimezone(), 1),
      ),
      gyldigTom: formatDateWithoutTimezone(
        addMonths(todayWithoutTimezone(), 1),
      ),
    };

    const orgtilknytninger = [orgtilknytning1];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [
        {
          fom: todayWithoutTimezone(),
          tom: addMonths(todayWithoutTimezone(), 1),
        },
      ],
      dolOverlaps: [],
    });
  });

  test("Sjekk at overlapp som starter før dagens dato behandles som at de starter på dagens dato", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "0",
      gyldigFom: formatDateWithoutTimezone(
        subMonths(todayWithoutTimezone(), 2),
      ),
      gyldigTom: formatDateWithoutTimezone(
        addMonths(todayWithoutTimezone(), 1),
      ),
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: formatDateWithoutTimezone(
        subMonths(todayWithoutTimezone(), 1),
      ),
      gyldigTom: formatDateWithoutTimezone(
        addMonths(todayWithoutTimezone(), 1),
      ),
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [
        {
          fom: todayWithoutTimezone(),
          tom: addMonths(todayWithoutTimezone(), 1),
        },
      ],
    });
  });

  test("Sjekk at mer komplekse overlapp som starter før dagens dato gir sammenslåtte overlaps", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "0",
      gyldigFom: formatDateWithoutTimezone(
        subMonths(todayWithoutTimezone(), 2),
      ),
      gyldigTom: formatDateWithoutTimezone(
        addMonths(todayWithoutTimezone(), 1),
      ),
    };
    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: formatDateWithoutTimezone(
        subMonths(todayWithoutTimezone(), 1),
      ),
      gyldigTom: formatDateWithoutTimezone(
        addMonths(todayWithoutTimezone(), 1),
      ),
    };
    const orgtilknytning3: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "2",
      gyldigFom: formatDateWithoutTimezone(
        subMonths(todayWithoutTimezone(), 1),
      ),
      gyldigTom: formatDateWithoutTimezone(
        addMonths(todayWithoutTimezone(), 3),
      ),
    };
    const orgtilknytning4: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "3",
      gyldigFom: formatDateWithoutTimezone(todayWithoutTimezone()),
      gyldigTom: formatDateWithoutTimezone(
        addMonths(todayWithoutTimezone(), 2),
      ),
    };

    const orgtilknytninger = [
      orgtilknytning1,
      orgtilknytning2,
      orgtilknytning3,
      orgtilknytning4,
    ];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [
        {
          fom: todayWithoutTimezone(),
          tom: addMonths(todayWithoutTimezone(), 2),
        },
      ],
    });
  });
});
