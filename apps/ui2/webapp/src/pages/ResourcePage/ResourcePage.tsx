import { Alert, Link } from "@navikt/ds-react";
import { isBefore } from "date-fns";
import React from "react";
import { useParams } from "react-router";
import { useLocation } from "react-router-dom";

import type { Ressurs } from "~/api/generated/graphql_generated";
import { DetailedRessurs } from "~/pages/ResourcePage/RessursCards/DetailedRessurs";
import { RessursHasQuit } from "~/pages/ResourcePage/RessursCards/RessursHasQuit";

import * as Gq from "../../api/generated/graphql_generated";
import ContentWrapper from "../../components/ContentWrapper";

export const ResourcePage = () => {
  const { id } = useParams();
  const { state } = useLocation();
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
          {state?.alreadyExists ? (
            <Alert variant="info">
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
