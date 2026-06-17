import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";
import type { ReactNode } from "react";

import { blueTopBorder } from "~/resources/NomStyles";

export function ContainerWithHighlightedTop({
  children,
  title,
  icon,
  padding = 12,
  verticalPadding,
  horizontalPadding,
  gap = 9,
}: {
  children: ReactNode;
  title?: string | ReactNode;
  icon?: any;
  padding?: number;
  verticalPadding?: number;
  horizontalPadding?: number;
  gap?: number;
}) {
  const paddingValue = `var(--a-spacing-${verticalPadding ?? padding}) var(--a-spacing-${horizontalPadding ?? padding})`;
  const gapValue = `var(--a-spacing-${gap})`;

  return (
    <div
      css={[
        blueTopBorder,
        css`
          padding: ${paddingValue};
          display: flex;
          flex-direction: column;
          background: white;
          gap: ${gapValue};
        `,
      ]}
    >
      {(!!title || !!icon) && (
        <div
          css={css`
            display: flex;
            flex-direction: column;
            gap: var(--a-spacing-4);
          `}
        >
          {icon && typeof icon === "string" && (
            <img
              alt=""
              css={css`
                height: 65px;
                width: 65px;
                align-self: center;
              `}
              src={icon}
            />
          )}

          {icon && typeof icon !== "string" && (
            <div
              css={css`
                align-self: center;
                font-size: 65px;
              `}
            >
              {icon}
            </div>
          )}

          {title && (
            <Heading
              css={css`
                align-self: center;
              `}
              size="large"
            >
              {title}
            </Heading>
          )}
        </div>
      )}
      {children}
    </div>
  );
}
