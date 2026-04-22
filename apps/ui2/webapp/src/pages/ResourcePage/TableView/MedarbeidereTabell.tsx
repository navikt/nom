import { css } from "@emotion/react";
import { VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import React, { useState } from "react";

import { getFysiskeBrukersteder } from "~/api/react-query-endpoints";
import ContentWrapper from "~/components/ContentWrapper";
import { PageBanner } from "~/components/PageBanner";
import { FysiskBrukerstedSelect } from "~/components/select/FysiskBrukerstedSelect";

export function MedarbeidereTabell() {
  return (
    <>
      <PageBanner text={"Medarbeidere i tabell"} />

      <ContentWrapper>
        <MedarbeiderTabell />
      </ContentWrapper>
    </>
  );
}

function MedarbeiderTabell() {
  const fysiskeBrukerstederQuery = useQuery(getFysiskeBrukersteder);
  const fysiskeBrukersteder = fysiskeBrukerstederQuery.data || [];

  const [fysiskBrukerstedFilter, setFysiskBrukerstedFilter] = useState("");

  console.log(fysiskBrukerstedFilter);

  return (
    <VStack
      css={css`
        background-color: #deecef;
        padding: var(--a-spacing-4) var(--a-spacing-6);
        border-radius: var(--a-border-radius-large);
      `}
      gap="4"
    >
      <FysiskBrukerstedSelect
        fysiskeBrukersteder={fysiskeBrukersteder}
        onChange={(value) => setFysiskBrukerstedFilter(value ?? "")}
        value={fysiskBrukerstedFilter}
      />
    </VStack>
  );
}
