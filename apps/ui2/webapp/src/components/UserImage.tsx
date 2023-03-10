import { css } from "@emotion/react";
import React, { useEffect } from "react";

import { person, questionMarkIcon } from "./Images";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
};

const UserImage = (properties: UserImageProperties) => {
  const { navident, size, navn } = properties;
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
        alt={"Fotografi av " + (navn || navident)}
        css={STYLE}
        key={navident}
        onError={() => {
          setImage(person);
        }}
        src={image || person}
      />
    </>
  );
};

export default UserImage;
