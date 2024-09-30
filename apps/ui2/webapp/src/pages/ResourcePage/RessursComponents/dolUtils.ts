import { addDays, isAfter, isBefore, subDays } from "date-fns";
import orderBy from "lodash/orderBy";
import sortBy from "lodash/orderBy";

import type { OrgtilknytningEtterValidering } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import type { OrgtilknytningState, Ressurs } from "~/types/api-models";
import {
  earliestDate,
  earliestDateInList,
  formatDateWithoutTimezone,
  isBeforeDate,
  isSameDate,
  isSameDateOrBefore,
  isTodayOrAfter,
  latestDate,
  todayWithoutTimezone,
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

  for (let index1 = 0; index1 < periods.length - 1; index1++) {
    for (let index2 = index1 + 1; index2 < periods.length; index2++) {
      const period1 = periods[index1];
      const period2 = periods[index2];

      const overlapStart = latestDate(period1.fom, period2.fom);

      const overlapEnd =
        period1.tom && period2.tom
          ? earliestDate(period1.tom, period2.tom)
          : period1.tom || period2.tom;

      if (!overlapEnd || isSameDateOrBefore(overlapStart, overlapEnd)) {
        overlaps.push({ fom: overlapStart, tom: overlapEnd });
      }
    }
  }

  if (overlaps.length <= 1) return overlaps;

  overlaps.sort((a, b) => (isBefore(a.fom, b.fom) ? -1 : 1));

  const mergedPeriods: { fom: Date; tom: Date | undefined }[] = [];
  let currentPeriod = { ...overlaps[0] };

  for (let index = 1; index < overlaps.length; index++) {
    const nextPeriod = overlaps[index];

    if (
      !currentPeriod.tom ||
      isSameDateOrBefore(nextPeriod.fom, addDays(currentPeriod.tom, 1))
    ) {
      if (nextPeriod.tom) {
        if (!currentPeriod.tom || isAfter(nextPeriod.tom, currentPeriod.tom)) {
          currentPeriod.tom = nextPeriod.tom;
        }
      } else {
        currentPeriod.tom = undefined;
      }
    } else {
      mergedPeriods.push(currentPeriod);
      currentPeriod = { ...nextPeriod };
    }
  }

  mergedPeriods.push(currentPeriod);

  return mergedPeriods;
}

// TODO: Refaktorering
export function dolValidering(
  orgtilknytninger: OrgtilknytningEtterValidering[],
) {
  const today = todayWithoutTimezone();

  const dolPeriods = orgtilknytninger
    .filter(
      (tilknytning) =>
        tilknytning.erDagligOppfolging &&
        (!tilknytning.gyldigTom ||
          isTodayOrAfter(new Date(tilknytning.gyldigTom))),
    )
    .map((tilknytning) => {
      return {
        fom: isBefore(new Date(tilknytning.gyldigFom), today)
          ? today
          : new Date(tilknytning.gyldigFom),
        tom: tilknytning.gyldigTom
          ? new Date(tilknytning.gyldigTom)
          : undefined,
      };
    });

  const dolGaps: { fom: Date; tom: Date | undefined }[] =
    findPeriodGaps(dolPeriods);

  let earliestTilknytningDate: Date | undefined;
  let latestTilknytningDate: Date | undefined = new Date(0);

  orgtilknytninger
    .filter((tilknytning) => {
      return tilknytning.gyldigTom
        ? isTodayOrAfter(new Date(tilknytning.gyldigTom))
        : true;
    })
    .map((tilknytning) => {
      const cappedFom = isBefore(new Date(tilknytning.gyldigFom), today)
        ? today
        : new Date(tilknytning.gyldigFom);

      earliestTilknytningDate =
        !earliestTilknytningDate || isBefore(cappedFom, earliestTilknytningDate)
          ? cappedFom
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

  let earliestDolDate: Date | undefined;
  let latestDolDate: Date | undefined = new Date(0);

  if (dolPeriods.length > 0) {
    dolPeriods.map((tilknytning) => {
      earliestDolDate =
        !earliestDolDate || isBefore(tilknytning.fom, earliestDolDate)
          ? tilknytning.fom
          : earliestDolDate;

      if (tilknytning.tom && latestDolDate) {
        latestDolDate = isAfter(new Date(tilknytning.tom), latestDolDate)
          ? new Date(tilknytning.tom)
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
    } else if (isAfter(latestTilknytningDate, latestDolDate)) {
      dolGaps.push({
        fom: addDays(latestDolDate, 1),
        tom: latestTilknytningDate,
      });
    }
  } else {
    if (earliestTilknytningDate) {
      dolGaps.push({
        fom: earliestTilknytningDate,
        tom: latestTilknytningDate ?? undefined,
      });
    }
  }

  return { dolGaps, dolOverlaps: findPeriodOverlaps(dolPeriods) };
}

export function senesteTom(
  orgtilknytninger: Partial<OrgtilknytningState>[],
): string | undefined {
  if (orgtilknytninger.some((orgtilknytning) => !orgtilknytning.gyldigTom)) {
    return undefined;
  }

  return orderBy(orgtilknytninger, "gyldigTom", "desc")[0].gyldigTom;
}

export function setRessursStartdato(
  ressurs: Ressurs,
  orgtilknytninger: Partial<OrgtilknytningState>[],
  tidligsteOrgDatoPre: string | undefined,
) {
  const ressursStartdato = ressurs.startdato;

  const tidligsteNyeOrgtilknytningStartdato = earliestDateInList(
    orgtilknytninger
      .filter((orgtilknytning) => orgtilknytning.gyldigFom)
      .map((orgtilknytning) => new Date(orgtilknytning.gyldigFom as string)),
  );

  const tidligsteOrgDatoPost = tidligsteNyeOrgtilknytningStartdato
    ? formatDateWithoutTimezone(tidligsteNyeOrgtilknytningStartdato)
    : undefined;

  return ressursStartdato
    ? isSameDate(ressursStartdato, tidligsteOrgDatoPre as string) ||
        isBeforeDate(tidligsteOrgDatoPost as string, ressursStartdato)
    : false;
}
