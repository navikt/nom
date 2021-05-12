import React, { Fragment, ReactNode } from "react";
import {
  FieldDefs,
  LabeledDatefield,
  LabeledTextarea,
  LabeledTextfield,
  LabeledFieldArea,
} from "../../../components/nomforms/Nomforms";
import Nompagewrap from "../../../components/nompagewrap/Nompagewrap";

const fieldspec: { [x: string]: FieldDefs } = {
  pnr: {
    label: "Personnummer",
    id: "pnr",
  },
  visnavn: {
    label: "Visningsnavn",
    id: "visnvn",
  },
  beskr: {
    label: "Beskrivelse",
    id: "beskr",
  },
  gyldigfom: {
    label: "Gyldig fra og med",
    id: "gyldigfom",
  },
};

export default function Nybruker(props: any): JSX.Element {
  function handleSubmit(ev: any): void {
    ev.preventDefault();

    const out = {};
    // console.dir(ev.target);
    Object.keys(fieldspec)
      .map((it) => fieldspec[it].id)
      .forEach((it) => {
        out[it] = ev.target.elements[it].value;
      });
      alert("TODO. Håndtere bestilling, " + JSON.stringify(out));
  }

  return (
    <Nompagewrap pagetitle="Ny bruker">
      <form onSubmit={handleSubmit}>
        <LabeledFieldArea>
          <LabeledTextfield {...fieldspec.pnr} />
          <LabeledTextfield {...fieldspec.visnavn} />
          <LabeledTextarea {...fieldspec.beskr} />
          <LabeledDatefield {...fieldspec.gyldigfom} />
        </LabeledFieldArea>
        <button type="submit">Legg inn bestilling</button>
        <button type="reset">reset</button>
      </form>
    </Nompagewrap>
  );
}
