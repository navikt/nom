import { css } from "@emotion/css";
import { Heading } from "@navikt/ds-react";
import React, { Fragment } from "react";
import { Link } from "react-router-dom";

import type { Hierarchy } from "../../../api/transformations/hierarchy";
import { colorPalette, hierarkiCss, lederStyling } from "../styles";
import { Enhet } from "./Enhet";

export const OverEnhet = ({ overEnhet }: { overEnhet: Hierarchy.OrgEnhet }) => (
  <Fragment>
    <Link className={hierarkiCss.link} to={overEnhet.url}>
      <Enhet
        fornavnAndEtternavnForImg={overEnhet.ledere[0].name}
        navIdentForImg={overEnhet.ledere[0].navident}
      >
        <Heading size={"medium"}>{overEnhet.name}</Heading>
        <div>
          Leder:{" "}
          <span className={lederStyling.lederTekst}>
            {overEnhet.ledere[0]?.name || "Leder ikke funnet"}
          </span>
        </div>
      </Enhet>
    </Link>
    <OverEnheterDivider />
  </Fragment>
);

function OverEnheterDivider() {
  return (
    <div
      className={css`
        align-self: center;
        height: var(--navds-spacing-4);
        background-color: ${colorPalette.rim};
        width: 2px;
      `}
    />
  );
}
