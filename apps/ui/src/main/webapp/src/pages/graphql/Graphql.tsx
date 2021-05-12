import React from "react";
import { Link, useParams } from "react-router-dom";
import { useGetGraphql } from "../../api/graphql_fetch_hooks";
import NomUiHeader from "../../components/header/NomUiHeader";
import { createOrganisasjonsenhetQuery } from "../../api/graphql_queries";
import Nompagewrap from "../../components/nompagewrap/Nompagewrap";
import { useLocalstorageState } from "../../common/uselocalstorage";

const graphQlSchemaUri =
  "https://github.com/navikt/nom/blob/master/apps/api" +
  "/src/main/resources/graphql/query.graphqls";

const defaultQuery = `query {
  organisasjonsenhet(where: {agressoId: "NAV"}) {
      agressoId
      navn
  }
}`;

export default function Graphql(): JSX.Element {
  const txtareaRef = React.useRef<any>();
  const [txt, setTxt] = useLocalstorageState("nom_graphql_query", defaultQuery);

  function handleRefresh(): any {
    if (txtareaRef.current) {
      // alert(txtareaRef.current.value);
      setTxt(txtareaRef.current.value);
    }
  }

  React.useEffect(() => {
    txtareaRef.current.value = txt;
  }, [txt]);

  function handleReset(): any {
    setTxt(defaultQuery);
    txtareaRef.current.value = defaultQuery;
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
    <Nompagewrap pagetitle="Graphql">
      <div className="Graphql">
        {/* <NomUiHeader /> */}
        <button onClick={handleRefresh}>Refresh</button>
        <button onClick={handleReset}>Reset</button>
        <span>
          (Se <a href={graphQlSchemaUri}>Graphql skjema her</a>)
        </span>
        <br />
        <div className="Graphql__textpanels">
          <textarea
            className="Graphql__textarea"
            spellCheck={false}
            ref={txtareaRef}
            defaultValue={txt}
          ></textarea>

          <pre className="Graphql__jsondump">{out}</pre>
        </div>
      </div>
    </Nompagewrap>
  );
}
