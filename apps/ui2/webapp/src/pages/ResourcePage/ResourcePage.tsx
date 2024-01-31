import { css } from "@emotion/react";
import { Alert, Link } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams, useSearch } from "@tanstack/react-router";
import React from "react";

import { getRessurs } from "~/api/react-query-endpoints";
import { DetailedRessurs } from "~/pages/ResourcePage/RessursComponents/DetailedRessurs";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import { isBeforePresent } from "~/utils";

import ContentWrapper from "../../components/ContentWrapper";

export const ResourcePage = () => {
  const { id } = useParams({ from: "/ressurs/$id" });
  const { alreadyExists } = useSearch({ from: "/ressurs/$id" });

  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(id),
    queryFn: () => getRessurs.queryFn(id),
  });

  const ressurs = ressursQuery.data;

  const hasQuit =
    ressurs?.sluttdato && isBeforePresent(new Date(ressurs.sluttdato));

  return (
    <ContentWrapper>
      {ressurs && (
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
            <RessursCard hasQuit ressurs={ressurs} />
          ) : (
            <DetailedRessurs ressurs={ressurs} />
          )}
        </>
      )}
    </ContentWrapper>
  );
};
