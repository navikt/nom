import React from "react";
import { useParams } from "react-router-dom";
import EnhetOversikt from "../../components/enhetsoversikt/EnhetOversikt";
import NomUiHeader from "../../components/header/NomUiHeader";

export default function Oversikt(): JSX.Element{
  const { id } = useParams<any>();
  return (
    <div className="Oversikt">
      <NomUiHeader />
      <EnhetOversikt id={id}/>
    </div>
  );
}
