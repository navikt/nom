import React from "react";
import {FieldDefs, LabeledDatefield, LabeledTextfield} from "../../../components/nomforms/Nomforms";
import {Feilmeldinger, validateFnr,} from "../../../components/nomforms/NomFormValidering";
import Nompagewrap from "../../../components/nompagewrap/Nompagewrap";
import {Hovedknapp, Knapp} from 'nav-frontend-knapper';
import {SkjemaGruppe} from 'nav-frontend-skjema';
import {FormProvider, useForm} from 'react-hook-form';
import moment, {HTML5_FMT} from "moment";

interface NyBruker {
  pnr: string,
  visnavn: string,
  beskr: string,
  gyldigfom: string,
}

const spec: { [K in keyof NyBruker]: FieldDefs } = {
  pnr: {
    label: "Personnummer",
    id: "pnr",
    defaultValue: '',
    rules: {
      validate: validateFnr
    }
  },
  visnavn: {
    label: "Visningsnavn",
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
    defaultValue: moment().format(HTML5_FMT.DATE),
  },
};

const defaultValues = Object.keys(spec).reduce((defaults, key) => {
  defaults[key] = spec[key].defaultValue;
  return defaults;
}, {} as NyBruker);

export default function Nybruker(): JSX.Element {
  const onSubmit = (val: any): void => {
    alert("TODO. Håndtere bestilling, " + JSON.stringify(val));
  };

  const formHook = useForm<NyBruker>();

  return (
    <Nompagewrap pagetitle="Ny bruker">
      <FormProvider {...formHook}>
        <form onSubmit={formHook.handleSubmit(onSubmit)}>
          <SkjemaGruppe style={{maxWidth: '600px'}}>
            <LabeledTextfield {...spec.pnr} />
            <LabeledTextfield {...spec.visnavn} />
            <LabeledTextfield {...spec.beskr} />
            <LabeledDatefield {...spec.gyldigfom}/>
          </SkjemaGruppe>

          <Feilmeldinger errors={formHook.formState.errors}/>

          <Hovedknapp htmlType="submit" style={{marginRight: '6px'}}>Legg inn bestilling</Hovedknapp>
          <Knapp htmlType="reset" onClick={() => formHook.reset(defaultValues)}>reset</Knapp>
        </form>
      </FormProvider>
    </Nompagewrap>
  );
}
