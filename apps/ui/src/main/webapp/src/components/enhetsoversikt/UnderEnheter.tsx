import React, { useState } from "react";
import { Link } from "react-router-dom";
import { OrgEnhetFelter } from "../../common/types";
import TableSorter from "tabell";
import { Knapp } from "nav-frontend-knapper";
import OpprettOrgenhet from "../forms/OpprettOrgenhet";

export interface UnderEnheterProps {
  underEnheter: OrgEnhetFelter[];
  refresh: () => void;
}

function Tabell(props: {underEnheter: OrgEnhetFelter[]}): JSX.Element{
  return  <TableSorter
  items={props.underEnheter.map((it) => {
    return { key: it.enhetNr, ...it };
  })}
  columns={[
    {
      id: "enhetNavn",
      label: "Navn",
      type: "string",
      renderCell: function (item, value): JSX.Element {
        const it = (item as any) as OrgEnhetFelter;
        return <Link to={"/oversikt/" + it.enhetNr}>{value}</Link>;
      },
    },
    { id: "enhetNr", label: "Nr", type: "string" },
    { id: "enhetType", label: "Type", type: "string" },
  ]}
  itemsPerPage={50}
  animatable={false}
  searchable={false}
  selectable={false}
  sortable={true}
  sort={{ column: "enhetNavn", order: "ascending" }}
  striped={false}
  summary={false}
  compact={true}
/>;
}


export default function UnderEnheter(props: UnderEnheterProps): JSX.Element {
  const [show, setShow] = useState(false);
  const knappTekst = !show ? "Opprett ny underenhet" : "tilbake";
  if (props.underEnheter.length === 0) {
    return (
      <div className="UnderEnheter">
        <Knapp onClick={() => setShow(!show)}>{knappTekst}</Knapp>
        {show ? (
          <OpprettOrgenhet parentId="01" />
        ) : (
          <p>Ingen tilknyttede enheter funnet</p>
        )}
      </div>
    );
  }
  return (
    <div className="UnderEnheter">
      <Knapp onClick={() => setShow(!show)}>{knappTekst}</Knapp>
      {show ? (
        <OpprettOrgenhet parentId="01" />
      ) : (
       <Tabell underEnheter={props.underEnheter} />
      )}
    </div>
  );
}
