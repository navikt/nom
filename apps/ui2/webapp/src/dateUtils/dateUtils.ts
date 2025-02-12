import { formatISO, isAfter, isBefore, isSameDay } from "date-fns";

export function formatDateWithoutTimezone(date: Date) {
  return formatISO(date, { representation: "date" });
}

export function dateWithoutTimezone(date: Date | string) {
  return new Date(formatDateWithoutTimezone(new Date(date)));
}

export function todayWithoutTimezone() {
  return dateWithoutTimezone(new Date());
}

export function dateIsSame(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return isSameDay(
    dateWithoutTimezone(new Date(checkDate)),
    dateWithoutTimezone(new Date(compareDate)),
  );
}

export function dateIsSameOrBefore(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return (
    dateIsSame(checkDate, compareDate) ||
    isBefore(dateWithoutTimezone(checkDate), dateWithoutTimezone(compareDate))
  );
}

export function dateIsSameOrAfter(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return (
    dateIsSame(checkDate, compareDate) ||
    isAfter(dateWithoutTimezone(checkDate), dateWithoutTimezone(compareDate))
  );
}

export function dateIsBefore(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return !dateIsSameOrAfter(checkDate, compareDate);
}

export function dateIsAfter(
  checkDate: Date | string,
  compareDate: Date | string,
) {
  return !dateIsSameOrBefore(checkDate, compareDate);
}

export function dateIsToday(checkDate: Date | string) {
  return dateIsSame(checkDate, todayWithoutTimezone());
}

export function dateIsTodayOrBefore(checkDate: Date | string) {
  return dateIsSameOrBefore(checkDate, todayWithoutTimezone());
}

export function dateIsTodayOrAfter(checkDate: Date | string) {
  return dateIsSameOrAfter(checkDate, todayWithoutTimezone());
}

export function dateIsBeforeToday(checkDate: Date | string) {
  return dateIsBefore(checkDate, todayWithoutTimezone());
}

export function dateIsAfterToday(checkDate: Date | string) {
  return dateIsAfter(checkDate, todayWithoutTimezone());
}

export function earliestDate(date1: Date | string, date2: Date | string) {
  return dateIsSameOrBefore(date1, date2)
    ? dateWithoutTimezone(date1)
    : dateWithoutTimezone(date2);
}

export function latestDate(date1: Date | string, date2: Date | string) {
  return dateIsSameOrAfter(date1, date2)
    ? dateWithoutTimezone(date1)
    : dateWithoutTimezone(date2);
}

export function earliestDateInList(dates: (Date | string | undefined)[]) {
  const dateDates = dates.map((date) =>
    date ? dateWithoutTimezone(new Date(date)) : undefined,
  );

  const sortedDates = dateDates.sort((a, b) => {
    if (!a && !b) {
      return 0;
    }

    if (!a) {
      return 1;
    }
    if (!b) {
      return -1;
    }

    if (isSameDay(a, b)) {
      return 0;
    }

    return dateIsSameOrBefore(a, b) ? -1 : 1;
  });

  return sortedDates[0];
}
