import React from "react";
import {Link} from "react-router-dom";
import TableSorter from "tabell";
import {LookupState} from "../../api/utils_api_hook";
import {OrgEnhetData, OrgEnhetFelter, Ressurs, RessursFlat,} from "../../common/types";
import AlertStripe from "nav-frontend-alertstriper";
import Contentbox from "../../components/contentbox/Contentbox";
import {GqlOrganisasjonsenhet, GqlRessurs} from "../../common/gqltypes";
import {ItemLink} from '../../components/navfrontend/ItemLink';

const nullText = '[null]';

function transformResult(graphQlData: any): OrgEnhetData | undefined {
  const organisasjonsenhet: GqlOrganisasjonsenhet =
    graphQlData.organisasjonsenhet;

  if (!organisasjonsenhet) {
    return undefined;
  }

  const underEnheterTmp = organisasjonsenhet.organiseringer.filter(
    (it) => it.retning === "under"
  );

  const underEnheter: OrgEnhetFelter[] = underEnheterTmp.map((it) => {
    return {
      enhetNavn: it.organisasjonsenhet.navn,
      enhetNr: it.organisasjonsenhet.agressoId,
      enhetType: it.organisasjonsenhet.type?.navn || nullText,
    };
  });

  const enhetHierarkiListe = (function hierarkiToList(orgenhet: any): any {
    const out: any[] = [];
    let currentOverenhetList = orgenhet.organiseringer.filter(
      (it) => it.retning === "over"
    );

    while (
      currentOverenhetList[0] &&
      currentOverenhetList[0].organisasjonsenhet &&
      currentOverenhetList[0].organisasjonsenhet.organiseringer
      ) {
      out.push({
        navn: currentOverenhetList[0].organisasjonsenhet.navn,
        agressoId: currentOverenhetList[0].organisasjonsenhet.agressoId,
      });
      currentOverenhetList = currentOverenhetList[0].organisasjonsenhet.organiseringer.filter(
        (it) => it.retning === "over"
      );
    }

    return out;
  })(organisasjonsenhet);

  const ressurser: Ressurs[] = organisasjonsenhet.koblinger.map((it) => {
    const gqlRes: GqlRessurs = it.ressurs;
    const out: Ressurs = {
      id: gqlRes.navIdent,
      pnr: gqlRes.personIdent.substr(0, 5) + "....",
    };
    out.person = {
      navn: {
        fornavn: gqlRes.person?.navn.fornavn ?? "[ukjent]",
        mellomnavn: gqlRes.person?.navn.mellomnavn ?? "",
        etternavn: gqlRes.person?.navn.etternavn ?? "[ukjent]",
      },
    };
    return out as Ressurs;
  });

  const lederData = organisasjonsenhet.leder[0];

  return {
    data: {
      enhetNavn: organisasjonsenhet.navn,
      enhetNr: organisasjonsenhet.agressoId,
      enhetType: organisasjonsenhet.type?.navn || nullText,
    },
    koblinger: {
      sti:
        enhetHierarkiListe.length !== 0
          ? enhetHierarkiListe.map((it) => {
            return {
              enhetNavn: it.navn,
              enhetNr: it.agressoId,
              enhetType: it.type?.navn || nullText,
            };
          })
          : [],
      underEnheter: underEnheter,
      ressurser: ressurser,
    },
    leder: {
      id: lederData ? lederData.ressurs.navIdent : "ukjent",
      pnr: lederData ? lederData.ressurs.personIdent : "ukjent",
      person: lederData ? lederData.ressurs.person : undefined,
    },
  };
}

function LeaderBox(props: {ressurs: Ressurs}): JSX.Element {
  return (
    <div>
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
      {", "}
      <span>
        <Link to={"/medarbeidere/" + props.ressurs.id}>
          ({props.ressurs.id})
        </Link>
        ,{" "}
      </span>
    </div>
  );
}

