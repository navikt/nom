import { css } from "@emotion/react";
import { CopyButton, HelpText, HStack } from "@navikt/ds-react";
import React from "react";

export function LabelWithValue({
  label,
  value,
  helpText,
  split = false,
  displayEmpty = false,
  copyButton = false,
}: {
  label: string;
  value: string | null | undefined;
  helpText?: string;
  split?: boolean;
  displayEmpty?: boolean;
  copyButton?: boolean;
}) {
  if (!value && !displayEmpty) {
    return <></>;
  }

  if (split) {
    return (
      <>
        <span>{label}:</span>
        <InformationView
          copyButton={copyButton}
          helpText={helpText}
          value={value}
        />
      </>
    );
  }

  return (
    <span
      css={css`
        display: flex;
      `}
    >
      <InformationView
        copyButton={copyButton}
        helpText={helpText}
        label={label}
        value={value}
      />
    </span>
  );
}

function InformationView({
  label,
  value,
  helpText,
  copyButton,
}: {
  label?: string;
  value: string | null | undefined;
  helpText?: string;
  copyButton: boolean;
}) {
  return (
    <HStack gap="2">
      <span>
        {label && <>{label}: </>}
        <b>{value}</b>
      </span>
      {copyButton && value && <CopyButton copyText={value} size="xsmall" />}
      {helpText && <HelpText>{helpText}</HelpText>}
    </HStack>
  );
}
