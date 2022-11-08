import { css } from "@emotion/css";
import { Loader } from "@navikt/ds-react";
import React, { useEffect } from "react";

import { person, questionMarkIcon } from "./Images";

const imageSource = (navident: string) => `/teamcat/resource/${navident}/photo`;

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
  altText?: string;
};

const UserImage = (properties: UserImageProperties) => {
  const { navident, size, altText, navn } = properties;
  const [image, setImage] = React.useState<string>();
  const [loading, setLoading] = React.useState(true);

  useEffect(() => {
    setLoading(true);
    if (navident) {
      setImage(imageSource(navident));
    }
  }, [navident]);

  const stl = css({
    height: size || "65px",
    width: size || "65px",
    borderRadius: "100%",
  });

  if (!navident) {
    return (
      <img alt="manglende nav ident" className={stl} src={questionMarkIcon} />
    );
  }

  return (
    <>
      {loading && <Loader className={stl} />}
      <img
        alt={altText || "Fotografi av " + (navn || navident)}
        className={stl}
        hidden={loading}
        onError={() => {
          setImage(person);
          setLoading(false);
        }}
        onLoad={() => setLoading(false)}
        src={image}
      />
    </>
  );
};

export default UserImage;
