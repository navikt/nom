import type { ReactNode } from "react";

import { building2Icon } from "../../../components/Images";
import { panelCss } from "../styles";
import { HorizontallyAlignedPanel } from "./HorizontallyAlignedPanel";

export const Enhet = (properties: {
  children: ReactNode;
  navIdentForImg: string;
  fornavnAndEtternavnForImg?: string;
}) => (
  <HorizontallyAlignedPanel
    className={panelCss.underEnhet}
    fornavnAndEtternavnForImg={properties.fornavnAndEtternavnForImg}
    iconSrc={building2Icon}
    navidentForImg={properties.navIdentForImg}
  >
    {properties.children}
  </HorizontallyAlignedPanel>
);
