import { AlertStripeInfo } from "nav-frontend-alertstriper";
import React, { Fragment } from "react";
import {Link, useNavigate, useParams} from "react-router-dom";
import TableSorter from "tabell";
import { useFetchRessurs } from "../../api/graphql_fetch_hooks";
import { GqlRessurs } from "../../common/gqltypes";
import { fulltNavnForRessurs } from "../../common/navnutils";
import Contentbox from "../../components/contentbox/Contentbox";
import Nompagewrap from "../../components/nompagewrap/Nompagewrap";
import { ItemLink } from "../../components/navfrontend/ItemLink";
import LabeledItemsgrid from "../../components/labeleditemsgrid/Labeleditemsgrid";
import { agressoIdDataToUrl } from "../../common/url_utils";

export interface Medarbeider {
  navIdent: string;
  fulltNavn: string;
  epost: string;
  ressurstype: string[];
  telefon: {
    nummer: string;
    type: string;
    beskrivelse: string;
  }[];
  sluttdato: string;
  ledere: {
    navIdent: string;
    fulltNavn: string;
  }[];
  ansattKategori: "Statlig" | "Kommunal" | "Ekstern" | "[ukjent]";
  enheter: {
    agressoId: string;
    agressoNiva: string;
    enhetNavn: string;
    enhetType: "AgressoEnhet";
    ansattForhold: "Ansatt" | "Leder";
  }[];
  enheterLederFor: {
    agressoId: string;
    agressoNiva: string;
    enhetNavn: string;
    enhetType: "AgressoEnhet";
    ansattForhold: "Ansatt" | "Leder";
  }[];
}

const templateMedarbeider: Medarbeider = {
  navIdent: "[ukjent]",
  fulltNavn: "[ukjent]",
  epost: "[ukjent]",
  telefon: [],
  ressurstype: [],
  sluttdato: "[ukjent]",
  ledere: [],
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

  out.telefon = ressurs.telefon;

  out.ressurstype = ressurs.ressurstype;

  out.sluttdato = ressurs.sluttdato;

  out.ledere = ressurs.ledere.map((it) => {
    const lederRes = it.ressurs;
    return {
      navIdent: lederRes.navIdent,
      fulltNavn: fulltNavnForRessurs(lederRes),
    };
  });

  if (ressurs.koblinger.length !== 0) {
    const tmp: {
      agressoId: string;
      agressoNiva: string;
      enhetNavn: string;
      enhetType: "AgressoEnhet";
      ansattForhold: "Ansatt" | "Leder";
    }[] = ressurs.koblinger.map((it) => {
      return {
        agressoId: it.organisasjonsenhet.agressoId,
        agressoNiva: it.organisasjonsenhet.orgNiv,
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
      agressoNiva: string;
      enhetNavn: string;
      enhetType: "AgressoEnhet";
      ansattForhold: "Ansatt" | "Leder";
    }[] = ressurs.lederFor.map((it) => {
      return {
        agressoId: it.organisasjonsenhet.agressoId,
        agressoNiva: it.organisasjonsenhet.orgNiv,
        enhetNavn: it.organisasjonsenhet.navn,
        enhetType: "AgressoEnhet",
        ansattForhold: "Leder",
      };
    });
    out.enheterLederFor = tmp;
  }

  return out;
}

function TelDisplay(props:  {telArr: {nummer: string, type: string, beskrivelse: string}[]}): JSX.Element {
    const lines: string[] = [];
    for(let k = 0; k<props.telArr.length; k+=1){
        const tlf = props.telArr[k];
        let tlfInfo = "[ukjent]";
        if(tlf){
            const nrTmp = tlf.nummer.startsWith("47") ? tlf.nummer.substring(2) : tlf.nummer;
            let nrAcc = "";
            for(let k = 0; k <nrTmp.length; k+=2){
                nrAcc += nrTmp.substring(k,k+2) + " ";
            }
            const nr = nrAcc.substring(0,nrAcc.length-1);
            const tpTmp = tlf.type.startsWith("NAV_") ? tlf.type.substring(4) : tlf.type;
            const tp = tpTmp.toLocaleLowerCase().replaceAll("_","");
            tlfInfo = nr + " (" + tp + ") ";
            lines.push(tlfInfo);
        }
    }

    return <span>
        {lines.map(it => <span key={it}>{it} <br/> </span>)}
    </span>;
}


function Infopanel(props: { medarbeider: Medarbeider }): JSX.Element {

  return (
    <LabeledItemsgrid>
      <label htmlFor="navIdentHeader">Nav-ident: </label>
      <span id="navIdentHeader">{props.medarbeider.navIdent}</span>
      <label htmlFor="fulltNavnHeader">Navn: </label>
      <span id="fulltNavnHeader">{props.medarbeider.fulltNavn}</span>
      <label htmlFor="ansattTypeHeader">Kategori: </label>
      <span id="ansattTypeHeader">{props.medarbeider.ansattKategori}</span>
      <label htmlFor="ansattEpost">Epost: </label>
      <span id="ansattEpost">{props.medarbeider.epost}</span>
      <label htmlFor="ressurstype">Ressurstype: </label>
      <span id="ressurstype">{props.medarbeider.ressurstype}</span>
      <label htmlFor={"ansattTlf"}>Telefon: </label>
      <span id={"ansattTlf"}><TelDisplay telArr={props.medarbeider.telefon}/></span>
      <label htmlFor="sluttdato">Sluttdato: </label>
      <span id="sluddato">{props.medarbeider.sluttdato}</span>

      {props.medarbeider.ledere.length !== 0
        ? props.medarbeider.ledere.map((it) => {
            const labelId = "linkTilLeder" + it.navIdent;
            return (
              <Fragment key={it.navIdent}>
                <label htmlFor={labelId}>Leder: </label>
                <span id={labelId}>
                  {it.fulltNavn}{" "}
                  <Link to={"/medarbeidere/" + it.navIdent}>
                    ({it.navIdent})
                  </Link>
                </span>
              </Fragment>
            );
          })
        : null}
    </LabeledItemsgrid>
  );
}

function OrgenhetListview(props: {
  orgenheter: {
    agressoId: string;
    agressoNiva: string;
    enhetNavn: string;
    enhetType: "AgressoEnhet";
  }[];
}): JSX.Element {

  const history = useNavigate();

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
          id: "enhetNavn",
          label: "Enhet navn",
          type: "string",
          renderCell: (item, value) => (
            <ItemLink
              history={history}
              url={"/organisasjonsenheter/" + agressoIdDataToUrl(item.agressoId, item.agressoNiva, item.enhetNavn)}
              label={item.enhetNavn}
            />
          ),
        },
        {
          id: "agressoId",
          label: "AgressoId",
          type: "string",
        },
        { id: "agressoNiva", label: "Enhet nivå", type: "string" },
        { id: "enhetType", label: "Enhet type", type: "string" },
      ]}
      itemsPerPage={50}
      animatable={false}
      searchable={false}
      selectable={false}
      sortable={true}
      sort={{ column: "enhetNavn", order: "asc" }}
      striped={false}
      summary={false}
      compact={true}
    />
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
    <Nompagewrap pagetitle="Medarbeidere">
      {/* <NomUiHeader /> */}
      <p>
        Bruk ID i adresselinjen for å se på forskjellige medarbeidere (f.eks
        /medarbeidere/X123456)
      </p>
      <div className="MedarbeidereLinkview">{out}</div>
    </Nompagewrap>
  );
}
