import { addMonths, subMonths } from "date-fns";
import { describe, expect, test } from "vitest";

import {
  dolValidering,
  findPeriodGaps,
  findPeriodOverlaps,
  nomDateIsAfter,
  nomDateIsAfterToday,
  nomDateIsBefore,
  nomDateIsBeforeToday,
  nomDateIsSame,
  nomDateIsSameOrAfter,
  nomDateIsSameOrBefore,
  nomDateIsToday,
  nomDateIsTodayOrAfter,
  nomDateIsTodayOrBefore,
  nomEarliestDate,
  nomLatestDate,
} from "~/pages/ResourcePage/RessursComponents/dolUtils";
import type { OrgtilknytningEtterValidering } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import {
  dateWithoutTimezone,
  formatDateWithoutTimezone,
  todayWithoutTimezone,
} from "~/utils";

describe("findPeriodGaps", () => {
  test("Sjekk at ingen orgtilknytninger gir ingen gaps", () => {
    expect(findPeriodGaps([])).toEqual([]);
  });

  test("Sjekk at én orgtilknytning gir ingen gaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const perioder = [periode1];

    expect(findPeriodGaps(perioder)).toEqual([]);
  });

  test("Sjekk at to perioder med opphold gir riktig gap", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periode2 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodGaps(perioder)).toEqual([
      { fom: new Date("2023-02-01"), tom: new Date("2023-02-28") },
    ]);
  });

  test("Sjekk at rekkefølge ikke har noe å si", () => {
    const periode1 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const periode2 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodGaps(perioder)).toEqual([
      { fom: new Date("2023-02-01"), tom: new Date("2023-02-28") },
    ]);
  });

  test("Sjekk at perioder inntil hverandre ikke gir gaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const periode2 = {
      fom: new Date("2023-01-11"),
      tom: new Date("2023-01-20"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodGaps(perioder)).toEqual([]);
  });

  test("Sjekk at én dags opphold gir riktig gap", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const periode2 = {
      fom: new Date("2023-01-12"),
      tom: new Date("2023-01-20"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodGaps(perioder)).toEqual([
      {
        fom: new Date("2023-01-11"),
        tom: new Date("2023-01-11"),
      },
    ]);
  });

  test("Sjekk at overlappende perioder ikke påvirker gaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periode2 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const periode3 = {
      fom: new Date("2022-12-01"),
      tom: new Date("2023-01-10"),
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodGaps(perioder)).toEqual([
      { fom: new Date("2023-02-01"), tom: new Date("2023-02-28") },
    ]);
  });

  test("Sjekk at flere perioder uten opphold gir ingen gaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const periode2 = {
      fom: new Date("2023-01-20"),
      tom: new Date("2023-01-31"),
    };

    const periode3 = {
      fom: new Date("2022-01-05"),
      tom: new Date("2023-01-25"),
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodGaps(perioder)).toEqual([]);
  });

  test("Sjekk at flere opphold mellom perioder gir flere gaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const periode2 = {
      fom: new Date("2023-01-20"),
      tom: new Date("2023-01-30"),
    };

    const periode3 = {
      fom: new Date("2023-02-10"),
      tom: new Date("2023-02-20"),
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodGaps(perioder)).toEqual([
      { fom: new Date("2023-01-11"), tom: new Date("2023-01-19") },
      { fom: new Date("2023-01-31"), tom: new Date("2023-02-09") },
    ]);
  });

  test("Sjekk at flere perioder inntil hverandre ikke gir gaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-10"),
    };

    const periode2 = {
      fom: new Date("2023-01-11"),
      tom: new Date("2023-01-20"),
    };

    const periode3 = {
      fom: new Date("2023-01-21"),
      tom: new Date("2023-01-31"),
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodGaps(perioder)).toEqual([]);
  });

  test("Sjekk at langt opphold fungerer som forventet", () => {
    const periode1 = {
      fom: new Date("2021-01-01"),
      tom: new Date("2021-01-31"),
    };

    const periode2 = {
      fom: new Date("2024-01-01"),
      tom: new Date("2024-01-31"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodGaps(perioder)).toEqual([
      {
        fom: new Date("2021-02-01"),
        tom: new Date("2023-12-31"),
      },
    ]);
  });

  test("Sjekk at udefinert slutt behandles som å vare på ubestemt tid", () => {
    const periode1 = {
      fom: new Date("2023-02-01"),
      tom: new Date("2023-02-28"),
    };

    const periode2 = {
      fom: new Date("2023-03-01"),
      tom: new Date("2023-03-31"),
    };

    const periode3 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodGaps(perioder)).toEqual([]);
  });

  test("Sjekk at to perioder uten slutt gir ingen gaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const periode2 = {
      fom: new Date("2023-02-01"),
      tom: undefined,
    };

    const perioder = [periode1, periode2];

    expect(findPeriodGaps(perioder)).toEqual([]);
  });
});

