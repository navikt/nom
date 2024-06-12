import { css } from "@emotion/react";
import { CheckmarkCircleFillIcon } from "@navikt/aksel-icons";
import { Timeline } from "@navikt/ds-react";
import { useQueries } from "@tanstack/react-query";
import { addMonths, addYears, subMonths } from "date-fns";
import React from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import type { OrgtilknytningState } from "~/types/api-models";
import { formatDisplayDate } from "~/utils";

export function OrgtilknytningTimeline({
  orgtilknytninger,
}: {
  orgtilknytninger: Partial<OrgtilknytningState>[];
}) {
  const orgenhetIdListe = [
    ...new Set(
      orgtilknytninger.flatMap(
        (orgtilknytning) => orgtilknytning.orgEnhetId || [],
      ),
    ),
  ];

  // TODO: ønsket sortering?
  const orgenheter = useQueries({
    queries: orgenhetIdListe.map((id) => ({
      queryKey: getOrgEnhet.queryKey(id),
      queryFn: () => getOrgEnhet.queryFn(id),
    })),
    combine: (results) => results.flatMap((r) => r.data ?? []),
  });

  const dateWhenGyldigTomNotSet = addYears(new Date(), 5);

  return (
    <div
      css={css`
        background: #fff;
        padding: var(--a-spacing-6) var(--a-spacing-4);
        border-radius: 8px;
        border: 1px solid var(--a-border-subtle);
      `}
    >
      <Timeline
        endDate={addMonths(new Date(), 10)}
        startDate={subMonths(new Date(), 2)}
      >
        <Timeline.Pin date={new Date()}>
          <p>I dag</p>
        </Timeline.Pin>
        {orgenheter.map((orgenhet) => {
          const orgenhetTilknytninger = orgtilknytninger.filter(
            (orgtilknytning) => orgtilknytning.orgEnhetId === orgenhet.id,
          );

          return (
            <Timeline.Row key={orgenhet.id} label={orgenhet.navn}>
              {orgenhetTilknytninger
                .filter((orgtilknytning) => orgtilknytning.gyldigFom)
                .map((orgtilknytning, index) => {
                  if (!orgtilknytning.gyldigFom) {
                    return <></>;
                  }

                  return (
                    <Timeline.Period
                      end={
                        new Date(
                          orgtilknytning.gyldigTom || dateWhenGyldigTomNotSet,
                        )
                      }
                      icon={
                        orgtilknytning.erDagligOppfolging ? (
                          <DagligOppfolgingIcon />
                        ) : undefined
                      }
                      key={index}
                      start={new Date(orgtilknytning.gyldigFom)}
                      status="success"
                      statusLabel="Testlabel"
                    >
                      {`${formatDisplayDate(orgtilknytning.gyldigFom)} - ${formatDisplayDate(orgtilknytning.gyldigTom)}`}
                    </Timeline.Period>
                  );
                })}
            </Timeline.Row>
          );
        })}
      </Timeline>
    </div>
  );
}

function DagligOppfolgingIcon() {
  return (
    <div
      css={css`
        display: flex;
        align-items: center;
        margin-left: 2px;
      `}
    >
      <CheckmarkCircleFillIcon />
      <span
        css={css`
          margin-left: var(--a-spacing-2);
        `}
      >
        <b>Daglig oppfølging</b>
      </span>
    </div>
  );
}
