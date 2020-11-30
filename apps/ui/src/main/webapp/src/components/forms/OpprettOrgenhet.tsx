import React, { useEffect } from "react";
import { Input } from "nav-frontend-skjema";
import { Hovedknapp } from "nav-frontend-knapper";
import { useState } from "react";
import usePostOrgEnhet from "../../api/usePostOrgEnhet";
import { useParams } from "react-router-dom";
import { AlertStripeFeil, AlertStripeSuksess } from "nav-frontend-alertstriper";
import NavFrontendSpinner from "nav-frontend-spinner";

interface OrgEnhetPostRequest {
  parentId: string;
  enhetNavn: string;
  enhetType: string;
}

export default function OpprettOrgenhet(props: {
  parentId: string;
}): JSX.Element {
  const [submitToggle, setSubmitToggle] = useState(false);
  // const navnRef = useRef<HTMLElement>(null);
  // const typeRef = useRef<HTMLInputElement | null>(null);
  const { id } = useParams<any>();

  const [payload, setPayload] = useState<OrgEnhetPostRequest | null>(null);
  const [postResult] = usePostOrgEnhet(payload);

  const [navn, setNavn] = useState("");
  const [type, setType] = useState("");

  useEffect(() => {
    if (submitToggle) {
      setPayload({ enhetNavn: navn, enhetType: type, parentId: id });
    }
  }, [submitToggle, id, navn, type]);

  switch (postResult.lookupState) {
    case "PENDING": {
      return <NavFrontendSpinner />;
    }
    case "WAITING": {
      return (
        <div>
          <p>Ny enhet vil tilknyttes enhet med ID {props.parentId} </p>
          <Input
            title="Navn"
            label="Navn"
            value={navn}
            onChange={(ev) => setNavn(ev.target.value)}
          ></Input>
          <Input
            title="Type"
            label="Type"
            value={type}
            onChange={(ev) => setType(ev.target.value)}
          ></Input>
          <Hovedknapp onClick={() => setSubmitToggle(!submitToggle)}>
            {submitToggle ? "confirm" : "submit"}
          </Hovedknapp>
        </div>
      );
    }
    case "ERROR": {
      return (
        <div>
          <AlertStripeFeil>{postResult.error.message}</AlertStripeFeil>
        </div>
      );
    }
    case "RESOLVED": {
      return <AlertStripeSuksess>Ny orgenhet opprettet!</AlertStripeSuksess>;
    }
  }
}
