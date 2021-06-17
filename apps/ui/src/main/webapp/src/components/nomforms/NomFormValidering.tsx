import {fnr} from '@navikt/fnrvalidator';
import {DeepMap, FieldError, FieldValues} from 'react-hook-form';
import {Feiloppsummering, SkjemaGruppe} from 'nav-frontend-skjema';
import React from 'react';


const feilmelding = {
  required: 'Feltet er påkrevd'
};

export const getFeilmelding = (e?: FieldError, oppsumering = false): string => {
  if (!e) {
    return '';
  }
  const message = e.message || feilmelding[e.type] || 'Mangler feilmelding for type: ' + e.type;
  if (oppsumering) {
    return `${(e.ref as any)?.props.label}: ${message}`;
  }
  return message;
};

export const validateFnr = (value: string): string | boolean => {
  const res = fnr(value);
  return res.status === 'valid' ? true : res.reasons.join(', ');
};

export const Feilmeldinger = ({errors}: {errors: DeepMap<FieldValues, FieldError>}): JSX.Element => {
  return (
    <div style={{marginBottom: '16px'}}>
      {!!Object.keys(errors).length && (
        <SkjemaGruppe>
          <Feiloppsummering
            tittel="For å gå videre må du rette opp følgende:"
            feil={Object.values(errors)
            .map(e => ({skjemaelementId: e.ref.props.id, feilmelding: getFeilmelding(e, true)}))}
          />
        </SkjemaGruppe>
      )}
    </div>
  );
};