import React from "react";
import { NavLink } from "react-router-dom";

import ContentWrapper from "../components/ContentWrapper";

export function NotAuthorizedPage() {
  return (
    <ContentWrapper>
      <>
        403. Ingen tilgang til undersiden. Gå <NavLink to="/">hjem</NavLink>
      </>
    </ContentWrapper>
  );
}
