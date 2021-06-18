import {fnr} from '@navikt/fnrvalidator';
import {DeepMap, FieldError, FieldValues} from 'react-hook-form';
import {Feiloppsummering, SkjemaGruppe} from 'nav-frontend-skjema';
import React from 'react';
import {FormSpec} from './Nomforms';


const feilmelding = {
  required: 'Feltet er påkrevd'
};

export const getFeilmelding = (e?: FieldError, prefix?: string): string => {
  if (!e) {
    return '';
  }
  const message = e.message || feilmelding[e.type] || 'Mangler feilmelding for type: ' + e.type;
  if (prefix) {
    return `${prefix}: ${message}`;
  }
  return message;
};

export const validateFnr = (value: string): string | boolean => {
  const res = fnr(value);
  return res.status === 'valid' ? true : res.reasons.join(', ');
};

export const Feilmeldinger = <T extends FieldValues>({errors, spec}: {errors: DeepMap<T, FieldError>, spec: FormSpec<T>}): JSX.Element => {
  return (
    <div style={{marginBottom: '16px'}}>
      {!!Object.keys(errors).length && (
        <SkjemaGruppe>
          <Feiloppsummering
            tittel="For å gå videre må du rette opp følgende:"
            feil={Object.values(errors)
            .map(e => {
              const fieldId = e.ref?.name;
              const specElement = spec[fieldId];
              return ({skjemaelementId: fieldId, feilmelding: getFeilmelding(e, specElement?.label)});
            })}
          />
        </SkjemaGruppe>
      )}
    </div>
  );
};