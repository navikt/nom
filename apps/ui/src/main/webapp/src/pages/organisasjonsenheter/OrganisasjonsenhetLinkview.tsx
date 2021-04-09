import React from "react";
import { Link } from "react-router-dom";
import TableSorter from "tabell";
import { LookupState } from "../../api/utils_api_hook";
import { OrgEnhetFelter, Ressurs, OrgEnhetData } from "../../common/types";
import AlertStripe from "nav-frontend-alertstriper";
import { createRessursQuery } from "../../api/graphql_queries";

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
      const out: Record<string, any> = {
        id: it.ressurs.navIdent,
        pnr: it.ressurs.personIdent.substr(0, 5) + "....",
      };
      const navnOk = it.ressurs.person && it.ressurs.person.navn ? true : false;
      if (navnOk) {
        out.fornavn = it.ressurs.person.navn.fornavn;
        out.mellomnavn = it.ressurs.person.navn.mellomnavn;
        out.etternavn = it.ressurs.person.navn.etternavn;
      }
      return out;
    }
  );
  console.log(graphQlData);

  const lederData = graphQlData.organisasjonsenhet[0].leder[0];

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
    leder: {
      id: lederData ? lederData.ressurs.navIdent : "ukjent",
      pnr: lederData ? lederData.ressurs.personIdent : "ukjent",
      person: lederData ? lederData.ressurs.person : undefined,
    },
  };
}

function LeaderBox(props: { ressurs: Ressurs }): JSX.Element {
  props.ressurs.person= {navn: {fornavn: "Alfred", mellomnavn: "Ugla", etternavn: "Hansen"}};
  return (
    <div>
      <span>- Leder: </span>
      <span>
        <Link to={"/medarbeidere/" + props.ressurs.id}>{props.ressurs.id}</Link>
        ,{" "}
      </span>
      <span>
        {props.ressurs.person ? (
          <span>
            <span>{props.ressurs.person?.navn.fornavn} </span>
            {props.ressurs.person?.navn.mellomnavn}{" "}
            {props.ressurs.person?.navn.etternavn}{" "}
          </span>
        ) : (
          <span>...</span>
        )}
      </span>
    </div>
  );
}

function LocalTable(props: { orgenhetData }): JSX.Element {
  const orgenhetOver: OrgEnhetFelter | null = props.orgenhetData.koblinger
    .sti[0] as any;
  const orgenheterUnder: OrgEnhetFelter[] = props.orgenhetData.koblinger
    .underEnheter as any;
  const ressurserUnder: Ressurs[] = props.orgenhetData.koblinger.ressurser;

  const leder: Ressurs = props.orgenhetData.leder;

  const [selectedView, setSelectedView] = React.useState<
    "underenheter" | "ressurser"
  >("underenheter");

  const ressurserTable = (
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
        { id: "fornavn", label: "Fornavn", type: "string" },
        { id: "mellomnavn", label: "Mellomnavn", type: "string" },
        { id: "etternavn", label: "Etternavn", type: "string" },
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
  );

  const enheterTable = (
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
              <Link to={"/organisasjonsenheter/" + it.enhetNr}>{value}</Link>
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
  );

  return (
    <div className="Organisasjonsenheter__linkview">
      <div className="Organisasjonsenheter__linkview__titleview">
        Enhet navn / nummer:
        <h3>
          {props.orgenhetData.data.enhetNavn +
            " / " +
            props.orgenhetData.data.enhetNr}
        </h3>
        <LeaderBox ressurs={leder}></LeaderBox>
      </div>


      {orgenhetOver ? (
        <div className="Organisasjonsenheter__linkview__overenhetlink">
          <h4>Overenhet:</h4>-{" "}
          <Link to={"/organisasjonsenheter/" + orgenhetOver.enhetNr}>
            {orgenhetOver.enhetNavn}
          </Link>
        </div>
      ) : null}

      <hr />
      <p>
        <span>
          Visning: {selectedView}{" "}
          <button
            onClick={() => setSelectedView("underenheter")}
            disabled={selectedView === "underenheter"}
          >
            Vis underenheter ({orgenheterUnder.length})
          </button>
          <button
            onClick={() => setSelectedView("ressurser")}
            disabled={selectedView === "ressurser"}
          >
            Vis ressurser ({ressurserUnder.length})
          </button>
          <span></span>
        </span>
      </p>
      {selectedView === "ressurser" ? ressurserTable : null}
      {selectedView === "underenheter" ? enheterTable : null}
    </div>
  );
}

export default function OrganisasjonsenhetLinkview(props: {
  lookup: LookupState;
}): JSX.Element {
  switch (props.lookup.lookupState) {
    case "RESOLVED": {
      const orgenhetData = transformResult(props.lookup.result);
      return <LocalTable orgenhetData={orgenhetData}></LocalTable>;
    }
    case "RESOLVEDWITHERRORS": {
      const orgenhetData = transformResult(props.lookup.result);
      return (
        <div className="Organisasjonsenheter__linkview">
          <AlertStripe type="info">
            <p>Feil i graphql spørring, se console log</p>
          </AlertStripe>
          <LocalTable orgenhetData={orgenhetData}></LocalTable>
        </div>
      );
    }
    default:
      return <div>{props.lookup.lookupState}</div>;
  }
}
