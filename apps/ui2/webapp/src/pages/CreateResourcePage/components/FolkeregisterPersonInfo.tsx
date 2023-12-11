import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";

import type { FolkeregisterPerson } from "~/types/api-models";

const backgroundBox = css`
  background-color: var(--nom-secondary-hover);
  border: 1px solid var(--a-gray-400);
  border-radius: 4px;
  padding: var(--a-spacing-4);
`;

export function FolkeregisterPersonInfo({
  folkeregisterPerson,
}: {
  folkeregisterPerson: FolkeregisterPerson;
}) {
  return (
    <div css={backgroundBox}>
      <Heading size="small">Opplysninger fra Folkeregisteret</Heading>
      <div
        css={css`
          margin-top: var(--a-spacing-6);
          display: grid;
          grid-auto-flow: column;
          grid-template-rows: 1fr 1fr;
        `}
      >
        <span>Fornavn</span>
        <b>{folkeregisterPerson.navn.fornavn}</b>
        {folkeregisterPerson.navn.mellomnavn ? (
          <>
            <span>Mellomnavn</span>
            <b>{folkeregisterPerson.navn.mellomnavn}</b>
          </>
        ) : undefined}
        <span>Etternavn</span>
        <b>{folkeregisterPerson.navn.etternavn}</b>
      </div>
    </div>
  );
}
