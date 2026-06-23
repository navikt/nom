import { css } from "@emotion/react";
import { CheckmarkCircleFillIcon } from "@navikt/aksel-icons";
import { VStack } from "@navikt/ds-react";
import type { ReactNode } from "react";
import React from "react";

import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { PanelLink } from "~/pages/CreateResourcePage/SummaryStep";

export function OrgEnhetKvittering({
  title,
  children,
  orgEnhetId,
}: {
  title: string;
  children: ReactNode;
  orgEnhetId: string;
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

      <VStack
        css={css`
          width: 70%;
          margin: auto;
        `}
        gap="4"
      >
        <PanelLink
          href={`/org/${orgEnhetId}`}
          label="Gå til organisasjonsenhet"
        />

        <PanelLink
          href="/organisasjonsmastring"
          label="Gå tilbake til forsiden"
        />
      </VStack>
    </ContainerWithHighlightedTop>
  );
}
