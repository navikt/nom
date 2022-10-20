import { css } from "@emotion/css";
import type { ReactNode } from "react";

import { titleStyles } from "./Styles";

const cardStyles = css({
  background: "var(--navds-global-color-white)",
  width: "100%",
  padding: "20px 35px",
  border: "1px solid var(--navds-global-color-gray-600)",
  borderRadius: "5px",
  marginTop: "25px",
});

type CardProperties = {
  title?: string;
  children: ReactNode;
};

const Card = (properties: CardProperties) => {
  const { title, children } = properties;
  return (
    <div className={cardStyles}>
      {title && <span className={titleStyles}>{title}</span>}
      {children}
    </div>
  );
};

export default Card;
