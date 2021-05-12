import React, { Fragment, ReactNode } from "react";

export interface FieldDefs {
  label: string;
  id: string;
  description?: string;
}

export function LabeledTextfield(props: FieldDefs): JSX.Element {
  return (
    <Fragment>
      <label htmlFor={props.id}>{props.label}</label>
      <input id={props.id} name={props.id} />
    </Fragment>
  );
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
    <Fragment>
      <label htmlFor={props.id}>{props.label}</label>
      <input
        type="date"
        id={props.id}
        name={props.id}
        data-date-format="YYYY-MM-DD"
      ></input>
    </Fragment>
  );
}

export function LabeledFieldArea(props: { children: ReactNode }): JSX.Element {
  return <div className="Nomforms__Labeledtextfieldarea">{props.children}</div>;
}
