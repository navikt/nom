import { css } from "@emotion/react";
import { Back } from "@navikt/ds-icons";
import { Link } from "@navikt/ds-react";
import { Link as ReactRouterLink } from "react-router-dom";

export const BackFromRessurs = (properties: {
  valgtEnhetUrl: string;
  ressursIsLeder?: boolean;
}) => (
  <Link
    as={ReactRouterLink}
    css={css`
      display: grid;
      grid-template-columns: min-content auto;
      margin-top: var(--a-spacing-4);
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
