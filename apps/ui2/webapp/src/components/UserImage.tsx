import { css } from "@emotion/react";
import axios from "axios";
import React from "react";
import { useQuery } from "react-query";

import { person, questionMarkIcon } from "./Images";

type UserImageProperties = {
  navident?: string;
  navn?: string;
  size?: string;
};

const UserImage = (properties: UserImageProperties) => {
  const { navident, size, navn } = properties;

  const a = useQuery({
    queryKey: ["PHOTO", navident],
    queryFn: () => axios.get(`/teamcat/resource/${navident}/photo`),
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

  if (!navident) {
    return <img alt="manglende navident" css={STYLE} src={questionMarkIcon} />;
  }

  return (
    <>
      {!a.isSuccess && <img alt="" css={STYLE} src={person} />}
      <img
        alt={"Fotografi av " + (navn || navident)}
        css={STYLE}
        hidden={!a.isSuccess}
        key={navident}
        src={a.data?.data}
      />
    </>
  );
};

export default UserImage;
