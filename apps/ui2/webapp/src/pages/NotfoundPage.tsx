import React from "react";
import { NavLink } from "react-router-dom";

import ContentWrapper from "../components/ContentWrapper";

export function NotfoundPage() {
  return (
    <ContentWrapper>
      404. Undersiden fantes ikke. Gå <NavLink to="/">hjem</NavLink>
    </ContentWrapper>
  );
}
