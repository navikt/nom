import { css } from "@emotion/react";
import { Link, Tooltip } from "@navikt/ds-react";
import React, { useState } from "react";

import person from "~/resources/person.svg";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
  includeInstructionsWhenNotFound?: boolean;
  loadOnlyWhenVisible?: boolean; // default true => (wait until viewport includes img before fetching data), false => fetch img data on load
};

export function UserImage(props: UserImageProperties) {
  const { navident, size, navn, includeInstructionsWhenNotFound } = props;
  const loadOnlyWhenVisible = props.loadOnlyWhenVisible ?? true;

  const [imgError, setImgError] = useState(false);
  const [imgSrc, setImgSrc] = useState(person);

  React.useEffect(() => {
    setImgSrc(person);
    setImgError(false);
    const enableImgFetch =
      !!navident && globalThis.location.href.includes("nom.nav.no");

    const img = new Image();
    // img.loading = loadOnlyWhenVisible ? "lazy" : "eager";
    img.src = enableImgFetch
      ? `/frackend/nom-azure/picture/${props.navident}`
      : person;

    img
      .decode()
      .then(() => setImgSrc(img.src))
      .catch(() => {
        setImgError(true);
        setImgSrc(person);
      });
  }, [props.navident]);

  const STYLE = css`
    height: ${size || "65px"};
    width: ${size || "65px"};
    border-radius: 100%;
  `;

  if (includeInstructionsWhenNotFound && imgError) {
    return (
      <Link
        href="https://navno.sharepoint.com/sites/intranett-it/SitePages/oppdatere%20profilen%20din%20i%20medarbeiders%C3%B8ket.aspx"
        target="_blank"
      >
        <Tooltip content="Åpne instruks for å endre bilde">
          <img alt="" css={STYLE} src={person} />
        </Tooltip>
      </Link>
    );
  }

  return (
    <img
      alt={"Et fotografi av " + (navn || navident)}
      css={STYLE}
      loading={loadOnlyWhenVisible ? "lazy" : "eager"}
      src={imgSrc}
    />
  );
}
