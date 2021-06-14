import {Link, useHistory} from 'react-router-dom';
import React from 'react';


export const ItemLink = (props: {url: string, label: string}): React.ReactElement => {
  const history = useHistory();
  // nav frontend tabell fails to render some tostring of component if it contains hooks aka from the Link
  if (!history) {
    return <>{props.label}</>;
  }
  return (
    <Link to={props.url}>
      {props.label}
    </Link>
  );
};