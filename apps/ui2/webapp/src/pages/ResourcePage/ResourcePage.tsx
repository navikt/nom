import { useQuery } from "@tanstack/react-query";
import { useParams } from "@tanstack/react-router";
import React from "react";

import { getRessurs } from "~/api/react-query-endpoints";
import { DetailedRessurs } from "~/pages/ResourcePage/RessursComponents/DetailedRessurs";
import { RessursCard } from "~/pages/ResourcePage/RessursComponents/RessursCard";
import { isBeforePresent } from "~/utils";

import ContentWrapper from "../../components/ContentWrapper";

export const ResourcePage = () => {
  const { id } = useParams({ from: "/ressurs/$id" });

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
