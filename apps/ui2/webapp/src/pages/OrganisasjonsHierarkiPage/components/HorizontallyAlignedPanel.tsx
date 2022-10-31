import { css } from "@emotion/css";
import { Next } from "@navikt/ds-icons";
import type { ReactNode } from "react";

import UserImage from "../../../components/UserImage";
import { panelCss } from "../styles";

const nextChevronCss = css`
  margin: 0 1rem 0 0.2rem;
  height: 2rem;
  width: 2rem;
`;

export const HorizontallyAlignedPanel = (properties: {
  children: ReactNode;
  iconSrc: string;
  className: string;
  navidentForImg?: string;
  fornavnAndEtternavnForImg?: string;
}) => (
  <div className={properties.className}>
    <div className={panelCss.panelRim}>
      {properties.iconSrc && (
        <img alt={"src=" + properties.iconSrc} src={properties.iconSrc} />
      )}
    </div>
    <div className={panelCss.panelContent}>{properties.children}</div>
    {properties.navidentForImg && (
      <UserImage
        navIdent={properties.navidentForImg}
        navn={properties.fornavnAndEtternavnForImg}
      />
    )}
    <Next className={nextChevronCss} height={undefined} width={undefined} />
  </div>
);
