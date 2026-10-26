import { addDays, subDays } from "date-fns";
import orderBy from "lodash/orderBy";
import sortBy from "lodash/orderBy";

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
  earliestDateInList,
  formatDateWithoutTimezone,
  latestDate,
  todayWithoutTimezone,
} from "~/customUtils/dateUtils";
import type { OrgtilknytningEtterValidering } from "~/pages/ResourcePage/RessursComponents/EditOrgtilknytninger";
import type { OrgtilknytningState, Ressurs } from "~/types/api-models";

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

    if (dateIsAfter(periodStart, addDays(latestEndDate, 1))) {
      gaps.push({
        fom: addDays(latestEndDate, 1),
        tom: subDays(periodStart, 1),
      });
    }

    latestEndDate =
      periodEnd &&
      (dateIsAfter(periodEnd, latestEndDate) ? periodEnd : latestEndDate);
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

      if (!overlapEnd || dateIsSameOrBefore(overlapStart, overlapEnd)) {
        overlaps.push({ fom: overlapStart, tom: overlapEnd });
      }
    }
  }

  if (overlaps.length <= 1) return overlaps;

  overlaps.sort((a, b) => (dateIsBefore(a.fom, b.fom) ? -1 : 1));

  const mergedPeriods: { fom: Date; tom: Date | undefined }[] = [];
  let currentPeriod = { ...overlaps[0] };

  for (let index = 1; index < overlaps.length; index++) {
    const nextPeriod = overlaps[index];

    if (
      !currentPeriod.tom ||
      dateIsSameOrBefore(nextPeriod.fom, addDays(currentPeriod.tom, 1))
    ) {
      if (nextPeriod.tom) {
        if (nomDateIsAfter(nextPeriod.tom, currentPeriod.tom)) {
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
        nomDateIsTodayOrAfter(tilknytning.gyldigTom),
    )
    .map((tilknytning) => {
      return {
        fom: dateIsBefore(tilknytning.gyldigFom, today)
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
        ? dateIsTodayOrAfter(new Date(tilknytning.gyldigTom))
        : true;
    })
    .map((tilknytning) => {
      const cappedFom = dateIsBefore(tilknytning.gyldigFom, today)
        ? today
        : new Date(tilknytning.gyldigFom);

      earliestTilknytningDate =
        !earliestTilknytningDate ||
        dateIsBefore(cappedFom, earliestTilknytningDate)
          ? cappedFom
          : earliestTilknytningDate;

      if (tilknytning.gyldigTom && latestTilknytningDate) {
        latestTilknytningDate = dateIsAfter(
          tilknytning.gyldigTom,
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
        !earliestDolDate || dateIsBefore(tilknytning.fom, earliestDolDate)
          ? tilknytning.fom
          : earliestDolDate;

      if (tilknytning.tom && latestDolDate) {
        latestDolDate = dateIsAfter(tilknytning.tom, latestDolDate)
          ? new Date(tilknytning.tom)
          : latestDolDate;
      } else {
        latestDolDate = undefined;
      }
    });

    if (
      earliestTilknytningDate &&
      earliestDolDate &&
      dateIsBefore(earliestTilknytningDate, earliestDolDate)
    ) {
      dolGaps.unshift({
        fom: earliestTilknytningDate,
        tom: subDays(earliestDolDate, 1),
      });
    }

    if (!latestTilknytningDate && latestDolDate) {
      dolGaps.push({ fom: addDays(latestDolDate, 1), tom: undefined });
    } else if (nomDateIsAfter(latestTilknytningDate, latestDolDate)) {
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

export function skalSetteRessursStartdato(
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

  if (!ressursStartdato || !tidligsteOrgDatoPost) {
    return false;
  }

  if (
    tidligsteOrgDatoPre &&
    dateIsSame(tidligsteOrgDatoPre, ressursStartdato)
  ) {
    return true;
  }

  return dateIsBefore(tidligsteOrgDatoPost, ressursStartdato);
}

export function otCustomStortByDate(
  otA: OrgtilknytningState,
  otB: OrgtilknytningState,
) {
  if (!otA.gyldigTom && !otB.gyldigTom) {
    return (
      dateWithoutTimezone(new Date(otB.gyldigFom)).getTime() -
      dateWithoutTimezone(new Date(otA.gyldigFom)).getTime()
    );
  }

  if (!otA.gyldigTom) return -1;
  if (!otB.gyldigTom) return 1;

  const tomComparison =
    dateWithoutTimezone(new Date(otB.gyldigTom)).getTime() -
    dateWithoutTimezone(new Date(otA.gyldigTom)).getTime();
  if (tomComparison !== 0) return tomComparison;

  return (
    dateWithoutTimezone(new Date(otB.gyldigFom)).getTime() -
    dateWithoutTimezone(new Date(otA.gyldigFom)).getTime()
  );
}

export function senesteTom(
  orgtilknytninger: Partial<OrgtilknytningState>[],
): string | undefined {
  if (orgtilknytninger.some((orgtilknytning) => !orgtilknytning.gyldigTom)) {
    return undefined;
  }

  return orderBy(orgtilknytninger, "gyldigTom", "desc")[0].gyldigTom;
}

export function nomDateIsSame(
  checkDate: Date | string | null | undefined,
  compareDate: Date | string | null | undefined,
) {
  if (!checkDate && !compareDate) {
    return true;
  }

  if (!checkDate || !compareDate) {
    return false;
  }

  return dateIsSame(checkDate, compareDate);
}

export function nomDateIsSameOrBefore(
  checkDate: Date | string | null | undefined,
  compareDate: Date | string | null | undefined,
) {
  if (!checkDate && !compareDate) {
    return true;
  }

  if (!checkDate) {
    return false;
  }

  if (!compareDate) {
    return true;
  }

  return dateIsSameOrBefore(checkDate, compareDate);
}

export function nomDateIsSameOrAfter(
  checkDate: Date | string | null | undefined,
  compareDate: Date | string | null | undefined,
) {
  if (!checkDate && !compareDate) {
    return true;
  }

  if (!checkDate) {
    return true;
  }

  if (!compareDate) {
    return false;
  }

  return dateIsSameOrAfter(checkDate, compareDate);
}

export function nomDateIsBefore(
  checkDate: Date | string | null | undefined,
  compareDate: Date | string | null | undefined,
) {
  if (!checkDate && !compareDate) {
    return false;
  }

  if (!checkDate) {
    return false;
  }

  if (!compareDate) {
    return true;
  }

  return dateIsBefore(checkDate, compareDate);
}

export function nomDateIsAfter(
  checkDate: Date | string | null | undefined,
  compareDate: Date | string | null | undefined,
) {
  if (!checkDate && !compareDate) {
    return false;
  }

  if (!checkDate) {
    return true;
  }

  if (!compareDate) {
    return false;
  }

  return dateIsAfter(checkDate, compareDate);
}

export function nomDateIsToday(checkDate: Date | string | null | undefined) {
  if (!checkDate) {
    return false;
  }
  return dateIsToday(checkDate);
}

export function nomDateIsTodayOrBefore(
  checkDate: Date | string | null | undefined,
) {
  if (!checkDate) {
    return false;
  }
  return dateIsTodayOrBefore(checkDate);
}

export function nomDateIsTodayOrAfter(
  checkDate: Date | string | null | undefined,
) {
  if (!checkDate) {
    return true;
  }
  return dateIsTodayOrAfter(checkDate);
}

export function nomDateIsBeforeToday(
  checkDate: Date | string | null | undefined,
) {
  if (!checkDate) {
    return false;
  }
  return dateIsBeforeToday(checkDate);
}

export function nomDateIsAfterToday(
  checkDate: Date | string | null | undefined,
) {
  if (!checkDate) {
    return true;
  }
  return dateIsAfterToday(checkDate);
}

export function nomEarliestDate(
  date1: Date | string | null | undefined,
  date2: Date | string | null | undefined,
) {
  if (!date1 && !date2) {
    return null;
  }

  if (!date1 && !!date2) {
    return dateWithoutTimezone(date2);
  }

  if (!date2 && !!date1) {
    return dateWithoutTimezone(date1);
  }

  if (!!date1 && !!date2) {
    return earliestDate(date1, date2);
  }
}

export function nomLatestDate(
  date1: Date | string | null | undefined,
  date2: Date | string | null | undefined,
) {
  if (!date1 || !date2) {
    return null;
  }

  return latestDate(date1, date2);
}

export function isPeriodActive(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return nomDateIsTodayOrBefore(gyldigFom) && nomDateIsTodayOrAfter(gyldigTom);
}

export function isPeriodPast(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return nomDateIsBeforeToday(gyldigFom) && nomDateIsBeforeToday(gyldigTom);
}

export function isPeriodFuture(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return nomDateIsAfterToday(gyldigFom) && nomDateIsAfterToday(gyldigTom);
}

export function isPeriodActiveOrPast(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return (
    isPeriodActive(gyldigFom, gyldigTom) || isPeriodPast(gyldigFom, gyldigTom)
  );
}

export function isPeriodActiveOrFuture(
  gyldigFom: Date | string,
  gyldigTom: Date | string | undefined,
) {
  return (
    isPeriodActive(gyldigFom, gyldigTom) || isPeriodFuture(gyldigFom, gyldigTom)
  );
}
