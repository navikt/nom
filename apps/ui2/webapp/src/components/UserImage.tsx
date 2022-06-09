import { css } from "@emotion/css";
import { Loader } from "@navikt/ds-react";
import React, { useEffect } from "react";
import { mannMedFjes } from "./Images";

const imageSrc = (navIdent: string) => `https://nom-ui2.intern.nav.no/teamcat/resource/${navIdent}/photo`;

type UserImageProps = {
  navIdent?: string;
  size?: string;
};

const UserImage = (props: UserImageProps) => {
  const { navIdent, size } = props;
  const [image, setImage] = React.useState<string>();
  const [loading, setLoading] = React.useState(true);

  useEffect(() => {
    if (navIdent) {
      setImage(imageSrc(navIdent));
    }
    setLoading(false)
  }, []);

  return (
    <>
      {loading && <Loader />}
      {!loading && (
        <img
          className={css({
            height: size || "65px",
            width: size || "65px",
            borderRadius: "100%",
          })}
          src={image}
          onError={() => {
            setImage(mannMedFjes);
            setLoading(false);
          }}
          onLoad={() => setLoading(false)}
        />
      )}
    </>
  );
};

export default UserImage;
