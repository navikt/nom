import { useQuery } from "@tanstack/react-query";
import { useParams, useSearch } from "@tanstack/react-router";
import { subYears } from "date-fns";
import React from "react";

import { getRessurs } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import { PageBanner } from "~/components/PageBanner";
import { DetailedRessurs } from "~/pages/ResourcePage/RessursComponents/DetailedRessurs";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import {
  dateWithoutTimezone,
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
