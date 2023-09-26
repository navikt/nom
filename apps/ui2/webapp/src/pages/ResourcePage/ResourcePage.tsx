import { css } from "@emotion/react";
import { Alert, Link } from "@navikt/ds-react";
import { useParams, useSearch } from "@tanstack/react-router";
import { isBefore } from "date-fns";
import React from "react";

import type { Ressurs } from "~/api/generated/graphql_generated";
import { DetailedRessurs } from "~/pages/ResourcePage/RessursCards/DetailedRessurs";
import { RessursHasQuit } from "~/pages/ResourcePage/RessursCards/RessursHasQuit";
import { ressursRoute } from "~/reactRouterRoutes";

import * as Gq from "../../api/generated/graphql_generated";
import ContentWrapper from "../../components/ContentWrapper";

export const ResourcePage = () => {
  const { id } = useParams({ from: ressursRoute.id });
  const { alreadyExists } = useSearch({ from: ressursRoute.id });

  const hentRessursQuery = Gq.useHentRessursMedNavidentQuery({
    variables: { navident: id },
  });

  const ressurs = hentRessursQuery.data?.ressurs;

  const hasQuit =
    ressurs?.sluttdato && isBefore(new Date(ressurs.sluttdato), new Date());
  return (
    <ContentWrapper>
      {hentRessursQuery.data?.ressurs && (
        <>
          {alreadyExists ? (
            <Alert
              css={css`
                margin-bottom: var(--a-spacing-4);
              `}
              variant="info"
            >
              Medarbeider finnes allerede i NOM. Statlige medarbeidere må også
              etableres i{" "}
              <Link href="https://agresso.adeo.no/agresso/">Unit4</Link>.
              Tilganger tildeles i identrutina i{" "}
              <Link href="https://it-hjelpa.adeo.no/arsys/home">Remedy</Link>{" "}
              for alle medarbeidere.
            </Alert>
          ) : undefined}
          {hasQuit ? (
            <RessursHasQuit
              ressurs={hentRessursQuery.data.ressurs as Ressurs}
            />
          ) : (
            <DetailedRessurs
              ressurs={hentRessursQuery.data.ressurs as Ressurs}
            />
          )}
        </>
      )}
    </ContentWrapper>
  );
};
