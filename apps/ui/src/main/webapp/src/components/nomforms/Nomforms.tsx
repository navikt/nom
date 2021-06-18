import React, {ReactNode} from "react";
import {Controller, ControllerRenderProps, FieldError, FieldPath, FieldValues, useFormContext} from 'react-hook-form';
import {Input, Textarea} from 'nav-frontend-skjema';
import {RegisterOptions} from 'react-hook-form/dist/types/validator';
import {getFeilmelding} from './NomFormValidering';

export interface FieldDefs {
  label: string;
  id: string;
  description?: string;
  defaultValue: string;
  rules?: Omit<RegisterOptions<FieldValues, FieldPath<FieldValues>>, 'valueAsNumber' | 'valueAsDate' | 'setValueAs'>
}

export type FormSpec<T> = {
  [K in keyof T]: FieldDefs
};

type ControlledProps = {
  field: ControllerRenderProps,
  error: FieldError | undefined
};

export function Controlled(props: FieldDefs & {children: (props: ControlledProps) => React.ReactElement}): JSX.Element {
  const {control, formState: {errors}} = useFormContext();
  return (
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
  );
}

export function LabeledTextfield(props: FieldDefs): JSX.Element {
  return <Controlled {...props}>
    {({field, error}) =>
      <Input
        id={props.id}
        label={props.label} description={props.description}
        feil={getFeilmelding(error)}
        onChange={field.onChange}
        onBlur={field.onBlur}
        value={field.value}
      />
    }
  </Controlled>;
}


export function LabeledTextarea(props: FieldDefs): JSX.Element {
  return <Controlled {...props}>
    {({field, error}) =>
      <Textarea
        id={props.id}
        label={props.label} description={props.description}
        feil={getFeilmelding(error)}
        onChange={field.onChange}
        onBlur={field.onBlur}
        value={field.value}
      />
    }
  </Controlled>;
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
            onChange={field.onChange}
            onBlur={field.onBlur}
            value={field.value}
          />
        }
      </Controlled>
    </div>
  );
}

export function LabeledFieldArea(props: {children: ReactNode}): JSX.Element {
  return <div className="Nomforms__Labeledtextfieldarea">{props.children}</div>;
}