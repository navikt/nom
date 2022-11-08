import { css } from "@emotion/css";
import { Back } from "@navikt/ds-icons";
import { Link } from "react-router-dom";

export const BackFromRessurs = (properties: {
  valgtEnhetUrl: string;
  ressursIsLeder?: boolean;
}) => (
  <Link
    className={css`
      display: grid;
      grid-template-columns: min-content auto;
      color: var(--navds-global-color-gray-900);
      margin-top: var(--navds-spacing-4);
    `}
    to={properties.valgtEnhetUrl}
  >
    <Back className="navds-link-panel__chevron" />
    <strong>
      {properties.ressursIsLeder
        ? "Se alle underenhetene for enheten "
        : "Se alle ressurser i enheten"}
    </strong>
  </Link>
);
