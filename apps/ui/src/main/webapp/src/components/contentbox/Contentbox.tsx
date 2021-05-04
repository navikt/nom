import React, { ReactChild, ReactChildren, ReactNode } from "react";

export default function Contentbox(props: {
  children: ReactNode;
  label: string;
}): JSX.Element {
  return (
    <div className="Contentbox">
      <span className="Contentbox__label">{props.label}</span>
      <div className="Contentbox__children">{props.children}</div>
    </div>
  );
}
