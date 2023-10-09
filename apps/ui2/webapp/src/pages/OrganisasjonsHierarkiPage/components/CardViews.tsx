import { css } from "@emotion/react";
import { ChevronRightIcon } from "@navikt/aksel-icons";
import { BodyShort, Heading } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "@tanstack/react-router";
import type { ReactNode } from "react";
import React, { useEffect, useRef } from "react";

import type {
  HentHierarkiQuery,
  OrgenhetStandaloneWithLederFragment,
} from "~/api/generated/graphql_generated";
import {
  Sektor,
  useHentHierarkiQuery,
} from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { orgRoute, ressursRoute } from "~/reactRouterRoutes";
import building from "~/resources/building.svg";

export function OrgEnhetView({
  orgEnhet,
  autoFocus = false,
}: {
  orgEnhet: OrgenhetStandaloneWithLederFragment;
  autoFocus?: boolean;
}) {
  // Optimistically fetch orgEnhet this view links to.
  useHentHierarkiQuery({
    variables: {
      id: orgEnhet.id,
    },
    fetchPolicy: "cache-first",
  });

  const reference = useRef<HTMLAnchorElement>(null);
  useEffect(() => {
    // Give autofocus on initial load.
    // This way the user can continue navigation from where they were before page change.
    if (autoFocus) {
      reference.current?.focus();
    }
  }, []);

  return (
    <ReactRouterLink
      css={css`
        text-decoration: none;
        color: var(--a-gray-900);
      `}
      params={{ id: orgEnhet.id }}
      ref={reference}
      to={orgRoute.to}
    >
      <UnitContainerLink>
        <VerticalEdge size="large">
          <img alt={"orgenhet"} src={building} />
        </VerticalEdge>
        <div
          css={css`
            margin: var(--a-spacing-3) 0;
          `}
        >
          <Heading level="2" size="small">
            {orgEnhet.navn}
          </Heading>
          <BodyShort size="small">
            Leder:{" "}
            {orgEnhet.leder[0]?.ressurs.visningsnavn || "Leder ikke funnet"}
          </BodyShort>
        </div>
      </UnitContainerLink>
    </ReactRouterLink>
  );
}

type HierarkiQueryRessurs = NonNullable<
  NonNullable<NonNullable<HentHierarkiQuery>["ressursKoblinger"]>["koblinger"]
>[0]["ressurs"];

export function ResourceLink({ ressurs }: { ressurs: HierarkiQueryRessurs }) {
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
      to={ressursRoute.to}
    >
      <UnitContainerLink>
        <UserImage
          navident={ressurs.navident}
          navn={ressurs.visningsnavn ?? ""}
          size="55px"
        />
        <div>
          <Heading level="2" size="small">
            {ressurs.visningsnavn ?? "uten navn"}
          </Heading>
          <span>
            {ressurs.sektor.includes(Sektor.Ekstern) ? "(Ekstern)" : ""}
          </span>
        </div>
      </UnitContainerLink>
    </ReactRouterLink>
  );
}

function UnitContainerLink({ children }: { children: ReactNode }) {
  return (
    <div
      css={css`
        width: 100%;
        height: 100%;
        border-radius: 5px;
        border: 1px solid var(--a-gray-400);
        background-color: var(--a-white);
        display: grid;
        grid-template-columns: min-content 1fr 40px;
        align-items: center;
        gap: 1rem;
        cursor: pointer;

        &:hover {
          background: var(--nom-secondary-hover);
        }
      `}
    >
      {children}
      <ChevronRightIcon fontSize="2rem" />
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

export function HorizontalEdge() {
  return (
    <div
      css={css`
        background-color: var(--a-lightblue-800);
        height: var(--a-spacing-2);
        width: 100%;
        border-radius: 4px 4px 0 0;
      `}
    />
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
