import { css } from "@emotion/react";
import { BodyShort, ErrorMessage, HelpText, Label } from "@navikt/ds-react";
import type { ReactElement, ReactNode } from "react";
import React, { forwardRef } from "react";
import type { FieldError } from "react-hook-form";
import type {
  CSSObjectWithLabel,
  GroupBase,
  MenuListProps,
  Props,
} from "react-select";
import Select, { components } from "react-select";
import type { AsyncProps } from "react-select/async";
import AsyncSelect from "react-select/async";

const commonSelectCss = css`
  border-radius: var(--ax-radius-4);
  border: 1px solid var(--ax-border-neutral-strong);

  width: 100%;
  height: 48px;

  color: var(--ax-text-neutral);

  > div {
    cursor: text;
    height: 100%;
  }

  :focus-within {
    outline: 3px solid var(--ax-border-focus);
    outline-offset: 3px;
  }
`;

const commonSelectOverwriteStyles = {
  clearIndicator: (base: CSSObjectWithLabel) => ({
    ...base,
    cursor: "pointer",
  }),
  // Removes default focus-border, so it can be replaced with focus from DesignSystem
  control: (base: CSSObjectWithLabel) => ({
    ...base,
    boxShadow: "none",
    border: 0,
  }),
  // Give a high zIndex so that a long result list will overflow from inside a Modal
  menuPortal: (base: CSSObjectWithLabel) => ({ ...base, zIndex: 9999 }),
  // Make border and size of input box to be identical with those from DesignSystem
  valueContainer: (base: CSSObjectWithLabel) => ({
    ...base,
    padding: "4px",
    color: "black",
  }),
  // Remove separator
  indicatorSeparator: (base: CSSObjectWithLabel) => ({
    ...base,
    display: "none",
  }),
  dropdownIndicator: (base: CSSObjectWithLabel) => ({
    ...base,
    color: "var(--ax-text-neutral)",
  }),
  placeholder: (base: CSSObjectWithLabel) => ({
    ...base,
    color: "var(--ax-text-neutral)",
  }),
  noOptionsMessage: (base: CSSObjectWithLabel) => ({
    ...base,
    color: "var(--ax-text-neutral)",
  }),
};

function handleKeyDown(event: React.KeyboardEvent) {
  const input = event.target as HTMLInputElement;

  if (event.key === "Home") {
    if (event.shiftKey) {
      input.setSelectionRange(0, input.selectionEnd);
    } else {
      input.setSelectionRange(0, 0);
    }
    event.preventDefault();
  }

  if (event.key === "End") {
    if (event.shiftKey) {
      input.setSelectionRange(input.selectionStart, input.value.length);
    } else {
      input.setSelectionRange(input.value.length, input.value.length);
    }
    event.preventDefault();
  }
}

type AsyncSearch = <
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(
  properties: AsyncProps<Option, IsMulti, Group> & { ref?: unknown },
) => ReactElement;

export const AsyncSearch = forwardRef(
  <Option, IsMulti extends boolean, Group extends GroupBase<Option>>(
    properties: AsyncProps<Option, IsMulti, Group>,
    reference?: unknown,
  ) => {
    return (
      <AsyncSelect
        cacheOptions
        css={commonSelectCss}
        isClearable
        loadingMessage={() => "Søker..."}
        menuPortalTarget={document.body}
        // @ts-expect-error -- gidder ikke
        ref={reference}
        styles={{
          ...commonSelectOverwriteStyles,
          dropdownIndicator: (base) => ({ ...base, display: "none" }),
        }}
        {...properties}
        components={{ MenuList: CustomMenuList, ...properties.components }}
        onKeyDown={handleKeyDown}
      />
    );
  },
) as AsyncSearch;

export function BasicSelect<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(properties: Props<Option, IsMulti, Group>) {
  return (
    <Select
      isClearable
      {...properties}
      components={{ MenuList: CustomMenuList, ...properties.components }}
      css={commonSelectCss}
      escapeClearsValue
      isSearchable
      menuPosition="fixed"
      placeholder={properties.placeholder ?? ""}
      styles={commonSelectOverwriteStyles}
    />
  );
}

/**
 * Limit the number of items in the displayed list to 30 to help performance.
 * It is much more efficient to type a more specific search rather than look through a dropdown list of more than 100 items.
 */
function CustomMenuList<
  Option,
  IsMulti extends boolean = false,
  Group extends GroupBase<Option> = GroupBase<Option>,
>(properties: MenuListProps<Option, IsMulti, Group>) {
  const { children } = properties;
  const filteredChildren = Array.isArray(children)
    ? children.slice(0, 30)
    : children;
  return (
    <components.MenuList {...properties}>
      {filteredChildren}
    </components.MenuList>
  );
}

export function SelectLayoutWrapper({
  label,
  children,
  htmlFor,
  helpText,
  error,
  description,
}: {
  label: string;
  children: ReactNode;
  htmlFor: string;
  helpText?: string;
  error?: FieldError;
  description?: string;
}) {
  return (
    <div
      css={[
        css`
          display: flex;
          flex-direction: column;
          gap: var(--ax-space-8);
        `,
        error && errorCss,
      ]}
    >
      <Label
        css={css`
          display: flex;
          gap: var(--ax-space-8);
          align-items: center;
        `}
        htmlFor={htmlFor}
      >
        {label}
        {helpText ? (
          <HelpText strategy="fixed" title="Mer informasjon">
            {helpText}
          </HelpText>
        ) : undefined}
      </Label>
      {description && <BodyShort>{description}</BodyShort>}
      {children}
      {error && <ErrorMessage>{error.message}</ErrorMessage>}
    </div>
  );
}

const errorCss = css`
  > div {
    border-color: var(--ax-border-danger-strong);
    box-shadow: 0 0 0 1px var(--ax-border-danger-strong);
  }
`;
