import { css } from "@emotion/css";
import { Loader } from "@navikt/ds-react";
import React, { useEffect } from "react";
import { person } from "./Images";

const imageSrc = (navIdent: string) => `/teamcat/resource/${navIdent}/photo`;

type UserImageProps = {
  navIdent?: string;
  size?: string;
  altText?: string
};

const UserImage = (props: UserImageProps) => {
  const { navIdent, size, altText } = props;
  const [image, setImage] = React.useState<string>()
  const [loading, setLoading] = React.useState(true)

  useEffect(() => {
    setLoading(true)
    if (navIdent) {
      setImage(imageSrc(navIdent));
    }
    // setLoading(false)
  }, [navIdent]);

  return (
    <>
      {loading && <Loader />}
      {!loading && (
        <img
          src={image}
          alt={props.altText || 'Fotografi av ' + navIdent}
          onLoad={() => setLoading(false)}
          onError={() => {
            setImage(person);
            setLoading(false);
          }}
          className={css({
            height: size || "65px",
            width: size || "65px",
            borderRadius: "100%",
          })}
        />
      )}
    </>
  );
};

export default UserImage;
