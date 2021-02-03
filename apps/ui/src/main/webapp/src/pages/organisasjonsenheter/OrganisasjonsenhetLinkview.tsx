import React from "react";
import { Link } from "react-router-dom";
import TableSorter from "tabell";
import { LookupState } from "../../api/utils_api_hook";
import { OrgEnhetFelter, Ressurs, OrgEnhetData } from "../../common/types";

function transformResult(graphQlData: any): OrgEnhetData {
  const underEnheter = graphQlData.organisasjonsenhet[0].organiseringer.filter(
    (it) => it.retning === "under"
  );

  const underEnheter2: OrgEnhetFelter[] = underEnheter.map((it) => {
    return {
      enhetNavn: it.organisasjonsenhet.navn,

      enhetNr: it.organisasjonsenhet.agressoId,
      enhetType: "Type (TODO)",
    };
  });

  const overenhet = graphQlData.organisasjonsenhet[0].organiseringer.filter(
    (it) => it.retning === "over"
  )[0];

  const ressurser: Ressurs[] = graphQlData.organisasjonsenhet[0].koblinger.map(
    (it) => {
      return {
        id: it.ressurs.navIdent,
        pnr: it.ressurs.personIdent.substr(0, 5) + "....",
      };
    }
  );

  return {
    data: {
      enhetNavn: graphQlData.organisasjonsenhet[0].navn,
      enhetNr: graphQlData.organisasjonsenhet[0].agressoId,
      enhetType: "type (todo)",
    },
    koblinger: {
      sti: overenhet
        ? [
          {
            enhetNavn: overenhet.organisasjonsenhet.navn,
            enhetNr: overenhet.organisasjonsenhet.agressoId,
            enhetType: "Type (TODO)",
          },
        ]
        : [],
      underEnheter: underEnheter2,
      ressurser: ressurser,
    },
  };
}

export default function OrganisasjonsenhetLinkview(props: {
  lookup: LookupState;
}): JSX.Element {
  switch (props.lookup.lookupState) {
    case "RESOLVED": {
      const orgenhetData = transformResult(props.lookup.result);
      const orgenhetOver: OrgEnhetFelter | null = orgenhetData.koblinger
        .sti[0] as any;
      const orgenheterUnder: OrgEnhetFelter[] = orgenhetData.koblinger
        .underEnheter as any;
      const ressurserUnder: Ressurs[] = orgenhetData.koblinger.ressurser;

      return (
        <div className="Organisasjonsenheter__linkview">
          <div className="Organisasjonsenheter__linkview__titleview">
            Enhet navn / nummer:
            <h3>
              {orgenhetData.data.enhetNavn + " / " + orgenhetData.data.enhetNr}
            </h3>
          </div>

          {orgenhetOver ? (
            <div className="Organisasjonsenheter__linkview__overenhetlink">
              <p>Overenhet:</p>-{" "}
              <Link to={"/organisasjonsenheter/" + orgenhetOver.enhetNr}>
                {orgenhetOver.enhetNavn}
              </Link>
            </div>
          ) : null}

          <hr />
          <p>Underenheter:</p>

          <TableSorter
            items={orgenheterUnder.map((it) => {
              return { key: it.enhetNr, ...it };
            })}
            columns={[
              {
                id: "enhetNavn",
                label: "Navn",
                type: "string",
                renderCell: function (item, value): JSX.Element {
                  const it = (item as any) as OrgEnhetFelter;
                  return (
                    <Link to={"/organisasjonsenheter/" + it.enhetNr}>
                      {value}
                    </Link>
                  );
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
          />

          <hr />
          <p>Ressurser</p>

          <TableSorter
            items={ressurserUnder.map((it) => {
              return { key: it.id, ...it };
            })}
            columns={[
              {
                id: "id",
                label: "NavIdent",
                type: "string",
                renderCell: function (item, value): JSX.Element {
                  return <Link to={"/medarbeidere/" + item.id}>{item.id}</Link>;
                },
              },
              { id: "pnr", label: "Personnummer", type: "string" },
            ]}
            itemsPerPage={50}
            animatable={false}
            searchable={false}
            selectable={false}
            sortable={true}
            sort={{ column: "id", order: "ascending" }}
            striped={false}
            summary={false}
            compact={true}
          ></TableSorter>
        </div>
      );
    }
    default:
      return <div>{props.lookup.lookupState}</div>;
  }
}
