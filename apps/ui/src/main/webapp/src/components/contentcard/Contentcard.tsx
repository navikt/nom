import React, { ReactNode } from "react";
import { Link } from "react-router-dom";

function LinkWithoutVisited(props: {
  to: string;
  children: ReactNode;
}): JSX.Element {
  return (
    <Link
      className="Contentcard__Linkwithoutvisited Contentcard__Linkcentered"
      to={props.to}
    >
      {props.children}
    </Link>
  );
}

// #######################

export function Contentcard(props: any): JSX.Element {
  return <div className="Contentcard">{props.children}</div>;
}

export function LinkCard(props: {
  title: string;
  uri: string;
  children?: ReactNode;
}): JSX.Element {
  return (
    <Contentcard>
      {/* <div className="Contentcard__Genericcenter"> */}
        <h2 className="Contentcard__centeredtext">
          <LinkWithoutVisited to={props.uri}>{props.title}</LinkWithoutVisited>
        </h2>
      {/* </div> */}
      {props.children}
    </Contentcard>
  );
}
