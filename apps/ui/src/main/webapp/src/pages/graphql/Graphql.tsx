import React from "react";
import { Link, useParams } from "react-router-dom";
import useGetOrganisasjonsEnhet from "../../api/useGetOrganisasjonsEnhet";
import NomUiHeader from "../../components/header/NomUiHeader";

export default function Graphql(): JSX.Element {
  const { id } = useParams<any>();

  const [res, refresh] = useGetOrganisasjonsEnhet(id);

  let out: any = <div></div>;
  
  switch (res.lookupState) {
    case "PENDING":
      out = "Pending";
      break;
    case "ERROR":
      out = res.error.message;
      break;
    case "RESOLVED":
      out = JSON.stringify(res.result, null, 1);
      break;
    case "WAITING":
      out = "Waiting";
  }
  return (
    <div className="Graphql">
      <NomUiHeader />
      <p>
        In progress. Eksempler på nummer man kan teste er:
        <Link to="/graphql/0106022">X</Link>,
        <Link to="/graphql/1571000">Y</Link>,
        <Link to="/graphql/0118000">Z</Link>
      </p>
      <pre className="Graphql__jsondump">{out}</pre>
    </div>
  );
}
