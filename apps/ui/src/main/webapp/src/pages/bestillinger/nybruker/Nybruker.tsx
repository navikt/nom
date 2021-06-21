import React, {useEffect, useState} from "react";
import {FormSpec, LabeledDatefield, LabeledTextarea, LabeledTextfield} from "../../../components/nomforms/Nomforms";
import {Feilmeldinger, validateFnr,} from "../../../components/nomforms/NomFormValidering";
import Nompagewrap from "../../../components/nompagewrap/Nompagewrap";
import {Hovedknapp, Knapp} from 'nav-frontend-knapper';
import {SkjemaGruppe} from 'nav-frontend-skjema';
import {FormProvider, useForm} from 'react-hook-form';
import moment, {HTML5_FMT} from "moment";
import {ApolloError, gql, useMutation, useQuery} from '@apollo/client';
import AlertStripe from 'nav-frontend-alertstriper';
import {GqlRessurs} from '../../../common/gqltypes';
import {fulltNavnForRessurs} from '../../../common/navnutils';

interface NyBruker {
  fnr: string,
  visningsnavn: string,
  tlf: string,
  beskrivelse: string,
  gyldigfom: string,
}

interface BestillingResponse {
  nomId
  requestId
  requestSetId
}

const opprettBruker = gql`
    mutation opprett($fnr: String!, $visningsnavn: String, $beskrivelse: String) {
        opprettRessurs(request: {
            metadata: {aktivering: IMMEDIATE, beskrivelse: $beskrivelse},
            fnr: $fnr,
            visningsnavn: $visningsnavn
        }) {
            nomId
            requestId
            requestSetId
        }
    }
`;

interface LookupRessursNavnResult {
  lookupRessursNavn: GqlRessurs
}

const lookupFnr = gql`
    query lookupReesursNavn($fnr: String!) {
        ressurs(where: {personIdent: $fnr}) {
            person {
                navn {
                    fornavn
                    mellomnavn
                    etternavn
                }
            }
        }
    }
`;

const spec: FormSpec<NyBruker> = {
  fnr: {
    label: "Personnummer",
    id: "fnr",
    defaultValue: '',
    rules: {
      validate: validateFnr
    }
  },
  visningsnavn: {
    label: "Visningsnavn",
    id: "visningsnavn",
    defaultValue: '',
  },
  tlf: {
    label: "Telefonnummer",
    id: "tlf",
    defaultValue: '',
    rules: {
      required: false
    }
  },
  beskrivelse: {
    label: "Beskrivelse",
    id: "beskrivelse",
    defaultValue: '',
    rules: {
      required: false
    }
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

const ResultInfo = ({data, error}: {data?: BestillingResponse, error?: ApolloError}): JSX.Element => (
  <div style={{marginTop: '16px', maxWidth: '600px'}}>
    {data && <AlertStripe type={data.requestId ? 'suksess' : 'feil'}>
      {error?.message ?? `Bruker ${data.nomId} opprettet i request ${data.requestId}`}
    </AlertStripe>}
  </div>
);

export default function Nybruker(): JSX.Element {
  const [fnrLookup, setFnrLookup] = useState<string | undefined>();
  const {data: ressursLookupData, error: ressursLookupError} = useQuery<LookupRessursNavnResult, {fnr: string}>(lookupFnr, {
    variables: {fnr: fnrLookup || ''},
    skip: !fnrLookup || fnrLookup.length !== 11
  });
  const [opprett, {data, error}] = useMutation<{opprettRessurs: BestillingResponse}, NyBruker>(opprettBruker);

  const methods = useForm<NyBruker>({});

  useEffect(() => {
    if (ressursLookupData?.lookupRessursNavn?.person?.navn.fornavn) {
      methods.setValue(spec.fnr.id, fulltNavnForRessurs(ressursLookupData?.lookupRessursNavn));
    }
  }, [fnrLookup]);

  const reset = (): void => methods.reset(defaultValues);
  const submit = (variables: NyBruker): Promise<any> => {
    return opprett({variables}).then(reset);
  };

  return (
    <Nompagewrap pagetitle="Ny bruker">
      <FormProvider {...methods}>
        <form onSubmit={methods.handleSubmit(submit)}>
          <SkjemaGruppe style={{maxWidth: '600px'}}>
            <LabeledTextfield {...spec.fnr} onChange={(val) => setFnrLookup(val)}/>
            <LabeledTextfield {...spec.visningsnavn} />
            <LabeledTextfield {...spec.tlf} />
            <LabeledTextarea {...spec.beskrivelse} />
            <LabeledDatefield {...spec.gyldigfom}/>
          </SkjemaGruppe>

          <Feilmeldinger errors={methods.formState.errors} spec={spec}/>

          <Hovedknapp htmlType="submit" style={{marginRight: '6px'}} disabled={methods.formState.isSubmitting}>Legg inn bestilling</Hovedknapp>
          <Knapp htmlType="reset" onClick={reset}>reset</Knapp>
        </form>
      </FormProvider>

      <ResultInfo data={data?.opprettRessurs} error={error || ressursLookupError}/>
    </Nompagewrap>
  );
}
