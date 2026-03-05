import { css } from "@emotion/react";
import {
  ChevronDownIcon,
  ChevronUpIcon,
  InformationSquareFillIcon,
} from "@navikt/aksel-icons";
import { Heading, List } from "@navikt/ds-react";
import React, { useState } from "react";

import { SmartLink } from "~/components/navigation/SmartLink";
import { nomDateIsTodayOrAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";

const latestDisplayDate = "2026-03-31";

const displayBanner = nomDateIsTodayOrAfter(latestDisplayDate);

export const pilotBannerHeight = displayBanner ? "50px" : "0px";

export function PilotBanner() {
  const [isOpen, setIsOpen] = useState(false);

  if (!displayBanner) {
    return;
  }

  return (
    <div
      css={css`
        width: 100%;
        min-height: ${pilotBannerHeight};
        background-color: var(--nom-secondary-hover);
        border-bottom: 1px solid var(--a-lightblue-800);
        padding: var(--a-spacing-3) 0;
      `}
    >
      <div
        css={css`
          display: flex;
          justify-content: center;
          align-items: center;
          cursor: pointer;
        `}
        onClick={() => setIsOpen(!isOpen)}
      >
        <InformationSquareFillIcon
          css={css`
            margin-right: var(--a-spacing-2);
          `}
          fontSize="24px"
        />

        <Heading size="small">
          Feil i organisasjonsenhet, -struktur eller -leder?
        </Heading>

        <div
          css={css`
            display: flex;
            margin-left: var(--a-spacing-2);
          `}
        >
          {isOpen ? (
            <ChevronUpIcon fontSize="32px" />
          ) : (
            <ChevronDownIcon fontSize="32px" />
          )}
        </div>
      </div>

      {isOpen && (
        <List
          as="ul"
          css={css`
            width: 100%;
            max-width: 600px;
            margin: var(--a-spacing-3) auto 0;
          `}
        >
          <Heading
            css={css`
              margin-bottom: var(--a-spacing-2);
              margin-left: var(--a-spacing-2);
            `}
            level="2"
            size="small"
          >
            Mars 2026
          </Heading>

          <List.Item>
            NOM henter data om leder og organisasjonsenheter fra Unit4
            (lønnssystemet). Når det er feil i data om leder eller
            organisasjonsenheter må dette rettes i Unit4.{" "}
            <SmartLink
              href="https://navno.sharepoint.com/sites/intranett-lonnsadministrasjon/SitePages/L%C3%B8nnsskjema.aspx"
              inline
            >
              Dette gjøres ved å sende lønnsskjemaer i Unit4 til NAV
              Økonomitjeneste
            </SmartLink>
            . Hvis du har spørsmål til dette kan du kontakte NAV
            Økonomitjeneste.
          </List.Item>
        </List>
      )}
    </div>
  );
}
