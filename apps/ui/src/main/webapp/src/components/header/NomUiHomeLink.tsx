import React from "react";
import { Link } from "react-router-dom";

export default function NomUiHomeLink(props: any): JSX.Element {
  return (
    <Link className="NomUiHomeLink" to="/">
      <img
        className="NomUiHomeLink__img"
        src="/assets/nav-logo.png"
        alt="NAV logo, lenke til forsiden"
      />
      <div className="NomUiHomeLink__text">NAV Organisasjonsmaster </div>
    </Link>
  );
}
