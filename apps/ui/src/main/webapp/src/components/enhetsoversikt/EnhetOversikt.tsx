import React, { useEffect, useState } from "react";
import useGetOrgEnhet from "../../api/useGetOrgEnhet";
import EnhetBreadcrumbs from "./EnhetBreadcrumbs";
import EnhetOm from "./EnhetOm";
import EnhetTeam from "./EnhetTeam";
import UnderEnheter from "./UnderEnheter";
import Tabs from "nav-frontend-tabs";
import Panel from "nav-frontend-paneler";
import NavFrontendSpinner from "nav-frontend-spinner";
import { OrgEnhetData } from "../../common/types";
import { AlertStripeFeil } from "nav-frontend-alertstriper";
import Lenke from "nav-frontend-lenker";
import { VenstreChevron } from "nav-frontend-chevron";
import { Knapp } from "nav-frontend-knapper";
import { useHistory } from "react-router-dom";

export interface EnhetOversiktProps {
  id: string;
}

// type Action = { type: "REQUEST_REFRESH" } | { type: "COMPLETE_REFRESH" };

// interface State {
//   refreshRequested: boolean;
// }

// function reducer(s: State, a: Action): State {
//   console.log({ reducingWithAction: a });
//   switch (a.type) {
//     case "REQUEST_REFRESH":
//       return { refreshRequested: true };
//     case "COMPLETE_REFRESH":
//       return { refreshRequested: false };
//     default:
//       return s;
//   }
// }

function extractTabFromUrlHash(hash: string): number{
  if(!hash.startsWith("#tab=")) {
    return 0;
  }
  const n = hash.split("=")[1];
  if(["0","1","2","3"].includes(n)) {
    return parseInt(n,10);
  }
  return 0;
}


function EnhetOversiktResolved(props: {enhet: OrgEnhetData; refresh: () => void}): JSX.Element{
  const history = useHistory();
  const tabInit = extractTabFromUrlHash(history.location.hash);
  const [tabSelect, setTabSelect] = useState<number>(tabInit);
  const transformertSti = [...props.enhet.koblinger.sti, props.enhet.data];

  useEffect(function tabHandle(): void{
    history.replace(history.location.pathname + "#tab="+tabSelect);
  },[tabSelect, history]);

  return (
    <div className="EnhetOversikt">
      <EnhetBreadcrumbs sti={transformertSti} />
      <h1>
        <span className="RefreshImg" onClick={() => console.error("TODO")}>
        <VenstreChevron />
        </span>
        {props.enhet.data.enhetNavn} 
        <img
          className="RefreshImg"
          alt="Refresh"
          src="/assets/synchronize-1.svg"
          onClick={props.refresh}
        />
      </h1>

      <Tabs
        defaultAktiv={tabSelect}
        onChange={(_: any, idx: number) => setTabSelect(idx)}
        tabs={[
          { label: "Tilknyttede enheter" },
          { label: "Om enheten" },
          { label: "Tilknyttede teams" },
          { label: "Operasjoner..." },
        ]}
      />
      <Panel className="EnhetOversikt__TabPanel">
        {tabSelect === 0 ? (
          <UnderEnheter
            underEnheter={props.enhet.koblinger.underEnheter}
            refresh={props.refresh}
          />
        ) : null}
        {tabSelect === 1 ? (
          <EnhetOm
            navn={props.enhet.data.enhetNavn}
            type={props.enhet.data.enhetType}
          />
        ) : null}
        {tabSelect === 2 ? <EnhetTeam /> : null}
        {tabSelect === 3 ? <div>TODO...</div> : null}
      </Panel>
    </div>
  );
}

export default function EnhetOversikt(props: EnhetOversiktProps): JSX.Element {
  const [enhetLookup, refresh] = useGetOrgEnhet(props.id);

  switch (enhetLookup.lookupState) {
    case "RESOLVED": {
      return <EnhetOversiktResolved enhet={enhetLookup.result} refresh={refresh}/>;
    }
    case "WAITING":
    case "PENDING": {
      return (
        <div className="EnhetOversikt">
          <NavFrontendSpinner />
          <br />
          Henter data for orgenhet med id = {props.id} ...
        </div>
      );
    }
    case "ERROR": {
      console.error(enhetLookup);
      return (
        <div className="EnhetOversikt">
          <AlertStripeFeil>{enhetLookup.error.message}</AlertStripeFeil>
          <Knapp onClick={refresh}>Refresh</Knapp>
          <Lenke href="/app/oversikt">Tilbake til oversikt</Lenke>
        </div>
      );
    }
  }
}
