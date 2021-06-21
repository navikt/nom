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
  person: GqlRessurs
}

const lookupFnr = gql`
    query lookupPersonNavn($fnr: String!) {
        person(where: {personIdent: $fnr}) {
            nomId
            personIdent
            navIdent
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
    label: "Personident",
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
    {data && <AlertStripe type='suksess'>Bruker {data?.nomId} opprettet i request {data?.requestSetId}/{data?.requestId}</AlertStripe>}
    {error && <AlertStripe type={'feil'}>{error?.message}</AlertStripe>}
  </div>
);

export default function Nybruker(): JSX.Element {
  const [fnrLookup, setFnrLookup] = useState<string | undefined>();
  const {data: personLookupData, error: ressursLookupError} = useQuery<LookupRessursNavnResult, {fnr: string}>(lookupFnr, {
    variables: {fnr: fnrLookup || ''},
    skip: !fnrLookup || fnrLookup.length !== 11
  });
  const [opprett, {data, error}] = useMutation<{opprettRessurs: BestillingResponse}, NyBruker>(opprettBruker);

  const methods = useForm<NyBruker>({reValidateMode: 'onBlur'});
  const {reset, setValue, setError, clearErrors, formState, handleSubmit} = methods;

  useEffect(() => {
    if (personLookupData?.person?.person?.navn.fornavn) {
      setValue(spec.visningsnavn.id, fulltNavnForRessurs(personLookupData?.person));
      clearErrors(spec.visningsnavn.id);
    } else {
      setValue(spec.visningsnavn.id, '');
    }
    if (personLookupData?.person.navIdent) {
      setError(spec.fnr.id, {message: `Personident er allerede registrert i NOM på ident ${personLookupData.person.navIdent}`, type: 'FINNES_ALLEREDE'});
    }
  }, [personLookupData, setError, setValue]);

  const doReset = (): void => reset(defaultValues);
  const submit = (variables: NyBruker): Promise<any> => opprett({variables}).then(doReset);

  return (
    <Nompagewrap pagetitle="Ny bruker">
      <FormProvider {...methods}>
        <form onSubmit={handleSubmit(submit)}>
          <SkjemaGruppe style={{maxWidth: '600px'}}>
            <LabeledTextfield {...spec.fnr} onChange={(val) => setFnrLookup(val)}/>
            <LabeledTextfield {...spec.visningsnavn} />
            <LabeledTextfield {...spec.tlf} />
            <LabeledTextarea {...spec.beskrivelse} />
            <LabeledDatefield {...spec.gyldigfom}/>
          </SkjemaGruppe>

          <Feilmeldinger errors={formState.errors} spec={spec}/>

          <Hovedknapp htmlType="submit" style={{marginRight: '6px'}} disabled={formState.isSubmitting}>Legg inn bestilling</Hovedknapp>
          <Knapp htmlType="reset" onClick={doReset}>reset</Knapp>
        </form>
      </FormProvider>
      <ResultInfo data={data?.opprettRessurs} error={error || ressursLookupError}/>
    </Nompagewrap>
  );
}
