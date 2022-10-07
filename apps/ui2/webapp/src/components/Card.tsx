import { css } from "@emotion/css";
import { ReactNode } from "react";
import { titleStyles } from "./Styles";

const cardStyles = css({
  background: "var(--navds-global-color-white)",
  width: "100%",
  padding: "20px 35px",
  border: "1px solid var(--navds-global-color-gray-600)",
  borderRadius: "5px",
  marginTop: "25px",
});

type CardProps = {
  title?: string;
  children: ReactNode;
};

const Card = (props: CardProps) => {
  const { title, children } = props;
  return (
    <div className={cardStyles}>
      {title && <span className={titleStyles}>{title}</span>}
      {children}
    </div>
  );
};

export default Card;
