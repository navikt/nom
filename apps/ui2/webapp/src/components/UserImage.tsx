import { css } from "@emotion/css";
import { Loader } from "@navikt/ds-react";
import React, { useEffect } from "react";

import { person, questionMarkIcon } from "./Images";

const imageSource = (navIdent: string) => `/teamcat/resource/${navIdent}/photo`;

type UserImageProperties = {
  navIdent?: string;
  navn?: string;
  size?: string;
  altText?: string;
};

const UserImage = (properties: UserImageProperties) => {
  const { navIdent, size, altText, navn } = properties;
  const [image, setImage] = React.useState<string>();
  const [loading, setLoading] = React.useState(true);

  useEffect(() => {
    setLoading(true);
    if (navIdent) {
      setImage(imageSource(navIdent));
    }
  }, [navIdent]);

  const stl = css({
    height: size || "65px",
    width: size || "65px",
    borderRadius: "100%",
  });

  if (!navIdent) {
    return (
      <div>
        <img className={stl} src={questionMarkIcon} />
      </div>
    );
  }

  return (
    <div>
      {loading && <Loader className={stl} />}
      <img
        alt={altText || "Fotografi av " + (navn || navIdent)}
        className={stl}
        hidden={loading}
        onError={() => {
          setImage(person);
          setLoading(false);
        }}
        onLoad={() => setLoading(false)}
        src={image}
      />
    </div>
  );
};

export default UserImage;
