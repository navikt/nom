import { AlertStripeInfo } from "nav-frontend-alertstriper";
import React, { Fragment } from "react";
import { Link, useParams } from "react-router-dom";
import TableSorter from "tabell";
import { useFetchRessurs } from "../../api/graphql_fetch_hooks";
import { GqlRessurs } from "../../common/gqltypes";
import { fulltNavnForRessurs } from "../../common/navnutils";
import Contentbox from "../../components/contentbox/Contentbox";
import NomUiHeader from "../../components/header/NomUiHeader";

export interface Medarbeider {
  navIdent: string;
  fulltNavn: string;
  epost: string;
  leder?: {
    navIdent: string;
    fulltNavn: string;
  };
  ansattKategori: "Statlig" | "Kommunal" | "Ekstern" | "[ukjent]";
  enheter: {
    agressoId: string;
    enhetNavn: string;
    enhetType: "AgressoEnhet";
    ansattForhold: "Ansatt" | "Leder";
  }[];
  enheterLederFor: {
    agressoId: string;
    enhetNavn: string;
    enhetType: "AgressoEnhet";
    ansattForhold: "Ansatt" | "Leder";
  }[];
}

const templateMedarbeider: Medarbeider = {
  navIdent: "[ukjent]",
  fulltNavn: "[ukjent]",
  epost: "[ukjent]",
  leder: undefined,
  ansattKategori: "[ukjent]",
  enheter: [],
  enheterLederFor: [],
};

function translateGqlToMedarbeider(gqlResponse: any): Medarbeider | null {
  const out: Medarbeider = { ...templateMedarbeider };
  const ressurs: GqlRessurs = gqlResponse.result.ressurs;
  if (!ressurs) {
    return null;
  }

  out.navIdent = ressurs.navIdent;

  out.fulltNavn = fulltNavnForRessurs(ressurs);

  out.epost = ressurs.epost;

  if (ressurs?.ledere[0]) {
    const leder = ressurs.ledere[0].ressurs;
    out.leder = {
      navIdent: leder.navIdent,
      fulltNavn: fulltNavnForRessurs(leder),
    };
  }

  if (ressurs.koblinger.length !== 0) {
    const tmp: {
      agressoId: string;
      enhetNavn: string;
      enhetType: "AgressoEnhet";
      ansattForhold: "Ansatt" | "Leder";
    }[] = ressurs.koblinger.map((it) => {
      return {
        agressoId: it.organisasjonsenhet.agressoId,
        enhetNavn: it.organisasjonsenhet.navn,
        enhetType: "AgressoEnhet",
        ansattForhold: "Ansatt",
      };
    });
    out.enheter = tmp;
  }

  if (ressurs.lederFor && ressurs.lederFor.length !== 0) {
    const tmp: {
      agressoId: string;
      enhetNavn: string;
      enhetType: "AgressoEnhet";
      ansattForhold: "Ansatt" | "Leder";
    }[] = ressurs.lederFor.map((it) => {
      return {
        agressoId: it.organisasjonsenhet.agressoId,
        enhetNavn: it.organisasjonsenhet.navn,
        enhetType: "AgressoEnhet",
        ansattForhold: "Leder",
      };
    });
    out.enheterLederFor = tmp;
  }

  return out;
}

function Infopanel(props: { medarbeider: Medarbeider }): JSX.Element {
  return (
    <div className="MedarbeidereLinkview__Infopanel">
      <label htmlFor="navIdentHeader">Nav-ident: </label>
      <span id="navIdentHeader">{props.medarbeider.navIdent}</span>
      <label htmlFor="fulltNavnHeader">Navn: </label>
      <span id="fulltNavnHeader">{props.medarbeider.fulltNavn}</span>
      <label htmlFor="ansattTypeHeader">Kategori: </label>
      <span id="ansattTypeHeader">{props.medarbeider.ansattKategori}</span>
      <label htmlFor="ansattEpost">Epost: </label>
      <span id="ansattEpost">{props.medarbeider.epost}</span>

      {props.medarbeider.leder ? (
        <Fragment>
          <label htmlFor="linkTilLeder">Leder: </label>
          <span id="linkTilLeder">
            {props.medarbeider.leder.fulltNavn}{" "}
            <Link to={"/medarbeidere/" + props.medarbeider.leder.navIdent}>
              ({props.medarbeider.leder.navIdent})
            </Link>
          </span>
        </Fragment>
      ) : null}
    </div>
  );
}

function OrgenhetListview(props: {
  orgenheter: {
    agressoId: string;
    enhetNavn: string;
    enhetType: "AgressoEnhet";
  }[];
}): JSX.Element {
  return (
    <TableSorter
      items={props.orgenheter.map((it) => {
        return { key: it.agressoId, ...it };
      })}
      columns={[
        {
          id: "ansattForhold",
          label: "Ansattforhold",
          type: "string",
          renderCell: (item, value) => <b>{value}</b>,
        },
        {
          id: "id",
          label: "AgressoId",
          type: "string",
          renderCell: function (item, value): JSX.Element {
            return (
              <Link to={"/organisasjonsenheter/" + item.agressoId}>
                {item.agressoId}
              </Link>
            );
          },
        },
        { id: "enhetNavn", label: "Enhet navn", type: "string" },
        { id: "enhetType", label: "Enhet type", type: "string" },
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
}

function MedarbeiderRender(props: {
  medarbeider: Medarbeider | null;
}): JSX.Element {
  const medarbeider = props.medarbeider;

  if (!medarbeider) {
    return <div>Ugyldig navident</div>;
  }

  return (
    <div>
      {/* <div className="MedarbeidereLinkview"> */}
      <div>
        <h1>
          {medarbeider.fulltNavn} ({medarbeider.navIdent})
        </h1>

        <Contentbox label="Info">
          <Infopanel medarbeider={medarbeider}></Infopanel>
        </Contentbox>

        <Contentbox
          label={"Enhetstilhørighet (" + medarbeider.enheter.length + ")"}
        >
          {0 !== medarbeider.enheter.length ? (
            <OrgenhetListview orgenheter={medarbeider.enheter} />
          ) : null}
        </Contentbox>

        <Contentbox
          label={
            "Enhetslederansvar (" + medarbeider.enheterLederFor.length + ")"
          }
        >
          {0 !== medarbeider.enheterLederFor.length ? (
            <OrgenhetListview orgenheter={medarbeider.enheterLederFor} />
          ) : null}
        </Contentbox>
      </div>
    </div>
  );
}

export default function MedarbeiderePage(): JSX.Element {
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
      out = <MedarbeiderRender medarbeider={translateGqlToMedarbeider(res)} />;
      break;
    case "WAITING":
      out = "Venter på navident i adresselinjen";
      break;
    case "RESOLVEDWITHERRORS":
      out = (
        <Fragment>
          <AlertStripeInfo>Graphql feil. Se console log.</AlertStripeInfo>
          <MedarbeiderRender medarbeider={translateGqlToMedarbeider(res)} />
        </Fragment>
      );
      break;
    default:
      throw new Error("Should not happen");
  }
  return (
    <div>
      <NomUiHeader />
      <p>
        Bruk ID i adresselinjen for å se på forskjellige medarbeidere (f.eks
        /medarbeidere/X123456)
      </p>
      <div className="MedarbeidereLinkview">{out}</div>
    </div>
  );
}
