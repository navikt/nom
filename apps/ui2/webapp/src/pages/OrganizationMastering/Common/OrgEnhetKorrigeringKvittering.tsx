import { css } from "@emotion/react";
import { CheckmarkCircleFillIcon } from "@navikt/aksel-icons";
import type { ReactNode } from "react";

import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";

export function OrgEnhetKorrigeringKvittering({
  title,
  children,
}: {
  title: string;
  children: ReactNode;
}) {
  return (
    <ContainerWithHighlightedTop
      icon={
        <CheckmarkCircleFillIcon
          css={css`
            color: green;
          `}
        />
      }
      title={title}
    >
      {children}
    </ContainerWithHighlightedTop>
  );
}
