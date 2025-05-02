import { css } from "@emotion/react";
import { ChevronRightIcon } from "@navikt/aksel-icons";
import { BodyShort, Heading } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React, { useEffect, useRef } from "react";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { UserImage } from "~/components/UserImage";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import building from "~/resources/building.svg";
import type { Ressurs } from "~/types/api-models";
import { formatVisningsnavn, isInDevelopment } from "~/utils";

export function OrgEnhetView({
  orgEnhetId,
  autoFocus = false,
}: {
  orgEnhetId: string;
  autoFocus?: boolean;
}) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId),
  });

  const orgEnhet = orgEnhetQuery.data;

  const reference = useRef<HTMLAnchorElement>(null);
  useEffect(() => {
    // Give autofocus on initial load.
    // This way the user can continue navigation from where they were before page change.
    if (autoFocus) {
      reference.current?.focus();
    }
  }, []);

  const gammel = !!orgEnhet
    ? !isInDevelopment() && gamleOrgEnheter.includes(orgEnhet.id)
    : false;

  return (
    <ReactRouterLink
      css={css`
        text-decoration: none;
        color: var(--a-gray-900);
      `}
      params={{ id: orgEnhetId }}
      ref={reference}
      to="/org/$id"
    >
      <UnitContainerLink gammel={gammel}>
        <VerticalEdge size="large">
          <img alt={"orgenhet"} src={building} />
        </VerticalEdge>
        <div
          css={css`
            margin: var(--a-spacing-3) 0;
          `}
        >
          <Heading level="2" size="small">
            {orgEnhet?.navn}
          </Heading>
          <BodyShort size="small">
            Leder:{" "}
            <b>{formatVisningsnavn(orgEnhet?.ledere[0]?.visningsnavn)}</b>
          </BodyShort>
        </div>
      </UnitContainerLink>
    </ReactRouterLink>
  );
}

export function ResourceLink({ ressurs }: { ressurs: Ressurs }) {
  if (!ressurs) {
    return <></>;
  }

  return (
    <ReactRouterLink
      css={css`
        text-decoration: none;
        color: var(--a-gray-900);

        > div {
          padding: var(--a-spacing-2);
        }
      `}
      params={{ id: ressurs.navident }}
      to="/ressurs/$id"
    >
      <UnitContainerLink>
        <UserImage
          navident={ressurs.navident}
          navn={formatVisningsnavn(ressurs.visningsnavn)}
          size="55px"
        />
        <div>
          <Heading level="2" size="small">
            {formatVisningsnavn(ressurs.visningsnavn)}
          </Heading>

          <BodyShort size="small">
            {SektorNameMapping[ressurs.sektor]}
          </BodyShort>
        </div>
      </UnitContainerLink>
    </ReactRouterLink>
  );
}

function UnitContainerLink({
  children,
  gammel = false,
}: {
  children: ReactNode;
  gammel?: boolean;
}) {
  return (
    <div
      css={css`
        width: 100%;
        height: 100%;
        border-radius: 5px;
        border: 1px solid var(--a-gray-400);
        background-color: ${gammel ? "var(--a-gray-300)" : "var(--a-white)"};
        display: grid;
        grid-template-columns: min-content 1fr 40px;
        align-items: center;
        gap: var(--a-spacing-4);
        cursor: pointer;

        &:hover {
          background: var(--nom-secondary-hover);
        }
      `}
    >
      {children}
      <ChevronRightIcon fontSize="var(--a-spacing-8)" />
    </div>
  );
}

export function VerticalEdge({
  size = "medium",
  children,
}: {
  size?: "large" | "medium";
  children: ReactNode;
}) {
  return (
    <div
      // Using outline with a border behind creates some weird lines showing through.
      // A hack around that to use 2px for the outline and -1 for the offset.
      css={css`
        outline: 2px solid var(--a-lightblue-800);
        outline-offset: -1px;
        background-color: var(--a-lightblue-800);
        display: flex;
        align-items: center;
        justify-content: center;
        width: ${size === "large" ? 42 : 24}px;
        border-radius: 4px 0 0 4px;
        height: 100%;
      `}
    >
      {children}
    </div>
  );
}

export function OverEnheterDivider() {
  return (
    <div
      css={css`
        margin-left: 65px;
        height: var(--a-spacing-4);
        background-color: var(--a-lightblue-800);
        width: 6px;
      `}
    />
  );
}

