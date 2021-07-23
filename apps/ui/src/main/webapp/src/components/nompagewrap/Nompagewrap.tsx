import React, { ReactNode } from "react";
import { Link } from "react-router-dom";
import useFetchLoggedInName from "../../api/useFetchLoggedInName";
import { useLocalstorageState } from "../../common/uselocalstorage";

function SidepanelStickyContent(props: { children: ReactNode }): JSX.Element {
  return (
    <div className="Nompagewrap__sidepanel">
      <div className="Nompagewrap__sidepanelcontent">{props.children}</div>
    </div>
  );
}

function HomeLink(): JSX.Element {
  return (
    <Link to="/" className="Nompagewrap__homelink">
      <b>NOM</b>
      <p>NAV organisasjons-master</p>
    </Link>
  );
}

function Sidepanel(): JSX.Element {
  return (
    <SidepanelStickyContent>
      <HomeLink />
      <hr />
      <Link to="/organisasjonsenheter">Organisasjonsenheter</Link>
      <Link to="/medarbeidere">Medarbeidere</Link>
      <Link to="/bestillinger">Bestillinger</Link>
      <Link to="/bestillingsgodkjenning">Godkjenning</Link>
      {/* <Link to="/enhetstyper">Enhetstyper</Link> */}
      <hr />
      <Link to="/graphql">Graphql</Link>
    </SidepanelStickyContent>
  );
}

function Loginbox(): JSX.Element {
  const [res] = useFetchLoggedInName();
  const [res2, setRes2] = useLocalstorageState("loginName", "");
  let loginName = res2 ?? "???";
  if (res.lookupState === "RESOLVED") {
    loginName = res.result;
    setRes2(loginName);
  }
  return <span className="Nompagewrap__loginbox">{loginName}</span>;
}

function Topbar(props: { pagetitle?: string }): JSX.Element {
  const pagetitle = props.pagetitle ?? "Pagetitle";
  return (
    <div className="Nompagewrap__topbar">
      <span>
        <h1>{pagetitle}</h1>
      </span>
      <Loginbox></Loginbox>
    </div>
  );
}

// EXPORT

export interface NompagewrapProps {
  children: ReactNode;
  pagetitle?: string;
}

export default function Nompagewrap(props: NompagewrapProps): JSX.Element {
  return (
    <div className="Nompagewrap">
      <Sidepanel />
      <div>
        <Topbar pagetitle={props.pagetitle} />
        <div>{props.children}</div>
      </div>
    </div>
  );
}
