import Ekspanderbartpanel from "nav-frontend-ekspanderbartpanel";
import { Knapp } from "nav-frontend-knapper";
import React, { Fragment, useState } from "react";
import RedigerOrgenhet from "../forms/RedigerOrgenhet";

export default function EnhetOm(props: { navn: string; type: string }): JSX.Element{
  const [show, setShow] = useState(false);
  const knappTekst = show ? "Tilbake" : "Rediger";
  return (
    <div className="EnhetOversikt__EnhetOm">
      <Knapp onClick={() => setShow(!show)}>{knappTekst}</Knapp>
      {!show ? (
        <Fragment>
          <dl>
            <dt>Navn</dt>
            <dd>{props.navn}</dd>
            <dt>Type</dt>
            <dd>{props.type}</dd>
          </dl>
          <Ekspanderbartpanel tittel="Identifikatorer i andre systemer">
            <ul>
              <li>TODO1</li>
              <li>TODO2</li>
            </ul>
          </Ekspanderbartpanel>
          <Ekspanderbartpanel tittel="Kontakter">
            <ul>
              <li>TODO1</li>
              <li>TODO2</li>
            </ul>
          </Ekspanderbartpanel>
        </Fragment>
      ) : null}
      {show ? <RedigerOrgenhet /> : null}
    </div>
  );
}
