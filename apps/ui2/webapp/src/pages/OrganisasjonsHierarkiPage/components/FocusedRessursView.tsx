import { css } from "@emotion/react";
import { Heading, HelpText, Label, Link, Tag } from "@navikt/ds-react";
import { format, isBefore } from "date-fns";
import uniqBy from "lodash/uniqBy";
import type { ReactNode, RefObject } from "react";
import React from "react";
import { Link as ReactRouterLink } from "react-router-dom";

import type { Ressurs, Telefon } from "~/api/generated/graphql_generated";
import { Sektor } from "~/api/generated/graphql_generated";
import { UserImage } from "~/components/UserImage";
import { HorizontalEdge } from "~/pages/OrganisasjonsHierarkiPage/components/CardViews";
import { DATE_FORMATS, formatTlf } from "~/utils";

import { SektorNameMapping } from "../../CreateResourcePage/components/CreateResourceForm";

function BaseView({
  focusedRessurs,
  scrollReference,
  children,
}: {
  children: ReactNode;
  focusedRessurs: Ressurs;
  scrollReference?: RefObject<HTMLDivElement>;
}) {
  return (
    <div
      css={css`
        display: grid;
        border-radius: 5px;
        margin-bottom: 1rem;
        background-color: var(--nom-secondary-hover);
        border: 1px solid var(--a-gray-400);
      `}
      ref={scrollReference}
    >
      <HorizontalEdge />
      <div
        css={css`
          justify-content: space-between;
          display: flex;
          padding: 40px;
        `}
      >
        <div>{children}</div>
        <UserImage
          navident={focusedRessurs.navident}
          navn={focusedRessurs.visningsnavn ?? ""}
          size="126px"
        />
      </div>
    </div>
  );
}
export const FocusedRessursView = ({
  focusedRessurs,
  scrollReference,
}: {
  focusedRessurs: Ressurs;
  scrollReference?: RefObject<HTMLDivElement>;
}) => {
  if (
    focusedRessurs.sluttdato &&
    isBefore(new Date(focusedRessurs.sluttdato), new Date())
  ) {
    return (
      <BaseView
        focusedRessurs={focusedRessurs}
        scrollReference={scrollReference}
      >
        <RessursHasQuit ressurs={focusedRessurs} />
      </BaseView>
    );
  }

  return (
    <BaseView focusedRessurs={focusedRessurs} scrollReference={scrollReference}>
      <Heading level="2" size="large" spacing>
        {focusedRessurs.visningsnavn ?? "Uten navn"}
      </Heading>
      {focusedRessurs.sektor[0] ? (
        <strong>
          {SektorNameMapping[focusedRessurs.sektor[0]]} (
          {focusedRessurs.navident})
        </strong>
      ) : undefined}
      <div
        css={css`
          display: grid;
          grid-auto-flow: dense;
          grid-template-columns: 1fr 1fr;
          align-items: center;
          margin-top: var(--a-spacing-8);
          gap: var(--a-spacing-4);

          @media (max-width: 80rem) {
            grid-template-columns: auto;
          }
        `}
      >
        <ShowEmailWithFallback epost={focusedRessurs.epost ?? ""} />
        {focusedRessurs.sektor.includes(Sektor.NavStatlig) ? (
          <ShowTlfWithFallback telefon={focusedRessurs.telefon} />
        ) : (
          <span />
        )}

        {uniqBy(
          focusedRessurs.koblinger,
          (kobling) => kobling.organisasjonsenhet.id
        ).map((orgTilknytning) => {
          const { navn, id } = orgTilknytning.organisasjonsenhet;
          return (
            <span
              css={css`
                grid-column-start: 1;
              `}
              key={id}
            >
              Org.enhet:{" "}
              <Link as={ReactRouterLink} to={`/org/${id}`}>
                <strong>{navn}</strong>
              </Link>
            </span>
          );
        })}

        {focusedRessurs.ledere.map((leder) => {
          const { navident, visningsnavn, orgTilknytning } = leder.ressurs;
          const { id } = orgTilknytning[0]?.orgEnhet ?? {};
          return (
            <span
              css={css`
                grid-column-start: 2;
              `}
              key={navident}
            >
              Leder:{" "}
              <Link as={ReactRouterLink} to={`/org/${id}/${navident}`}>
                <strong>{visningsnavn ?? "Uten navn"}</strong>
              </Link>
            </span>
          );
        })}
      </div>
    </BaseView>
  );
};

function RessursHasQuit({ ressurs }: { ressurs: Ressurs }) {
  return (
    <>
      <div
        css={css`
          display: inline-flex;
          align-items: center;
          gap: 1rem;
          margin-bottom: 1rem;
        `}
      >
        <Heading level="2" size="large">
          {ressurs.visningsnavn}
        </Heading>
        <Tag variant="warning">Sluttet</Tag>
      </div>
      <div
        css={css`
          display: grid;
          grid-template-columns: max-content 1fr;
          gap: var(--a-spacing-3);
        `}
      >
        <span>NAV-ident:</span>
        <Label as="span">{ressurs.navident}</Label>
        <span>Sektor:</span>
        <span>
          {ressurs.sektor.map((sektor) => SektorNameMapping[sektor]).join(", ")}
        </span>
        <span>Sluttdato:</span>
        <span>{format(new Date(ressurs.sluttdato), DATE_FORMATS.no)}</span>
      </div>
    </>
  );
}

function ShowEmailWithFallback({ epost }: { epost: string }) {
  return (
    <span>
      E-post:{" "}
      <b
        css={css`
          display: inline-flex;
          gap: var(--a-spacing-2);
        `}
      >
        {epost || (
          <>
            Kommer
            <HelpText>
              E-post vil vises her 1-2 dager etter at identansvarlig/leder har
              fullført skjermbildet for on-boarding av ressursen i Identrutina
              (Remedy).
            </HelpText>
          </>
        )}
      </b>
    </span>
  );
}

function ShowTlfWithFallback({ telefon }: { telefon: Telefon[] }) {
  const formattedTlfNumbers = telefon
    .map(({ nummer }) => formatTlf(nummer))
    .join(", ");

  return (
    <span>
      Tlf:{" "}
      <b
        css={css`
          display: inline-flex;
          gap: var(--a-spacing-2);
        `}
      >
        {formattedTlfNumbers || (
          <>
            Ikke tilgjengelig
            <HelpText>
              Telefonnummet hentes fra mobilabonnementet som er registrert på
              den ansatte hos Telenor. Ved registrering av abonnement tar det
              noen dager før det vises i NOM. <br /> <br />
              Dersom den ansatte ikke har abonnement gjennom Telenor vil
              telefonnummer ikke vises.
            </HelpText>
          </>
        )}
      </b>
    </span>
  );
}