describe("findPeriodOverlaps", () => {
  test("Sjekk at ingen orgtilknytninger gir ingen overlaps", () => {
    expect(findPeriodOverlaps([])).toEqual([]);
  });

  test("Sjekk at én orgtilknytning gir ingen overlaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const perioder = [periode1];

    expect(findPeriodOverlaps(perioder)).toEqual([]);
  });

  test("Sjekk at perioder med overlapp gir riktig overlap", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const periode2 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-25"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodOverlaps(perioder)).toEqual([
      { fom: new Date("2023-01-10"), tom: new Date("2023-01-15") },
    ]);
  });

  test("Sjekk at to like perioder gir overlap", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periode2 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodOverlaps(perioder)).toEqual([
      {
        fom: new Date("2023-01-01"),
        tom: new Date("2023-01-31"),
      },
    ]);
  });

  test("Sjekk at rekkefølge ikke har noe å si", () => {
    const periode1 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-25"),
    };

    const periode2 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodOverlaps(perioder)).toEqual([
      { fom: new Date("2023-01-10"), tom: new Date("2023-01-15") },
    ]);
  });

  test("Sjekk at perioder inntil hverandre ikke gir overlap", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const periode2 = {
      fom: new Date("2023-01-16"),
      tom: new Date("2023-01-25"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodOverlaps(perioder)).toEqual([]);
  });

  test("Sjekk at to perioder som slutter og starter på samme dag gir overlap", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const periode2 = {
      fom: new Date("2023-01-15"),
      tom: new Date("2023-01-25"),
    };

    const perioder = [periode1, periode2];

    expect(findPeriodOverlaps(perioder)).toEqual([
      {
        fom: new Date("2023-01-15"),
        tom: new Date("2023-01-15"),
      },
    ]);
  });

  test("Sjekk at opphold ikke påvirker overlapp", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-20"),
    };

    const periode2 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-15"),
    };

    const periode3 = {
      fom: new Date("2023-02-01"),
      tom: new Date("2023-02-20"),
    };

    const periode4 = {
      fom: new Date("2023-02-10"),
      tom: new Date("2023-02-15"),
    };

    const perioder = [periode1, periode2, periode3, periode4];

    expect(findPeriodOverlaps(perioder)).toEqual([
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

  test("Sjekk at overlappende overlapp blir slått sammen", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-15"),
    };

    const periode2 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-25"),
    };

    const periode3 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-20"),
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodOverlaps(perioder)).toEqual([
      {
        fom: new Date("2023-01-05"),
        tom: new Date("2023-01-20"),
      },
    ]);
  });

  test("Sjekk at helt overlappende overlapp blir slått sammen", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periode2 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-20"),
    };

    const periode3 = {
      fom: new Date("2023-01-10"),
      tom: new Date("2023-01-15"),
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodOverlaps(perioder)).toEqual([
      {
        fom: new Date("2023-01-05"),
        tom: new Date("2023-01-20"),
      },
    ]);
  });

  test("Sjekk at flere separate overlapp gir riktig overlaps", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periode2 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-10"),
    };

    const periode3 = {
      fom: new Date("2023-01-15"),
      tom: new Date("2023-01-20"),
    };

    const perioder = [periode1, periode2, periode3];

    expect(findPeriodOverlaps(perioder)).toEqual([
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
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: new Date("2023-01-31"),
    };

    const periode2 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-10"),
    };

    const periode3 = {
      fom: new Date("2023-01-05"),
      tom: new Date("2023-01-15"),
    };

    const periode4 = {
      fom: new Date("2023-01-20"),
      tom: new Date("2023-02-15"),
    };

    const periode5 = {
      fom: new Date("2023-02-10"),
      tom: new Date("2023-02-25"),
    };

    const perioder = [periode1, periode2, periode3, periode4, periode5];

    expect(findPeriodOverlaps(perioder)).toEqual([
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
    const periode1 = {
      fom: new Date("2023-02-01"),
      tom: new Date("2023-02-28"),
    };

    const periode2 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const perioder = [periode1, periode2];

    expect(findPeriodOverlaps(perioder)).toEqual([
      {
        fom: new Date("2023-02-01"),
        tom: new Date("2023-02-28"),
      },
    ]);
  });

  test("Sjekk at to perioder uten slutt gir overlap uten slutt", () => {
    const periode1 = {
      fom: new Date("2023-01-01"),
      tom: undefined,
    };

    const periode2 = {
      fom: new Date("2023-02-01"),
      tom: undefined,
    };

    const perioder = [periode1, periode2];

    expect(findPeriodOverlaps(perioder)).toEqual([
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
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-01-31",
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
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-01-31",
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-01-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [],
      dolOverlaps: [
        { fom: new Date("2027-01-01"), tom: new Date("2027-01-31") },
      ],
    });
  });

  test("Sjekk at enkeltperiode uten DOL gir forventet gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-01-31",
    };

    const orgtilknytninger = [orgtilknytning1];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2027-01-01"), tom: new Date("2027-01-31") }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at enkeltperiode uten DOL og uten sluttdato gir forventet gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: undefined,
    };

    const orgtilknytninger = [orgtilknytning1];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2027-01-01"), tom: undefined }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode før DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-02-28",
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2027-02-01",
      gyldigTom: "2027-02-28",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2027-01-01"), tom: new Date("2027-01-31") }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode etter DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-02-28",
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2027-02-01",
      gyldigTom: "2027-03-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [{ fom: new Date("2027-01-01"), tom: new Date("2027-01-31") }],
      dolOverlaps: [],
    });
  });

  test("Sjekk at perioder før og etter DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-02-28",
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2027-02-01",
      gyldigTom: "2027-03-31",
    };

    const orgtilknytning3: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "2",
      gyldigFom: "2027-03-01",
      gyldigTom: "2027-04-30",
    };

    const orgtilknytninger = [
      orgtilknytning1,
      orgtilknytning2,
      orgtilknytning3,
    ];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [
        { fom: new Date("2027-01-01"), tom: new Date("2027-01-31") },
        { fom: new Date("2027-04-01"), tom: new Date("2027-04-30") },
      ],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode lenger enn DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: "2027-04-30",
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2027-02-01",
      gyldigTom: "2027-03-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [
        { fom: new Date("2027-01-01"), tom: new Date("2027-01-31") },
        { fom: new Date("2027-04-01"), tom: new Date("2027-04-30") },
      ],
      dolOverlaps: [],
    });
  });

  test("Sjekk at periode uten sluttdato lenger enn DOL gir riktig gap", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: undefined,
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "1",
      gyldigFom: "2027-02-01",
      gyldigTom: "2027-03-31",
    };

    const orgtilknytninger = [orgtilknytning1, orgtilknytning2];

    expect(dolValidering(orgtilknytninger)).toEqual({
      dolGaps: [
        { fom: new Date("2027-01-01"), tom: new Date("2027-01-31") },
        { fom: new Date("2027-04-01"), tom: undefined },
      ],
      dolOverlaps: [],
    });
  });

  test("Sjekk at DOL- og ikke-DOL-perioder uten sluttdato gir ingen gaps", () => {
    const orgtilknytning1: OrgtilknytningEtterValidering = {
      erDagligOppfolging: true,
      orgEnhetId: "0",
      gyldigFom: "2027-01-01",
      gyldigTom: undefined,
    };

    const orgtilknytning2: OrgtilknytningEtterValidering = {
      erDagligOppfolging: false,
      orgEnhetId: "1",
      gyldigFom: "2027-02-01",
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
          tom: dateWithoutTimezone(addMonths(todayWithoutTimezone(), 1)),
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
          tom: dateWithoutTimezone(addMonths(todayWithoutTimezone(), 1)),
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
          tom: dateWithoutTimezone(addMonths(todayWithoutTimezone(), 2)),
        },
      ],
    });
  });
});

