import { css } from "@emotion/css";
import { Back } from "@navikt/ds-icons";
import { BodyShort } from "@navikt/ds-react";
import { Link } from "react-router-dom";

import { colorPalette } from "../styles";

export const BackFromRessurs = (properties: {
  valgtEnhetUrl: string;
  ressursIsLeder?: boolean;
}) => (
  <Link
    className={css`
      display: grid;
      grid-template-columns: min-content auto;
    `}
    to={properties.valgtEnhetUrl}
  >
    <Back className="navds-link-panel__chevron" color={colorPalette.font} />
    <BodyShort
      className={css({ fontWeight: "600", color: colorPalette.font })}
      size="medium"
    >
      {properties.ressursIsLeder
        ? "Se alle underenhetene for enheten "
        : "Se alle ressurser i enheten"}
    </BodyShort>
  </Link>
);
