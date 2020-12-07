import React from "react";
import { Link } from "react-router-dom";
import { OrgEnhetFelter } from "../../common/types";

export interface EnhetBreadcrumbsProps {
  sti: OrgEnhetFelter[];
}

export default function EnhetBreadcrumbs(
  props: EnhetBreadcrumbsProps
): JSX.Element {
  return (
    <div className="EnhetOversikt__EnhetBreadcrumbs">
      {props.sti.map((it, idx) => {
        const link = "/oversikt/" + it.enhetNr;
        const isLast = idx === props.sti.length - 1;
        if (!isLast) {
          return (
            <div className="EnhetOversikt__Breadcrumbs__Crumb" key={link}>
              <p>{it.enhetNr}</p>
              <span>{"- " + "- ".repeat(idx)}</span>
              <Link to={link}>
                {it.enhetNavn}
              </Link>
            </div>
          );
        } else {
          return (
            <div className="EnhetOversikt__Breadcrumbs__Crumb" key={link}>
              <p>{it.enhetNr}</p>
              <span>
                {"- " + "- ".repeat(idx)}
                {it.enhetNavn}
              </span>
            </div>
          );
        }
      })}
    </div>
  );
}
