import React, {ReactNode} from "react";
import {Controller, ControllerRenderProps, FieldError, FieldPath, FieldValues, useFormContext} from 'react-hook-form';
import {Input, Textarea} from 'nav-frontend-skjema';
import {RegisterOptions} from 'react-hook-form/dist/types/validator';
import {getFeilmelding} from './NomFormValidering';

export interface FieldDefs {
  label: string;
  id: string;
  description?: string;
  defaultValue?: any;
  rules?: Omit<RegisterOptions<FieldValues, FieldPath<FieldValues>>, 'valueAsNumber' | 'valueAsDate' | 'setValueAs'>
}

export interface FieldProps extends FieldDefs {
  onChange?: (val: string) => void
  disabled?: boolean
}

export type FormSpec<T> = {
  [K in keyof T]-?: FieldDefs & {id: K}
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

export function LabeledTextfield(props: FieldProps): JSX.Element {
  return <Controlled {...props}>
    {({field, error}) =>
      <Input
        id={props.id}
        label={props.label} description={props.description}
        feil={getFeilmelding(error)}
        onChange={e => {
          field.onChange(e);
          props.onChange && props.onChange(e.target.value);
        }}
        onBlur={field.onBlur}
        value={field.value}
        disabled={props.disabled}
      />
    }
  </Controlled>;
}


export function LabeledTextarea(props: FieldProps): JSX.Element {
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

export function LabeledDatefield(props: FieldProps): JSX.Element {
  return (
    <Controlled {...props}>
      {({field, error}) =>
        <Input
          type="date"
          id={props.id}
          label={props.label}
          feil={getFeilmelding(error)}
          onChange={field.onChange}
          onBlur={field.onBlur}
          value={field.value}
        />
      }
    </Controlled>
  );
}

export function LabeledFieldArea(props: {children: ReactNode}): JSX.Element {
  return <div className="Nomforms__Labeledtextfieldarea">{props.children}</div>;
}