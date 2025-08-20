import { CopyButton, HelpText, HStack } from "@navikt/ds-react";
import React from "react";

export function LabelWithValue({
  label,
  value,
  helpText,
  wrapped = false,
  displayEmpty = false,
  copyButton = false,
}: {
  label: string;
  value: string | null | undefined;
  helpText?: string;
  wrapped?: boolean;
  displayEmpty?: boolean;
  copyButton?: boolean;
}) {
  if (!value && !displayEmpty) {
    return <></>;
  }

  if (wrapped) {
    return (
      <span>
        {label}:{" "}
        <ValueView copyButton={copyButton} helpText={helpText} value={value} />
      </span>
    );
  }

  return (
    <>
      <span>{label}:</span>
      <ValueView copyButton={copyButton} helpText={helpText} value={value} />
    </>
  );
}

function ValueView({
  value,
  helpText,
  copyButton,
}: {
  value: string | null | undefined;
  helpText?: string;
  copyButton: boolean;
}) {
  return (
    <HStack gap="2">
      <b>{value}</b>
      {copyButton && value && <CopyButton copyText={value} size="xsmall" />}
      {helpText && <HelpText>{helpText}</HelpText>}
    </HStack>
  );
}
