import { css } from "@emotion/css";
import React from "react";

function SearchdemoPage() {
  // eslint-disable-next-line  @typescript-eslint/no-explicit-any
  const [txt, setTxt] = React.useState<any>([]);

  const [searchString, setSearchString] = React.useState("");
  const [dirtyString, setDirtyString] = React.useState("");
  const inputReference = React.useRef<HTMLInputElement>(null);
  const [timeoutId, setTimeoutId] = React.useState<NodeJS.Timeout>();

  // to restrict amount of network calls made
  React.useEffect(() => {
    clearTimeout(timeoutId);
    const timeoutIdTemporary = setTimeout(() => {
      setSearchString(dirtyString);
    }, 600);
    setTimeoutId(timeoutIdTemporary);
  }, [dirtyString]);

  React.useEffect(() => {
    if (inputReference.current && inputReference.current.value) {
      fetch("/nom-api/graphql", {
        body: JSON.stringify({
          operationName: "U",
          query: `query U {
              search(term:"${searchString ?? ""}"){
                __typename
                ... on Ressurs {
                  navident
                  visningsnavn
                }
                ... on OrgEnhet {
                  agressoId
                  navn
                }
              }
            }`,
          variables: {},
        }),
        method: "POST",
        headers: {
          "content-type": "application/json",
        },
      }).then((it) => it.json().then((tx) => setTxt(tx.data.search)));
    }
  }, [searchString]);

  const txtOut = txt.map(
    (it: {
      navident?: string;
      visningsnavn?: string;
      agressoId?: string;
      navn?: string;
      __typename: string;
    }) =>
      it.__typename +
      ", " +
      (it.navident || it.agressoId) +
      ", " +
      (it.visningsnavn || it.navn) +
      "\n"
  );

  return (
    <div
      className={css`
        padding: 2rem 20%;
      `}
    >
      <span>
        <span>Søk etter ressurser her</span>
        <input
          onChange={(changeevent) => setDirtyString(changeevent.target.value)}
          ref={inputReference}
        ></input>
      </span>
      <pre
        className={css`
          background-color: white;
        `}
      >
        {txtOut}
      </pre>
    </div>
  );
}

export default SearchdemoPage;
