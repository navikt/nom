import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";

import type { FolkeregisterPerson } from "~/api/generated/graphql_generated";

const contentStyles = css`
  background-color: var(--nom-secondary-hover);
  border: 1px solid var(--a-gray-400);
  border-radius: 4px;
  padding: 1rem;
`;

export const ResourceMetadata = ({
  person,
}: {
  person: FolkeregisterPerson;
}) => {
  return (
    <div css={contentStyles}>
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
        <b>{person.navn.fornavn}</b>
        {person.navn.mellomnavn ? (
          <>
            <span>Mellomnavn</span>
            <b>{person.navn.mellomnavn}</b>
          </>
        ) : undefined}
        <span>Etternavn</span>
        <b>{person.navn.etternavn}</b>
      </div>
    </div>
  );
};
