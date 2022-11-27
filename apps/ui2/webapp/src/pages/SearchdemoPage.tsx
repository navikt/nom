import { css } from "@emotion/css";
import React from "react";
import { Link } from "react-router-dom";

type SearchResultRow =
  | {
      kind: "orgenhet";
      agressoId: string;
      orgNiv: string;
      navn: string;
      urlPath: string;
    }
  | {
      kind: "ressurs";
      navn: string;
      navident: string;
      urlPath: string;
    };

type SearchRowRaw = {
  navident?: string;
  visningsnavn?: string;
  agressoId?: string;
  navn?: string;
  orgNiv: string;
  __typename: "Ressurs" | "OrgEnhet";
};

function transform(inp: SearchRowRaw): SearchResultRow {
  switch (inp.__typename) {
    case "OrgEnhet": {
      return {
        kind: "orgenhet",
        agressoId: inp.agressoId || "",
        navn: inp.navn || "",
        orgNiv: inp.orgNiv || "",
        urlPath: "/org/" + inp.agressoId + "/" + inp.orgNiv,
        // inp.navn,
      };
    }
    case "Ressurs": {
      return {
        kind: "ressurs",
        urlPath: "/ressurs/" + inp.navident || "",
        navident: inp.navident || "[null]",
        navn: inp.visningsnavn || "Uten navn",
      };
    }
  }
}

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
                  orgNiv
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

  const rows = txt.map((it: SearchRowRaw) => transform(it as SearchRowRaw));

  return (
    <div
      className={css`
        padding: 2rem 20%;
      `}
    >
      <span>
        <span>Søk etter ressurser og enheter her: </span>
        <input
          onChange={(changeevent) => setDirtyString(changeevent.target.value)}
          ref={inputReference}
        ></input>
      </span>

      <div
        className={css`
          display: grid;
          grid-template-columns: min-content min-content 1fr;
          column-gap: 1rem;
          row-gap: 1rem;
          align-items: center;
          background-color: white;
        `}
      >
        {rows.map((r: SearchResultRow) => (
          <>
            {r.kind === "ressurs" && (
              <>
                <span>{r.kind}</span>
                <span>{r.navident}</span>
                <Link to={r.urlPath}>{r.navn}</Link>
              </>
            )}
            {r.kind === "orgenhet" && (
              <>
                <span>{r.kind}</span>
                <span>
                  {r.orgNiv}_{r.agressoId}
                </span>
                <Link to={r.urlPath}>{r.navn}</Link>
              </>
            )}
          </>
        ))}
      </div>
    </div>
  );
}

export default SearchdemoPage;
