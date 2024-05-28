import { addDays, isAfter, isBefore, subDays } from "date-fns";
import sortBy from "lodash/sortBy";

import type { OrgtilknytningEtterValidering } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import {
  isPresentOrAfter,
  isSameDateOrAfter,
  isSameDateOrBefore,
} from "~/utils";

export function findPeriodGaps(
  periods: { fom: Date; tom: Date | undefined }[],
): { fom: Date; tom: Date }[] {
  if (periods.length <= 1) {
    return [];
  }

  const sortedPeriods = sortBy(periods, "fom");

  const gaps: { fom: Date; tom: Date }[] = [];

  let latestEndDate = sortedPeriods[0].tom;

  for (let index = 1; index < sortedPeriods.length; index++) {
    const { fom: periodStart, tom: periodEnd } = sortedPeriods[index];

    if (!latestEndDate) break;

    if (isAfter(periodStart, addDays(latestEndDate, 1))) {
      gaps.push({
        fom: addDays(latestEndDate, 1),
        tom: subDays(periodStart, 1),
      });
    }

    latestEndDate =
      periodEnd &&
      (isAfter(periodEnd, latestEndDate) ? periodEnd : latestEndDate);
  }

  return gaps;
}

export function findPeriodOverlaps(
  periods: { fom: Date; tom: Date | undefined }[],
): { fom: Date; tom: Date | undefined }[] {
  if (periods.length <= 1) {
    return [];
  }

  const overlaps: { fom: Date; tom: Date | undefined }[] = [];

  for (let index1 = 0; index1 < periods.length; index1++) {
    for (let index2 = index1 + 1; index2 < periods.length; index2++) {
      if (index1 !== index2) {
        const period1 = periods[index1];
        const period2 = periods[index2];

        if (
          (!period1.tom || isSameDateOrAfter(period1.tom, period2.fom)) &&
          (!period2.tom || isSameDateOrAfter(period2.tom, period1.fom))
        ) {
          const overlapStart = isAfter(period1.fom, period2.fom)
            ? period1.fom
            : period2.fom;

          let overlapEnd: Date | undefined;

          if (period1.tom && period2.tom) {
            overlapEnd = isBefore(period1.tom, period2.tom)
              ? period1.tom
              : period2.tom;
          } else if (period1.tom) {
            overlapEnd = period1.tom;
          } else if (period2.tom) {
            overlapEnd = period2.tom;
          } else {
            overlapEnd = undefined;
          }

          overlaps.push({ fom: overlapStart, tom: overlapEnd });
        }
      }
    }
  }

  const mergedPeriods: { fom: Date; tom: Date | undefined }[] = [];

  if (overlaps.length > 1) {
    overlaps.sort((a, b) => (isBefore(a.fom, b.fom) ? -1 : 1));

    let currentPeriod = {
      fom: overlaps[0].fom,
      tom: overlaps[0].tom,
    };

    for (let index = 1; index < overlaps.length; index++) {
      if (!currentPeriod.tom) {
        break;
      }

      const checkPeriod = overlaps[index];

      if (isSameDateOrBefore(checkPeriod.fom, addDays(currentPeriod.tom, 1))) {
        if (checkPeriod.tom) {
          currentPeriod.tom = isAfter(checkPeriod.tom, currentPeriod.tom)
            ? checkPeriod.tom
            : currentPeriod.tom;
        } else {
          currentPeriod.tom = undefined;
        }
      } else {
        mergedPeriods.push(currentPeriod);

        currentPeriod = {
          fom: checkPeriod.fom,
          tom: checkPeriod.tom,
        };
      }
    }

    mergedPeriods.push(currentPeriod);

    return mergedPeriods;
  }

  return overlaps;
}

// TODO: Sikkert lurt å skrive jest-tester for denne funksjonaliteten, deretter refaktorere litt
export function dolValidering(
  orgtilknytninger: OrgtilknytningEtterValidering[],
) {
  const dolOrgtilknytninger = orgtilknytninger.filter(
    (tilknytning) => tilknytning.erDagligOppfolging,
  );

  const fremtidigeTilknytninger = dolOrgtilknytninger.filter((tilknytning) =>
    tilknytning.gyldigTom
      ? isPresentOrAfter(new Date(tilknytning.gyldigTom))
      : true,
  );
  const stigendeFremtidigeTilknytninger = sortBy(
    fremtidigeTilknytninger,
    (orgtilknytning) => new Date(orgtilknytning.gyldigFom),
  );

  const dolPeriods = stigendeFremtidigeTilknytninger.map((tilknytning) => {
    return {
      fom: isBefore(new Date(tilknytning.gyldigFom), new Date())
        ? new Date()
        : new Date(tilknytning.gyldigFom),
      tom: tilknytning.gyldigTom ? new Date(tilknytning.gyldigTom) : undefined,
    };
  });

  const dolGaps: { fom: Date; tom: Date | undefined }[] =
    findPeriodGaps(dolPeriods);

  let earliestTilknytningDate: Date | undefined;
  let latestTilknytningDate: Date | undefined = new Date(0);

  let earliestDolDate: Date | undefined;
  let latestDolDate: Date | undefined = new Date(0);

  orgtilknytninger
    .filter((tilknytning) => {
      return tilknytning.gyldigTom
        ? isPresentOrAfter(new Date(tilknytning.gyldigTom))
        : true;
    })
    .map((tilknytning) => {
      const today = new Date();
      const cappedTom = isBefore(new Date(tilknytning.gyldigFom), today)
        ? today
        : new Date(tilknytning.gyldigFom);

      earliestTilknytningDate =
        !earliestTilknytningDate || isBefore(cappedTom, earliestTilknytningDate)
          ? cappedTom
          : earliestTilknytningDate;

      if (tilknytning.gyldigTom && latestTilknytningDate) {
        latestTilknytningDate = isAfter(
          new Date(tilknytning.gyldigTom),
          latestTilknytningDate,
        )
          ? new Date(tilknytning.gyldigTom)
          : latestTilknytningDate;
      } else {
        latestTilknytningDate = undefined;
      }
    });

  stigendeFremtidigeTilknytninger.map((tilknytning) => {
    const today = new Date();
    const cappedTom = isBefore(new Date(tilknytning.gyldigFom), today)
      ? today
      : new Date(tilknytning.gyldigFom);

    earliestDolDate =
      !earliestDolDate || isBefore(cappedTom, earliestDolDate)
        ? cappedTom
        : earliestDolDate;

    if (tilknytning.gyldigTom && latestDolDate) {
      latestDolDate = isAfter(new Date(tilknytning.gyldigTom), latestDolDate)
        ? new Date(tilknytning.gyldigTom)
        : latestDolDate;
    } else {
      latestDolDate = undefined;
    }
  });

  if (
    earliestTilknytningDate &&
    earliestDolDate &&
    isBefore(earliestTilknytningDate, earliestDolDate)
  ) {
    dolGaps.unshift({
      fom: earliestTilknytningDate,
      tom: subDays(earliestDolDate, 1),
    });
  }

  if (!latestTilknytningDate && latestDolDate) {
    dolGaps.push({ fom: addDays(latestDolDate, 1), tom: undefined });
  } else if (latestTilknytningDate && !latestDolDate) {
    dolGaps.push({
      fom: addDays(latestTilknytningDate, 1),
      tom: undefined,
    });
  } else if (isAfter(latestTilknytningDate, latestDolDate)) {
    dolGaps.push({
      fom: addDays(latestDolDate, 1),
      tom: latestTilknytningDate,
    });
  }

  return { dolGaps, dolOverlaps: findPeriodOverlaps(dolPeriods) };
}
