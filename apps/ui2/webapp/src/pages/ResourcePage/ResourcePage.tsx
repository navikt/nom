import { css } from "@emotion/react";
import { ChevronLeftIcon } from "@navikt/aksel-icons";
import { LocalAlert } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useParams, useSearch } from "@tanstack/react-router";
import { subYears } from "date-fns";
import React from "react";

import { getRessurs } from "~/api/react-query-endpoints";
import {
  useHasAdminAccess,
  useHasAdminOrWriteAccess,
  useHasRobotAccess,
  useHasUtenlandskKonsulentAccess,
  useHasVikafossenAccess,
} from "~/api/useGetSession";
import { ChristmasBanner } from "~/components/ChristmasBanner";
import { SmartLink } from "~/components/navigation/SmartLink";
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
  const canSeeErrorsUseAccess =
    useHasAdminOrWriteAccess() ||
    useHasVikafossenAccess() ||
    useHasRobotAccess() ||
    useHasUtenlandskKonsulentAccess();

  const sluttetOverEtArSiden =
    !!ressurs?.sluttdato &&
    !isSameDateOrAfter(
      dateWithoutTimezone(ressurs.sluttdato),
      subYears(todayWithoutTimezone(), 1),
    );

  const hasMissingFysiskBrukersted = !!ressurs?.primaryFysiskBrukersted;

  return (
    <>
      {editOrgtilknytninger ? (
        <PageBanner text="Endre medarbeider" />
      ) : (
        <ChristmasBanner />
      )}

      <ContentWrapper>
        {editOrgtilknytninger && !!ressurs && (
          <SmartLink
            css={css`
              display: flex;
              width: fit-content;
              margin-bottom: var(--a-spacing-4);
              color: #000;
              font-size: 1.2rem;
            `}
            href={`/ressurs/$id`}
          >
            <span
              css={css`
                display: flex;
                align-items: center;
              `}
            >
              <ChevronLeftIcon fontSize="1.8rem" />
              <b>Tilbake til {formatVisningsnavn(ressurs.visningsnavn)}</b>
            </span>
          </SmartLink>
        )}
        {!hasMissingFysiskBrukersted && canSeeErrorsUseAccess && (
          <>
            <LocalAlert
              css={css`
                margin-bottom: var(--a-spacing-4);
              `}
              status="warning"
            >
              <LocalAlert.Header>
                <LocalAlert.Title>
                  {formatVisningsnavn(ressurs?.visningsnavn) +
                    " mangler fysisk brukersted."}
                </LocalAlert.Title>
              </LocalAlert.Header>
              <LocalAlert.Content>
                {formatVisningsnavn(ressurs?.visningsnavn) +
                  " mangler fysisk brukersted, det må rettes opp før det kan gjøres andre endringer. Trykk 'Endre personopplysninger' og velg riktig fysisk brukersted."}
              </LocalAlert.Content>
            </LocalAlert>
          </>
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
