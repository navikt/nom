import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";
import type { RefObject } from "react";
import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";

import type { Hierarchy } from "../../../api/transformations/hierarchy";
import UserImage from "../../../components/UserImage";
import { lederStyling, panelCss } from "../styles";

const OrgenhetLederLink = (properties: {
  navident?: string;
  name: string;
  orgenhetCtxUrl: string;
}) => {
  const reference = useRef<HTMLAnchorElement>(null);

  // Give autofocus on initial load.
  // This way the user can continue navigation from where they were before page change.
  useEffect(() => {
    reference.current?.focus();
  }, []);

  if (!properties.navident) {
    return <span>Ingen leder</span>;
  }

  return (
    <Link
      className={lederStyling.lederTekst}
      ref={reference}
      to={`${properties.orgenhetCtxUrl}/${properties.navident}`}
    >
      {properties.name}
    </Link>
  );
};

export const ValgtEnhet = ({
  valgtEnhet,
  scrollReference,
}: {
  valgtEnhet: Hierarchy.OrgEnhet;
  scrollReference: RefObject<HTMLDivElement>;
}) => (
  <div className={panelCss.currentEnhet} ref={scrollReference}>
    <div className={panelCss.panelRim} />
    <div className={panelCss.currentEnhetPanelContent}>
      <div
        className={css`
          display: grid;
          align-content: center;
        `}
      >
        <Heading size={"large"}>
          <BuildingSvg />
          {valgtEnhet.name}
        </Heading>
        <div className={lederStyling.lederDiv}>
          <span>
            Leder:{" "}
            <OrgenhetLederLink
              name={valgtEnhet.ledere[0]?.name}
              navident={valgtEnhet.ledere[0]?.navident}
              orgenhetCtxUrl={valgtEnhet.url}
            />
          </span>
        </div>
      </div>
      <UserImage
        navIdent={valgtEnhet.ledere[0]?.navident}
        navn={valgtEnhet.ledere[0]?.name}
        size={"130px"}
      />
    </div>
  </div>
);

const BuildingSvg = (properties: { className?: string }) => (
  <svg
    className={properties.className || ""}
    fill="none"
    height="24"
    viewBox="0 0 18 18"
    width="24"
    xmlns="http://www.w3.org/2000/svg"
  >
    <path
      clipRule="evenodd"
      d={`M15.6405 1.611C15.6405 1.611 15.6397 1.6095 15.639 1.6095L14.1397 
        0.11025C14.07 0.03975 13.9747 0 13.875 0H4.125C4.02525 0 3.93 0.03975 3.8595 0.11025L2.3595 1.6095C2.35875
        1.61025 2.35875 1.611 2.358 1.611C2.289 1.68225 2.25 1.7955 2.25 1.875V17.625C2.25 17.832 2.418 18 2.625
        18H7.5V14.625C7.5 14.418 7.668 14.25 7.875 14.25H10.125C10.332 14.25 10.5 14.418 10.5
        14.625V18H15.375C15.582 18 15.75 17.832 15.75 17.625V1.875C15.75 1.79625 15.7095 1.67925 15.6405 1.611ZM6
        12.75H3.75V10.5H6V12.75ZM6 9H3.75V6.75H6V9ZM6 5.25H3.75V3H6
        V5.25ZM10.5 12.75H7.5V10.5H10.5V12.75ZM10.5
        9H7.5V6.75H10.5V9ZM10.5 5.25H7.5V3H10.5V5.25ZM14.25 12.75H12V10.5H14.25V12.75ZM14.25 9H12V6.75H14.25V9ZM14.25
        5.25H12V3H14.25V5.25ZM3.53025 1.5L4.28025 0.75H13.7198L14.4698 1.5H3.53025Z`}
      fillRule="evenodd"
    />
  </svg>
);
