import { css } from "@emotion/react";
import {
  ChevronDownIcon,
  ChevronUpIcon,
  InformationSquareFillIcon,
} from "@navikt/aksel-icons";
import { Heading, List } from "@navikt/ds-react";
import React, { useState } from "react";

import { todayWithoutTimezone } from "~/dateUtils/dateUtils";
import { nomDateIsAfter } from "~/pages/ResourcePage/RessursComponents/dolUtils";

export const pilotBannerHeight = "50px";

export function PilotBanner() {
  const [isOpen, setIsOpen] = useState(false);

  const latestDisplayDate = "2025-09-30";

  if (nomDateIsAfter(todayWithoutTimezone(), latestDisplayDate)) {
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
        `}
        onClick={() => setIsOpen(!isOpen)}
      >
        <InformationSquareFillIcon
          css={css`
            margin-right: var(--a-spacing-2);
          `}
          fontSize="24px"
        />

        <Heading size="small">Nytt i NOM</Heading>

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
            Juni 2025
          </Heading>

          <List.Item>Administrering av alle statlige ansatte</List.Item>
          <List.Item>
            Opprettelse og administrering av ikke-standard identer som roboter
            og utenlandske konsulenter
          </List.Item>
          <List.Item>
            Tabelloversikt over medarbeideres kontaktpersoner
          </List.Item>
          <List.Item>
            Masseflytting av medarbeidere til ny organisasjonsenhet
          </List.Item>
          <List.Item>
            Masseflytting av medarbeidere til nytt fysisk brukersted
          </List.Item>
          <List.Item>
            Oppdatert organisasjonskart og innplassering etter
            omorganiseringsprosessen i direktoratet
          </List.Item>
        </List>
      )}
    </div>
  );
}
