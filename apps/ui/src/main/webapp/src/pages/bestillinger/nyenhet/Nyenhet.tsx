import React from "react";
import {FieldDefs, LabeledFieldArea,} from "../../../components/nomforms/Nomforms";
import Nompagewrap from "../../../components/nompagewrap/Nompagewrap";

const fieldspec: {[x: string]: FieldDefs} = {
  visnavn: {
    label: "Enhetsnavn",
    id: "visnvn",
    defaultValue: '',
  },
  beskr: {
    label: "Beskrivelse",
    id: "beskr",
    defaultValue: '',
  },
  gyldigfom: {
    label: "Gyldig fra og med",
    id: "gyldigfom",
    defaultValue: '',
  },
};

export default function Nyenhet(props: any): JSX.Element {
  function handleSubmit(ev: any): void {
    ev.preventDefault();

    const out = {};
    Object.keys(fieldspec)
    .map((it) => fieldspec[it].id)
    .forEach((it) => {
      out[it] = ev.target.elements[it].value;
    });
    alert("TODO. Håndtere bestilling, " + JSON.stringify(out));
  }

  return (
    <Nompagewrap pagetitle="Ny enhet">
      <form onSubmit={handleSubmit}>
        <LabeledFieldArea>
          {/* <LabeledTextfield {...fieldspec.visnavn} />*/}
          {/* <LabeledTextarea {...fieldspec.beskr} />*/}
          {/* <LabeledDatefield {...fieldspec.gyldigfom} />*/}
        </LabeledFieldArea>
        <button type="submit">Legg til bestilling</button>
        <button type="reset">reset</button>
      </form>
    </Nompagewrap>
  );
}
