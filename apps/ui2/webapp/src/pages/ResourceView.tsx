import { css } from "@emotion/react";
import { Alert, Link } from "@navikt/ds-react";
import { useParams } from "react-router";
import { useLocation } from "react-router-dom";

import type { Ressurs } from "~/api/generated/graphql_generated";

import * as Gq from "../api/generated/graphql_generated";
import ContentWrapper from "../components/ContentWrapper";
import { FocusedRessursView } from "./OrganisasjonsHierarkiPage/components/FocusedRessursView";

export const ResourceView = () => {
  const { id } = useParams();
  const { state } = useLocation();
  const hentRessursQuery = Gq.useHentRessursMedNavidentQuery({
    variables: { navident: id },
  });

  return (
    <ContentWrapper>
      <div
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-4);
        `}
      >
        {hentRessursQuery.loading && (
          <div>...laster graphql data fra nom-api</div>
        )}
        {hentRessursQuery.data?.ressurs && (
          <>
            {state?.alreadyExists ? (
              <Alert variant="info">
                Ressurs finnes allerede i NOM. Statlige ressurser må også
                etableres i{" "}
                <Link href="https://agresso.adeo.no/agresso/">Unit4</Link>.
                Tilganger tildeles i identrutina i{" "}
                <Link href="https://it-hjelpa.adeo.no/arsys/home">Remedy</Link>{" "}
                for alle ressurser.
              </Alert>
            ) : undefined}
            <FocusedRessursView
              focusedRessurs={hentRessursQuery.data.ressurs as Ressurs}
            />
          </>
        )}
      </div>
    </ContentWrapper>
  );
};