function LocalTable(props: {orgenhetData?: OrgEnhetData}): JSX.Element {
  if (!props.orgenhetData) {
    return <div>Fant ikke enhet</div>;
  }

  const sti = props.orgenhetData.koblinger.sti;
  const orgenheterUnder: OrgEnhetFelter[] = props.orgenhetData.koblinger
    .underEnheter as any;

  const ressurserUnderTmp: Ressurs[] = props.orgenhetData.koblinger.ressurser;
  const ressurserUnder: Ressurs[] = [];

  // må filtrere vekk duplikate identer fram til react key kan inneholde informasjon som kan skille radene fra hverandre
  const ressursIdMap = {};
  ressurserUnderTmp.forEach((it) => {
    if (!ressursIdMap[it.id]) {
      ressurserUnder.push(it);
      ressursIdMap[it.id] = true;
    } else {
      console.warn(
        `Ressurs ${it.id} har mer enn én forekomst i enhet med nr ${props.orgenhetData?.data.enhetNr}`
      );
    }
  });

  const leder: Ressurs = props.orgenhetData.leder;

  const lederVisning = (
    <Contentbox label="Leder">
      <LeaderBox ressurs={leder}/>
    </Contentbox>
  );

  const infoVisning = (
    <Contentbox label="Info">
      <div className="OrganisasjonsenhetLinkView__Infovisning">
        <label>Enhetsnavn: </label>
        <div>{props.orgenhetData.data.enhetNavn}</div>
        <label>Enhet agresso-nr</label>
        <div>{props.orgenhetData.data.enhetNr}</div>
        <label>Enhet type</label>
        <div>{props.orgenhetData.data.enhetType}</div>
      </div>
    </Contentbox>
  );

  const stiReversed = [...sti].reverse();

  const hierarkiVisning = (
    <Contentbox label="Hierarki over">
      {sti.length !== 0
        ? stiReversed.map((it, k) => (
          <div
            key={it.enhetNr}
            className="Organisasjonsenheter__linkview__overenhetlink"
          >
            <span>{" - ".repeat(k)}</span>
            <Link to={"/organisasjonsenheter/" + it.enhetNr}>
              {it.enhetNavn}
            </Link>{" "}
            [nr: {it.enhetNr}]
          </div>
        ))
        : null}
    </Contentbox>
  );

  const ressurserTable = (
    <Contentbox label={"Ressurser (" + ressurserUnder.length + ")"}>
      {0 !== ressurserUnder.length ? (
        <TableSorter
          items={ressurserUnder.map((it: Ressurs) => {
            const resFlat: RessursFlat = {
              id: it.id,
              pnr: it.pnr,
              fornavn: it.person?.navn.fornavn ?? "",
              mellomnavn: it.person?.navn.mellomnavn ?? "",
              etternavn: it.person?.navn.etternavn ?? "",
            };
            return {key: resFlat.id, ...resFlat};
          })}
          columns={[
            {
              id: "id",
              label: "NavIdent",
              type: "string",
              renderCell: (item, value) => <ItemLink url={"/medarbeidere/" + item.id} label={item.id}/>
            },
            {id: "pnr", label: "Personnummer", type: "string"},
            {id: "fornavn", label: "Fornavn", type: "string"},
            {
              id: "mellomnavn",
              label: "Mellomnavn",
              type: "string",
            },
            {id: "etternavn", label: "Etternavn", type: "string"},
          ]}
          itemsPerPage={50}
          animatable={false}
          searchable={false}
          selectable={false}
          sortable={true}
          sort={{column: "id", order: "asc"}}
          striped={false}
          summary={false}
          compact={true}
        />
      ) : null}
    </Contentbox>
  );

  const enheterTable = (
    <Contentbox label={"Underenheter (" + orgenheterUnder.length + ")"}>
      {orgenheterUnder.length !== 0 ? (
        <TableSorter
          items={orgenheterUnder.map((it) => {
            return {key: it.enhetNr, ...it};
          })}
          columns={[
            {
              id: "enhetNavn",
              label: "Navn",
              type: "string",
              renderCell: (item, value) => <ItemLink url={"/organisasjonsenheter/" + item.enhetNr} label={value}/>
            },
            {id: "enhetNr", label: "Nr", type: "string"},
            {id: "enhetType", label: "Type", type: "string"},
          ]}
          itemsPerPage={50}
          animatable={false}
          searchable={false}
          selectable={false}
          sortable={true}
          sort={{column: "enhetNavn", order: "asc"}}
          striped={false}
          summary={false}
          compact={true}
        />
      ) : null}
    </Contentbox>
  );

  return (
    <div className="Organisasjonsenheter__linkview">
      <div className="Organisasjonsenheter__linkview__titleview">
        <i>Enhet navn / nummer:</i>
        <h1>
          {props.orgenhetData.data.enhetNavn}
          {" / "}
          {props.orgenhetData.data.enhetNr}
        </h1>
      </div>
      {hierarkiVisning}
      {infoVisning}
      {lederVisning}
      {enheterTable}
      {ressurserTable}
    </div>
  );
}

export default function OrganisasjonsenhetLinkview(props: {
  lookup: LookupState;
}): JSX.Element {
  switch (props.lookup.lookupState) {
    case "RESOLVED": {
      const orgenhetData = transformResult(props.lookup.result);
      return <LocalTable orgenhetData={orgenhetData}/>;
    }
    case "RESOLVEDWITHERRORS": {
      const orgenhetData = transformResult(props.lookup.result);
      return (
        <div className="Organisasjonsenheter__linkview">
          <AlertStripe type="info">
            <p>Feil i graphql spørring, se console log</p>
          </AlertStripe>
          <LocalTable orgenhetData={orgenhetData}/>
        </div>
      );
    }
    default:
      return <div>{props.lookup.lookupState}</div>;
  }
}
