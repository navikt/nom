import { css } from "@emotion/react";
import React, { useEffect } from "react";

import { person, questionMarkIcon } from "./Images";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
  altText?: string;
};

const UserImage = (properties: UserImageProperties) => {
  const { navident, size, altText, navn } = properties;
  const [image, setImage] = React.useState<string>();

  useEffect(() => {
    if (navident) {
      setImage(`/teamcat/resource/${navident}/photo`);
    }
  }, [navident]);

  const STYLE = css`
    height: ${size || "65px"};
    width: ${size || "65px"};
    border-radius: 100%;
  `;

  if (!navident) {
    return <img alt="manglende navident" css={STYLE} src={questionMarkIcon} />;
  }

  return (
    <>
      <img
        alt={altText || "Fotografi av " + (navn || navident)}
        css={STYLE}
        onError={() => {
          setImage(person);
        }}
        src={image || person}
      />
    </>
  );
};

export default UserImage;
