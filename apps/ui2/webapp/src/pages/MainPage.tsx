// import { css } from '@emotion/react';
import styled from "@emotion/styled";
import { ElementType } from "react";
import { css } from "@emotion/css";
import { Pagewrap } from "../components/Pagewrap";

const styleClassRed = css({
  backgroundColor: "red",
});

const Quote = styled("blockquote")((props) => ({
  fontSize: props.size,
})) as ElementType<any>;

const MainPage = () => {
  return (
      <Quote size={20}>
        Nom prosjekt
        <div className={styleClassRed}>Et prosjekt i gang.</div>
      </Quote>

  );
};

export default MainPage;
