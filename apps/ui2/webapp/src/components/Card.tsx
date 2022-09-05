import { css } from "@emotion/css";
import { ReactNode } from "react";
import { titleStyles } from "./Styles";

const cardStyles = css({
  background: "#FFFFFF",
  width: "100%",
  padding: "20px 35px",
  border: "1px solid #707070",
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