// Hardkodet listen over gamle enheter. Ikke god praksis, men dette er veldig midlertidig
const gamleOrgEnheter: string[] = [
  "py355m",
  "ke682s",
  "ra229y",
  "fu375w",
  "nu202a",
  "ba661e",
  "ku588f",
  "dy697h",
  "ty316c",
  "ju231d",
  "me676w",
  "sa273b",
  "me571z",
  "ry442a",
  "ju692r",
  "fe345a",
  "jy493d",
  "ru236h",
  "ge715w",
  "my278b",
  "ky442t",
  "ny289a",
  "fe923x",
  "ka377g",
  "fu531d",
  "gy551c",
  "ve212f",
  "fe698t",
  "sy841v",
  "tu298k",
  "pu375k",
  "ku230e",
  "du712x",
  "ha950x",
  "ke257t",
  "vy738a",
  "na684m",
  "ha898u",
  "be215m",
  "je882u",
  "ju174p",
  "ve798k",
  "ry508e",
  "pe231r",
  "ru419e",
  "hy484j",
  "ba760t",
  "pe101p",
  "pu661p",
  "hy534r",
  "vy551n",
  "my384j",
  "na894t",
  "re442w",
  "hy241h",
  "ma272e",
  "hy779z",
  "ja961e",
  "fy681s",
  "su606g",
  "pu113d",
  "fa496c",
  "du463b",
  "fe334s",
  "se714v",
  "na327d",
  "be460g",
  "ny983s",
  "gu916j",
  "ma132r",
  "je631x",
  "ne560a",
  "ne756m",
  "vy349t",
  "sa864x",
  "bu589v",
  "ju496y",
  "by307n",
  "he405b",
  "vu322j",
  "vy824j",
  "he399b",
  "vu747u",
  "ta152h",
  "sa568r",
  "ga519m",
  "gu706t",
  "ky689f",
  "pa655d",
  "va659x",
  "ju188y",
  "fe249f",
  "se887n",
  "te877w",
  "ry583p",
  "ru524s",
  "ky979c",
  "ry444c",
  "bu888w",
  "sa479w",
  "he318k",
  "da182a",
  "ha469y",
  "pa968y",
  "fe507n",
  "pa512h",
  "fe755w",
  "ve982v",
  "te277k",
  "ja556h",
  "ky442t",
  "ru236h",
  "ge715w",
  "tu298k",
  "sy841v",
  "pu375k",
  "ku230e",
  "ny289a",
  "fe923x",
  "ka377g",
  "fu531d",
  "fe698t",
  "ve212f",
  "gy551c",
  "du712x",
  "ke257t",
  "ha950x",
  "fy603b",
  "bu878j",
  "su717g",
  "ne837r",
  "ty486r",
  "ga184d",
  "be847f",
  "tu131a",
  "fu367r",
  "na918w",
  "te558v",
  "ke736j",
  "ke403y",
  "bu878z",
  "fe755w",
  "pa512h",
  "te277k",
  "ve982v",
  "se887n",
  "fe249f",
  "by307n",
  "vy824j",
  "he405b",
  "vu322j",
  "ta152h",
  "he399b",
  "sa568r",
  "vu747u",
  "ky689f",
  "ga519m",
  "gu706t",
  "va659x",
  "pa655d",
  "ju188y",
  "ha469y",
  "pa968y",
  "te877w",
  "ky979c",
  "ry583p",
  "ry444c",
  "bu888w",
  "ru524s",
  "he318k",
  "sa479w",
  "da182a",
  "hy534r",
  "pu661p",
  "na894t",
  "vy551n",
  "my384j",
  "pe231r",
  "hy484j",
  "ru419e",
  "ry508e",
  "fe507n",
  "re442w",
  "pe101p",
  "ba760t",
  "ve798k",
  "ma132r",
  "je631x",
  "ny983s",
  "gu916j",
  "hy241h",
  "sa864x",
  "vy349t",
  "ne560a",
  "bu589v",
  "ne756m",
  "ju496y",
  "na327d",
  "be460g",
  "fa496c",
  "se714v",
  "fe334s",
  "du463b",
  "fy681s",
  "ja961e",
  "hy779z",
  "ma272e",
  "su606g",
  "pu113d",
  "ha898u",
  "ju174p",
  "na684m",
  "be215m",
  "je882u",
  "ta911v",
  "ry299w",
  "da992d",
  "tu760a",
  "du589s",
  "va810k",
  "je898z",
  "vy738a",
  "ha603j",
  "sy287e",
  "ny732s",
  "ku268b",
  "fu761a",
  "re229d",
  "fe210d",
  "nu646b",
  "gu957p",
  "gy376z",
  "ke580s",
  "fe833w",
  "sy473w",
  "ke714x",
  "dy992e",
  "vy694e",
  "by626a",
  "fe619w",
  "se980x",
  "dy992e",
  "vu223a",
  "ba275t",
  "vy657x",
  "bu656c",
  "tu461s",
  "ty845s",
  "ne808e",
  "na236z",
  "ve673e",
  "pe876j",
  "my653u",
  "ku294c",
  "vu173u",
  "da768j",
  "dy494c",
  "pe167m",
  "su430w",
  "na370p",
  "nu832s",
  "pa338k",
  "pu191k",
  "ry631p",
  "sa965y",
  "ve973z",
  "ju692r",
  "sa273b",
  "ry442a",
  "ju231d",
  "me571z",
  "fe345a",
  "jy493d",
  "me676w",
  "dy697h",
  "va726m",
  "ta769w",
  "ry476h",
  "mu174u",
  "ku588f",
  "ty316c",
  "ba661e",
  "ke682s",
  "ra229y",
  "nu202a",
  "fu375w",
  "my278b",
];
