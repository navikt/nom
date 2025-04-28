import { css } from "@emotion/react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { useQuery } from "@tanstack/react-query";
import { useParams, useSearch } from "@tanstack/react-router";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import { subYears } from "date-fns";
import React from "react";

import { getRessurs } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { PageBanner } from "~/components/PageBanner";
import { DetailedRessurs } from "~/pages/ResourcePage/RessursComponents/DetailedRessurs";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import {
  dateWithoutTimezone,
  formatVisningsnavn,
  isSameDateOrAfter,
  todayWithoutTimezone,
} from "~/utils";

import ContentWrapper from "../../components/ContentWrapper";

export const ResourcePage = () => {
  const { id } = useParams({ from: "/ressurs/$id" });

  const { editOrgtilknytninger } = useSearch({ from: "/ressurs/$id" });

  const ressursQuery = useQuery({
    queryKey: getRessurs.queryKey(id),
    queryFn: () => getRessurs.queryFn(id),
  });

  const ressurs = ressursQuery.data;

  const hasAdminAccess = useHasAdminAccess();

  const sluttetOverEtArSiden =
    !!ressurs?.sluttdato &&
    !isSameDateOrAfter(
      dateWithoutTimezone(ressurs.sluttdato),
      subYears(todayWithoutTimezone(), 1),
    );

  return (
    <>
      {editOrgtilknytninger && <PageBanner text="Endre medarbeider" />}

      <ContentWrapper>
        {editOrgtilknytninger && !!ressurs && (
          <ReactRouterLink
            params={{ id: ressurs.navident }}
            to={`/ressurs/$id`}
          >
            <div
              css={css`
                color: #000;
                font-size: 1.2rem;
                margin-bottom: var(--a-spacing-4);
                display: flex;
                align-items: center;
              `}
            >
              <ChevronLeftIcon fontSize="1.8rem" />
              <b>Tilbake til {formatVisningsnavn(ressurs.visningsnavn)}</b>
            </div>
          </ReactRouterLink>
        )}

        {ressurs && (
          <>
            {sluttetOverEtArSiden && !hasAdminAccess ? (
              <RessursCard ressurs={ressurs} sluttetOverEtArSiden />
            ) : (
              <DetailedRessurs ressurs={ressurs} />
            )}
          </>
        )}
      </ContentWrapper>
    </>
  );
};
