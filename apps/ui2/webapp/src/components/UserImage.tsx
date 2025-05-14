import { css } from "@emotion/react";
import { Link, Tooltip } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import type { MutableRefObject } from "react";
import { useState } from "react";
import React from "react";

import { useUnleashToggle } from "~/hooks/useUnleashToggle";
import person from "~/resources/person.svg";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
  includeInstructionsWhenNotFound?: boolean;
  loadOnlyWhenVisible?: boolean; // default true => (wait until viewport includes img before fetching data), false => fetch img data on load
};

export function UserImage(properties: UserImageProperties) {
  const nomAzurePictures = useUnleashToggle("nom.ui.bilder.bruknomazure");
  return !nomAzurePictures ? (
    <UserImageOld {...properties} />
  ) : (
    <UserImageNew {...properties} />
  );
}

function UserImageOld(properties: UserImageProperties) {
  const { navident, size, navn, includeInstructionsWhenNotFound } = properties;
  const loadOnlyWhenVisible = properties.loadOnlyWhenVisible ?? true;
  const [shouldLoadImg, setShouldLoadImg] =
    React.useState(!loadOnlyWhenVisible);
  const reference = React.useRef<HTMLAnchorElement | HTMLImageElement | null>(
    null,
  );

  const enableQry =
    !!navident &&
    shouldLoadImg &&
    globalThis.location.href.includes("nom.nav.no");

  const imageQuery = useQuery({
    queryKey: getPhotoQuery.queryKey(navident as string),
    queryFn: () => getPhotoQuery.queryFn(navident as string),
    enabled: enableQry,
    staleTime: getPhotoQuery.staleTime,
    gcTime: getPhotoQuery.cacheTime,
    retry: false,
  });

  const STYLE = css`
    height: ${size || "65px"};
    width: ${size || "65px"};
    border-radius: 100%;
  `;

  React.useLayoutEffect(() => {
    const observer = new IntersectionObserver(
      (intersectionEntry) => {
        if (intersectionEntry[0]?.isIntersecting && !shouldLoadImg) {
          setShouldLoadImg(true);
        }
      },
      {
        root: undefined,
        rootMargin: "0%",
        threshold: 0.5,
      },
    );
    if (reference.current) {
      observer.observe(reference.current);
    }
    return () => {
      if (reference.current) {
        observer.unobserve(reference.current);
        observer.disconnect();
      }
    };
  }, []);

  if (imageQuery.isSuccess) {
    return (
      <img
        alt={"Fotografi av " + (navn || navident)}
        css={STYLE}
        ref={reference as MutableRefObject<HTMLImageElement>}
        src={imageQuery.data}
      />
    );
  }

  if (includeInstructionsWhenNotFound) {
    return (
      <Link
        href="https://navno.sharepoint.com/sites/intranett-it/SitePages/oppdatere%20profilen%20din%20i%20medarbeiders%C3%B8ket.aspx"
        ref={reference as MutableRefObject<HTMLAnchorElement>}
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
      alt=""
      css={STYLE}
      ref={reference as MutableRefObject<HTMLImageElement>}
      src={person}
    />
  );
}

export const getPhotoQuery = {
  queryKey: (navident: string) => ["PHOTO", navident],
  queryFn: fetchPhoto,
  staleTime: Number.POSITIVE_INFINITY,
  cacheTime: Number.POSITIVE_INFINITY,
};

async function fetchPhoto(navident: string) {
  const response = await fetch(
    new Request(`/frackend/teamcat/resource/${navident}/photo`),
  );
  if (response.status !== 200) {
    throw new Error("Failed to load image");
  }
  const blob = await response.blob();
  return URL.createObjectURL(blob);
}

function UserImageNew(props: UserImageProperties) {
  const { navident, size, navn, includeInstructionsWhenNotFound } = props;
  const loadOnlyWhenVisible = props.loadOnlyWhenVisible ?? true;

  const enableImgFetch =
    !!navident && globalThis.location.href.includes("nom.nav.no");

  const [imgSrc, setImgSrc] = useState(
    enableImgFetch ? `/frackend/nom-azure/picture/${props.navident}` : person,
  );
  const [imgError, setImgError] = useState(false);

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
      alt={"Fotografi av " + (navn || navident)}
      css={STYLE}
      loading={loadOnlyWhenVisible ? "lazy" : "eager"}
      onError={() => {
        setImgSrc(person);
        setImgError(true);
      }}
      src={imgSrc}
    />
  );
}
