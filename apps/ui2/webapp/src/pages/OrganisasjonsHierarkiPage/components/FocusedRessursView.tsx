import { css } from "@emotion/react";
import { Heading } from "@navikt/ds-react";
import uniqBy from "lodash/uniqBy";
import type { RefObject } from "react";
import { Link } from "react-router-dom";

import type { Ressurs } from "~/api/generated/graphql_generated";
import { formatTlf } from "~/utils";

import UserImage from "../../../components/UserImage";
import { SektorNameMapping } from "../../CreateResourcePage/components/CreateResourceForm";

export const FocusedRessursView = ({
  focusedRessurs,
  scrollReference,
}: {
  focusedRessurs: Ressurs;
  scrollReference?: RefObject<HTMLDivElement>;
}) => {
  return (
    <div ref={scrollReference}>
      <div
        css={css`
          height: var(--a-spacing-2);
          border-radius: 4px 4px 0 0;
        `}
      />
      <div
        css={css`
          justify-content: space-between;
          display: flex;
          background: var(--nom-secondary-hover);
          border: 1px solid var(--a-gray-500);
          border-radius: 0 0 4px 4px;
          padding: 40px;
        `}
      >
        <div>
          <Heading
            css={css`
              margin-bottom: var(--a-spacing-3);
            `}
            level={"2"}
            size="large"
          >
            {focusedRessurs.visningsnavn ?? "Uten navn"}
          </Heading>
          {focusedRessurs.sektor[0] ? (
            <strong>
              {SektorNameMapping[focusedRessurs.sektor[0]]} ansatt (
              {focusedRessurs.navident})
            </strong>
          ) : undefined}
          <div
            css={css`
              display: grid;
              grid-auto-flow: dense;
              grid-template-columns: 1fr 1fr;
              margin-top: var(--a-spacing-8);
              gap: var(--a-spacing-4);

              @media (max-width: 80rem) {
                grid-template-columns: auto;
              }
            `}
          >
            <span>
              E-post: <strong>{focusedRessurs.epost || "Mangler epost"}</strong>
            </span>
            <span>
              Tlf:{" "}
              <strong>
                {focusedRessurs.telefon
                  .map((telefon) => formatTlf(telefon.nummer))
                  .join(", ") || "Mangler tlf"}
              </strong>
            </span>

            {uniqBy(
              focusedRessurs.koblinger,
              (kobling) => kobling.organisasjonsenhet.agressoId
            ).map((orgTilknytning) => {
              const { agressoId, navn, orgNiv } =
                orgTilknytning.organisasjonsenhet;
              return (
                <span
                  css={css`
                    grid-column-start: 1;
                  `}
                  key={agressoId}
                >
                  Org.enhet:{" "}
                  <Link to={`/org/${agressoId}/${orgNiv}`}>
                    <strong>{navn}</strong>
                  </Link>
                </span>
              );
            })}

            {focusedRessurs.ledere.map((leder) => {
              const { navident, visningsnavn, orgTilknytning } = leder.ressurs;
              const { agressoId, orgNiv } = orgTilknytning[0]?.orgEnhet ?? {};
              return (
                <span
                  css={css`
                    grid-column-start: 2;
                  `}
                  key={navident}
                >
                  Leder:{" "}
                  <Link to={`/org/${agressoId}/${orgNiv}/${navident}`}>
                    <strong>{visningsnavn ?? "Uten navn"}</strong>
                  </Link>
                </span>
              );
            })}
          </div>
        </div>

        <UserImage
          navident={focusedRessurs.navident}
          navn={focusedRessurs.visningsnavn ?? ""}
          size="126px"
        />
      </div>
    </div>
  );
};
