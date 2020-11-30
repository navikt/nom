import React from "react";
import { Link } from "react-router-dom";
import NomLogin from "./NomLogin";
import NomUiHomeLink from "./NomUiHomeLink";

export default function NomUiHeader(props: any): JSX.Element{
  return (
    <div className="NomUiHeader">
      <div className="NomUiHeader__banner">
        <NomUiHomeLink />
        <NomLogin />
      </div>
      <div className="NomUiHeader__pages">
        <Link to="/">Home</Link>
        <Link to="/app/About">About</Link>
        <Link to="/app/Oversikt">Oversikt</Link>
      </div>
    </div>
  );
}
