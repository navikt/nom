import { css } from "@emotion/react";
import { CopyButton, HelpText, HStack } from "@navikt/ds-react";
import React from "react";

import { SmartLink } from "~/components/navigation/SmartLink";

export function LabelWithValue({
  label,
  value,
  helpText,
  split = false,
  displayEmpty = false,
  copyButton = false,
  valueHref,
}: {
  label: string;
  value: string | null | undefined;
  helpText?: string;
  split?: boolean;
  displayEmpty?: boolean;
  copyButton?: boolean;
  valueHref?: string;
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
          valueHref={valueHref}
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
        valueHref={valueHref}
      />
    </span>
  );
}

function InformationView({
  label,
  value,
  helpText,
  copyButton,
  valueHref,
}: {
  label?: string;
  value: string | null | undefined;
  helpText?: string;
  copyButton: boolean;
  valueHref?: string;
}) {
  return (
    <HStack gap="2">
      <span>
        {label && <>{label}: </>}
        <b>
          {!!valueHref ? (
            <SmartLink href={valueHref} target="_blank">
              {value}
            </SmartLink>
          ) : (
            value
          )}
        </b>
      </span>
      {copyButton && value && <CopyButton copyText={value} size="xsmall" />}
      {helpText && <HelpText>{helpText}</HelpText>}
    </HStack>
  );
}
