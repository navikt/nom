import { css } from "@emotion/react";
import { useQuery } from "@tanstack/react-query";
import React from "react";

import person from "~/resources/person.svg";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
};

export function UserImage(properties: UserImageProperties) {
  const { navident, size, navn } = properties;

  const imageQuery = useQuery({
    queryKey: getPhotoQuery.queryKey(navident as string),
    queryFn: () => getPhotoQuery.queryFn(navident as string),
    enabled: !!navident,
    staleTime: getPhotoQuery.staleTime,
    cacheTime: getPhotoQuery.cacheTime,
    retry: false,
  });

  const STYLE = css`
    height: ${size || "65px"};
    width: ${size || "65px"};
    border-radius: 100%;
  `;

  if (imageQuery.isSuccess) {
    return (
      <img
        alt={"Fotografi av " + (navn || navident)}
        css={STYLE}
        src={imageQuery.data}
      />
    );
  }

  return <img alt="" css={STYLE} src={person} />;
}

export const getPhotoQuery = {
  queryKey: (navident: string) => ["PHOTO", navident],
  queryFn: fetchPhoto,
  staleTime: Number.POSITIVE_INFINITY,
  cacheTime: Number.POSITIVE_INFINITY,
};

async function fetchPhoto(navident: string) {
  const response = await fetch(
    new Request(`/teamcat/resource/${navident}/photo`),
  );
  if (response.status !== 200) {
    throw new Error("Failed to load image");
  }
  const blob = await response.blob();
  return URL.createObjectURL(blob);
}
