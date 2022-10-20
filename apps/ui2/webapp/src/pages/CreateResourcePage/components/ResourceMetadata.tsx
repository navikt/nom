import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";

import type { FolkeregisterPerson } from "../../../api/generated/graphql_generated";

const contentStyles = css({
  backgroundColor: "var(--nom-secondary-hover)",
  border: "1px solid var(--navds-global-color-gray-400)",
  borderRadius: "4px",
  padding: "1rem",
});

export const ResourceMetadata = ({
  person,
}: {
  person: FolkeregisterPerson;
}) => {
  return (
    <div className={contentStyles}>
      <Heading size="small">Opplysninger fra Folkeregisteret</Heading>
      <div
        className={css`
          margin-top: var(--navds-spacing-6);
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