describe("nomDateIsSame(Date | string | null, Date | string | null)", () => {
  test("To datoer", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSame(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og undefined", () => {
    const testDate1 = undefined;
    const testDate2 = null;

    const input = nomDateIsSame(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og dato", () => {
    const testDate1 = null;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSame(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Dato og null", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = null;

    const input = nomDateIsSame(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Undefined og dato", () => {
    const testDate1 = undefined;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSame(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsSameOrBefore(Date | string | null | undefined, Date | string | null | undefined)", () => {
  test("To datoer", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSameOrBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og undefined", () => {
    const testDate1 = undefined;
    const testDate2 = null;

    const input = nomDateIsSameOrBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og dato", () => {
    const testDate1 = null;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSameOrBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Dato og null", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = null;

    const input = nomDateIsSameOrBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Undefined og dato", () => {
    const testDate1 = undefined;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSameOrBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsSameOrAfter(Date | string | null | undefined, Date | string | null | undefined)", () => {
  test("To datoer", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSameOrAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og undefined", () => {
    const testDate1 = undefined;
    const testDate2 = null;

    const input = nomDateIsSameOrAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og dato", () => {
    const testDate1 = null;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSameOrAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato og null", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = null;

    const input = nomDateIsSameOrAfter(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Undefined og dato", () => {
    const testDate1 = undefined;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsSameOrAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsBefore(Date | string | null | undefined, Date | string | null | undefined)", () => {
  test("To datoer", () => {
    const testDate1 = new Date("2023-12-31");
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og undefined", () => {
    const testDate1 = undefined;
    const testDate2 = null;

    const input = nomDateIsBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Null og dato", () => {
    const testDate1 = null;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Dato og null", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = null;

    const input = nomDateIsBefore(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Undefined og dato", () => {
    const testDate1 = undefined;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsBefore(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsAfter(Date | string | null | undefined, Date | string | null | undefined)", () => {
  test("To datoer", () => {
    const testDate1 = new Date("2024-01-02");
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Null og undefined", () => {
    const testDate1 = undefined;
    const testDate2 = null;

    const input = nomDateIsAfter(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Null og dato", () => {
    const testDate1 = null;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato og null", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = null;

    const input = nomDateIsAfter(testDate1, testDate2);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Undefined og dato", () => {
    const testDate1 = undefined;
    const testDate2 = new Date("2024-01-01");

    const input = nomDateIsAfter(testDate1, testDate2);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsToday(Date | string | null | undefined)", () => {
  test("Dato er i dag", () => {
    const testDate = todayWithoutTimezone();

    const input = nomDateIsToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato er ikke i dag", () => {
    const testDate = dateWithoutTimezone(new Date("2024-01-01"));

    const input = nomDateIsToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Null som dato", () => {
    const testDate = null;

    const input = nomDateIsToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Undefined som dato", () => {
    const testDate = undefined;

    const input = nomDateIsToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsTodayOrBefore(Date | string | null | undefined)", () => {
  test("Dato er i dag", () => {
    const testDate = todayWithoutTimezone();

    const input = nomDateIsTodayOrBefore(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato er før i dag", () => {
    const testDate = dateWithoutTimezone(new Date("2024-01-01"));

    const input = nomDateIsTodayOrBefore(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato er etter i dag", () => {
    const testDate = dateWithoutTimezone(addMonths(todayWithoutTimezone(), 1));

    const input = nomDateIsTodayOrBefore(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Null som dato", () => {
    const testDate = null;

    const input = nomDateIsTodayOrBefore(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Undefined som dato", () => {
    const testDate = undefined;

    const input = nomDateIsTodayOrBefore(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsTodayOrAfter(Date | string | null | undefined)", () => {
  test("Dato er i dag", () => {
    const testDate = todayWithoutTimezone();

    const input = nomDateIsTodayOrAfter(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato er etter i dag", () => {
    const testDate = dateWithoutTimezone(addMonths(todayWithoutTimezone(), 1));

    const input = nomDateIsTodayOrAfter(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato er før i dag", () => {
    const testDate = dateWithoutTimezone(new Date("2024-01-01"));

    const input = nomDateIsTodayOrAfter(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Null som dato", () => {
    const testDate = null;

    const input = nomDateIsTodayOrAfter(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Undefined som dato", () => {
    const testDate = undefined;

    const input = nomDateIsTodayOrAfter(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsBeforeToday(Date | string | null | undefined)", () => {
  test("Dato er før i dag", () => {
    const testDate = dateWithoutTimezone(new Date("2024-01-01"));

    const input = nomDateIsBeforeToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato er i dag", () => {
    const testDate = todayWithoutTimezone();

    const input = nomDateIsBeforeToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Dato er etter i dag", () => {
    const testDate = dateWithoutTimezone(addMonths(todayWithoutTimezone(), 1));

    const input = nomDateIsBeforeToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Null som dato", () => {
    const testDate = null;

    const input = nomDateIsBeforeToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Undefined som dato", () => {
    const testDate = undefined;

    const input = nomDateIsBeforeToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });
});

describe("nomDateIsAfterToday(Date | string | null | undefined)", () => {
  test("Dato er etter i dag", () => {
    const testDate = dateWithoutTimezone(addMonths(todayWithoutTimezone(), 1));

    const input = nomDateIsAfterToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Dato er i dag", () => {
    const testDate = todayWithoutTimezone();

    const input = nomDateIsAfterToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Dato er før i dag", () => {
    const testDate = dateWithoutTimezone(new Date("2024-01-01"));

    const input = nomDateIsAfterToday(testDate);

    const expected = false;

    expect(input).toEqual(expected);
  });

  test("Null som dato", () => {
    const testDate = null;

    const input = nomDateIsAfterToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });

  test("Undefined som dato", () => {
    const testDate = undefined;

    const input = nomDateIsAfterToday(testDate);

    const expected = true;

    expect(input).toEqual(expected);
  });
});

describe("nomEarliestDate(Date | string | null | undefined, Date | string | null | undefined)", () => {
  test("Begge datoer er null", () => {
    const testDate1 = null;
    const testDate2 = null;

    const input = nomEarliestDate(testDate1, testDate2);

    const expected = null;

    expect(input).toEqual(expected);
  });

  test("Den ene datoen er null", () => {
    const testDate1 = null;
    const testDate2 = new Date("2024-01-01");

    const input = nomEarliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate2);

    expect(input).toEqual(expected);
  });

  test("Begge datoene er definert", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-02-02");

    const input = nomEarliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate1);

    expect(input).toEqual(expected);
  });

  test("Den ene datoen er undefined", () => {
    const testDate1 = undefined;
    const testDate2 = new Date("2024-01-01");

    const input = nomEarliestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate2);

    expect(input).toEqual(expected);
  });

  test("Begge datoene er undefined", () => {
    const testDate1 = undefined;
    const testDate2 = undefined;

    const input = nomEarliestDate(testDate1, testDate2);

    const expected = null;

    expect(input).toEqual(expected);
  });
});

describe("nomLatestDate(Date | string | null | undefined, Date | string | null | undefined)", () => {
  test("Begge datoer er null", () => {
    const testDate1 = null;
    const testDate2 = null;

    const input = nomLatestDate(testDate1, testDate2);

    const expected = null;

    expect(input).toEqual(expected);
  });

  test("Den ene datoen er null", () => {
    const testDate1 = null;
    const testDate2 = new Date("2024-01-01");

    const input = nomLatestDate(testDate1, testDate2);

    const expected = null;

    expect(input).toEqual(expected);
  });

  test("Begge datoene er definert", () => {
    const testDate1 = new Date("2024-01-01");
    const testDate2 = new Date("2024-02-02");

    const input = nomLatestDate(testDate1, testDate2);

    const expected = dateWithoutTimezone(testDate2);

    expect(input).toEqual(expected);
  });

  test("Den ene datoen er undefined", () => {
    const testDate1 = undefined;
    const testDate2 = new Date("2024-01-01");

    const input = nomLatestDate(testDate1, testDate2);

    const expected = null;

    expect(input).toEqual(expected);
  });

  test("Begge datoene er undefined", () => {
    const testDate1 = undefined;
    const testDate2 = undefined;

    const input = nomLatestDate(testDate1, testDate2);

    const expected = null;

    expect(input).toEqual(expected);
  });
});
