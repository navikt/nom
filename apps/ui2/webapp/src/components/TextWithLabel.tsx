import { css } from "@emotion/css";

import { boldText, regularText } from "./Styles";

const labelStyles = css({
  regularText,
  marginBottom: "5px",
});

type TextWithLabelProperties = {
  label: string;
  text: string;
};

const TextWithLabel = (properties: TextWithLabelProperties) => (
  <div>
    <div className={labelStyles}>{properties.label}</div>
    <div className={boldText}>{properties.text}</div>
  </div>
);

export default TextWithLabel;
