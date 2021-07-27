import React, { ReactNode } from "react";
import LabeledItemPair from "./Labeleditempair";

interface LabeleditemsgridProps {
  children: ReactNode[];
}

export default function LabeledItemsgrid(
  props: LabeleditemsgridProps
): JSX.Element {
  // return <div className="Labeleditemsgrid">{props.children}</div>;

  return <div className="Labeleditemsgrid">{props.children}</div>;
}
