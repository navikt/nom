import { css } from "@emotion/react";
import {
  ChevronDownIcon,
  ChevronUpIcon,
  InformationSquareFillIcon,
} from "@navikt/aksel-icons";
import { Heading, List } from "@navikt/ds-react";
import { useState } from "react";

export const pilotBannerHeight = "50px";

const outerDivStyles = css`
  width: 100%;
  min-height: ${pilotBannerHeight};
  background-color: var(--nom-secondary-hover);
  border-bottom: 1px solid var(--a-lightblue-800);
  border-top: 1px solid var(--a-lightblue-800);
`;

const bannerStyles = css`
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: var(--a-spacing-2);
`;

const iconStyles = css`
  height: 25px;
  width: 25px;
  margin-right: var(--a-spacing-2);
`;

const textDivStyles = css`
  width: 30%;
  padding-bottom: 0.7rem;
  padding-top: 10px;
  margin: 0 auto 10px;
`;

const buttonDivStyles = css`
  display: flex;
  align-items: center;
  margin-left: var(--a-spacing-2);
`;

const PilotBanner = () => {
  const [isOpen, setIsOpen] = useState(false);

  return (
    <div css={outerDivStyles}>
      <div css={bannerStyles} onClick={() => setIsOpen(!isOpen)}>
        <InformationSquareFillIcon css={iconStyles} />
        <Heading size="small">Nytt i NOM</Heading>
        <div css={buttonDivStyles}>
          {isOpen && <ChevronUpIcon fontSize="32px" />}
          {!isOpen && <ChevronDownIcon fontSize="32px" />}
        </div>
      </div>

      {isOpen && (
        <List as="ul" css={textDivStyles} title="Desember 2024">
          <List.Item>
            Pilotenheter administrerer statlige medarbeidere i NOM.
          </List.Item>
          <List.Item>Støtte for sektorbytte.</List.Item>
          <List.Item>
            Støtte for å masseflytte medarbeidere fra én orgenhet til en annen.
          </List.Item>
          <List.Item>
            Mulighet for å endre kontaktinfo for kontaktpersoner (pårørende).
          </List.Item>
          <List.Item>
            Administrering av alle statlige medarbeidere i NOM kommer i januar.
          </List.Item>
        </List>
      )}
    </div>
  );
};
export default PilotBanner;
