import { css } from "@emotion/react";
import React from "react";
import { useQuery } from "react-query";

import { person } from "./Images";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
};

const UserImage = (properties: UserImageProperties) => {
  const { navident, size, navn } = properties;

  const imageQuery = useQuery({
    queryKey: ["PHOTO", navident],
    queryFn: async () => {
      const response = await fetch(
        new Request(`/teamcat/resource/${navident}/photo`)
      );
      if (response.status !== 200) {
        throw new Error("Failed to load image");
      }
      const blob = await response.blob();
      return URL.createObjectURL(blob);
    },
    enabled: !!navident,
    staleTime: Number.POSITIVE_INFINITY,
    cacheTime: Number.POSITIVE_INFINITY,
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
};

export default UserImage;
