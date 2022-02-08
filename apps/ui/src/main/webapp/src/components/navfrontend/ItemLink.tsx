import {Link, NavigateFunction, useNavigate} from 'react-router-dom';
import React from 'react';


export const ItemLink = (props: {url: string, label: string, history: any}): React.ReactElement => {
  // nav frontend tabell fails to render some tostring of component if it contains hooks aka from the Link
  if (!props.history) {
    return <>{props.label}</>;
  }

  const nav: NavigateFunction = props.history;

  return (
      <button className={"navlinkbutton"} onClick={() => nav(props.url)}>
        {props.label}
      </button>
  );
};