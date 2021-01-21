import React from "react";
import { useParams } from "react-router";
import useFetchRessurs from "../../api/useFetchRessurs";
import NomUiHeader from "../../components/header/NomUiHeader";

export default function Medarbeidere(props: any) {
  const { id } = useParams<any>();
  const [res, refresh] = useFetchRessurs(id);

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
    <div className="Medarbeidere">
      <NomUiHeader />
      <p>
        Bruk ID i adresselinjen for å se på forskjellige medarbeidere (f.eks
        /medarbeidere/X123456)
      </p>

      <pre className="Graphql__jsondump">{out}</pre>
    </div>
  );
}
