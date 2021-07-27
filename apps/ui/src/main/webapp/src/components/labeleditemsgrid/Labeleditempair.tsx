import React, { Fragment, ReactElement, ReactNode } from "react";

export interface LabeledItemPairProps {
  id?: string;
  children: [ReactElement, ReactElement];
}

export default function LabeledItemPair(
  props: LabeledItemPairProps
): JSX.Element {
  const Label = props.children[0];
  const Item = props.children[1];

  return (
    <Fragment>
      {React.cloneElement(Label, { htmlFor: props.id })}
      <div>{React.cloneElement(Item, { id: props.id })}</div>
    </Fragment>
  );
}
