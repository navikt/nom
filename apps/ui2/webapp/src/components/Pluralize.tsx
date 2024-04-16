import type { ReactNode } from "react";

export function Pluralize({
  numberOfElements,
  singleForm,
  pluralForm,
}: {
  numberOfElements: number;
  singleForm?: ReactNode;
  pluralForm?: ReactNode;
}) {
  if (numberOfElements === 1) {
    return singleForm;
  }
  return pluralForm;
}
