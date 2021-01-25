import React from "react";
import { Link, useParams } from "react-router-dom";
import { useGetOrganisasjonsEnhet } from "../../api/graphql_fetch_hooks";
import NomUiHeader from "../../components/header/NomUiHeader";

export default function Organisasjonsenheter(props: any): JSX.Element {
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
    <div className="Organisasjonsenheter">
      <NomUiHeader />
      <p>
        Bruk ID i adresselinjen for å se på forskjellige enheter (f.eks
        /organisasjonsenheter/R02), eller gå til
        <Link to="/organisasjonsenheter/NAV"> toppen av hierarkiet</Link>,
      </p>
      <pre className="Graphql__jsondump">{out}</pre>
    </div>
  );
}
