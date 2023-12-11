import React from "react";

export function LabelWithValue({
  label,
  value,
  wrapped = false,
}: {
  label: string;
  value: string | null | undefined;
  wrapped?: boolean;
}) {
  if (!value) {
    return <></>;
  }

  if (wrapped) {
    return (
      <span>
        {label}: <b>{value}</b>
      </span>
    );
  }

  return (
    <>
      <span>{label}:</span>
      <b>{value}</b>
    </>
  );
}
