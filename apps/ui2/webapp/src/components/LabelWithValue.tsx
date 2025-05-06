import { HelpText, HStack } from "@navikt/ds-react";
import React from "react";

export function LabelWithValue({
  label,
  value,
  helpText,
  wrapped = false,
  displayEmpty = false,
}: {
  label: string;
  value: string | null | undefined;
  helpText?: string;
  wrapped?: boolean;
  displayEmpty?: boolean;
}) {
  if (!value && !displayEmpty) {
    return <></>;
  }

  if (wrapped) {
    return (
      <span>
        {label}: <ValueView helpText={helpText} value={value} />
      </span>
    );
  }

  return (
    <>
      <span>{label}:</span>
      <ValueView helpText={helpText} value={value} />
    </>
  );
}

function ValueView({
  value,
  helpText,
}: {
  value: string | null | undefined;
  helpText?: string;
}) {
  if (helpText) {
    return (
      <HStack gap="2">
        <b>{value}</b>
        <HelpText>{helpText}</HelpText>
      </HStack>
    );
  }

  return <b>{value}</b>;
}
