import React, {Fragment, ReactNode} from "react";
import {Controller, ControllerRenderProps, FieldError, FieldPath, FieldValues, useFormContext} from 'react-hook-form';
import {Input} from 'nav-frontend-skjema';
import {RegisterOptions} from 'react-hook-form/dist/types/validator';
import {getFeilmelding} from './NomFormValidering';

export interface FieldDefs {
  label: string;
  id: string;
  description?: string;
  defaultValue: string;
  rules?: Omit<RegisterOptions<FieldValues, FieldPath<FieldValues>>, 'valueAsNumber' | 'valueAsDate' | 'setValueAs'>
}

type ControlledProps = {
  field: ControllerRenderProps,
  error: FieldError | undefined
};

export function Controlled(props: FieldDefs & {children: (props: ControlledProps) => React.ReactElement}): JSX.Element {
  const {control, formState: {errors}} = useFormContext();
  return (
    <Fragment>
      <Controller
        name={props.id}
        control={control}
        defaultValue={props.defaultValue}
        rules={({required: true, ...props.rules})}
        render={({field}) => {
          const error: FieldError = errors[props.id];
          return props.children({field, error});
        }}
      />
    </Fragment>
  );
}

export function LabeledTextfield(props: FieldDefs): JSX.Element {
  return <Controlled {...props}>
    {({field, error}) =>
      <Input
        id={props.id}
        label={props.label} description={props.description}
        feil={getFeilmelding(error)}
        {...field}/>
    }
  </Controlled>;
}

export function LabeledTextarea(props: FieldDefs): JSX.Element {
  return (
    <Fragment>
      <label htmlFor={props.id}>{props.label}</label>
      <textarea
        className="Nomforms__Labeledtextarea"
        id={props.id}
        name={props.id}
      />
    </Fragment>
  );
}

export function LabeledDatefield(props: FieldDefs): JSX.Element {
  return (
    <div className={'skjemaelement'}>
      <Controlled {...props}>
        {({field, error}) =>
          <Input
            type="date"
            id={props.id}
            label={props.label}
            data-date-format="YYYY-MM-DD"
            feil={getFeilmelding(error)}
            {...field}
          />
        }
      </Controlled>
    </div>
  );
}

export function LabeledFieldArea(props: {children: ReactNode}): JSX.Element {
  return <div className="Nomforms__Labeledtextfieldarea">{props.children}</div>;
}