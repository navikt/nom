import { css } from "@emotion/react";
import { Label } from "@navikt/ds-react";
import type { OptionProps } from "react-select";
import { components } from "react-select";

type Option = { value: string; label: string };

export function OptionWithValue(properties: OptionProps<Option, false>) {
  return (
    <components.Option {...properties}>
      <div
        css={css`
          display: flex;
          gap: var(--a-spacing-4);
        `}
      >
        <Label
          css={css`
            min-width: 50px;
          `}
          size="small"
        >
          {properties.data.value}
        </Label>
        <span>{properties.data.label}</span>
      </div>
    </components.Option>
  );
}
