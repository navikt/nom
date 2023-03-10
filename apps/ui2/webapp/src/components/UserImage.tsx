import { css } from "@emotion/react";
import React, { useEffect, useState } from "react";

import { person, questionMarkIcon } from "./Images";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
};

const UserImage = (properties: UserImageProperties) => {
  const { navident, size, navn } = properties;
  const [image, setImage] = useState<string>();
  const [isLoaded, setIsLoaded] = useState(false);

  useEffect(() => {
    if (navident) {
      setIsLoaded(false);
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
      {!isLoaded && <img alt="" css={STYLE} src={person} />}
      <img
        alt={"Fotografi av " + (navn || navident)}
        css={STYLE}
        hidden={!isLoaded}
        key={navident}
        onLoad={() => setIsLoaded(true)}
        src={image}
      />
    </>
  );
};

export default UserImage;
