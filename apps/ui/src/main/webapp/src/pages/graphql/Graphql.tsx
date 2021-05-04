import React from "react";
import { Link, useParams } from "react-router-dom";
import { useGetGraphql } from "../../api/graphql_fetch_hooks";
import NomUiHeader from "../../components/header/NomUiHeader";
import { createOrganisasjonsenhetQuery } from "../../api/graphql_queries";

export default function Graphql(): JSX.Element {
  const { id } = useParams<any>();
  const txtareaRef = React.useRef<any>();
  const [txt, setTxt] = React.useState(createOrganisasjonsenhetQuery("NAV"));

  function handleClick(): any {
    if (txtareaRef.current) {
      // alert(txtareaRef.current.value);
      setTxt(txtareaRef.current.value);
    }
  }

  const [res, refresh] = useGetGraphql(txt);

  let out: any = <div></div>;

  switch (res.lookupState) {
    case "PENDING":
      out = "Pending";
      break;
    case "ERROR":
      out = res.error.message;
      break;
    case "RESOLVED":
    case "RESOLVEDWITHERRORS":
      out = JSON.stringify(res, null, 1);
      break;
    case "WAITING":
      out = "Waiting";
  }
  return (
    <div className="Graphql">
      <NomUiHeader />
      <p>
        <button onClick={handleClick}>click</button>
        <br />
        <div className="Graphql__textpanels">
          <textarea
            spellCheck={false}
            ref={txtareaRef}
            defaultValue={createOrganisasjonsenhetQuery("NAV")}
          ></textarea>
          <pre className="Graphql__jsondump">{out}</pre>
        </div>
      </p>
    </div>
  );
}
