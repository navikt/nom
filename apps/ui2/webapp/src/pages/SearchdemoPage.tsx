import { css } from "@emotion/react";
import { Link, Switch } from "@navikt/ds-react";
import React from "react";
import { Link as ReactRouterLink } from "react-router-dom";

type SearchResultRow =
  | {
      kind: "orgenhet";
      navn: string;
      urlPath: string;
      id: string;
    }
  | {
      kind: "ressurs";
      navn: string;
      navident: string;
      urlPath: string;
    };

type SearchRowRaw = {
  id: string;
  navident?: string;
  visningsnavn?: string;
  navn?: string;
  __typename: "Ressurs" | "OrgEnhet";
};

function transform(inp: SearchRowRaw): SearchResultRow {
  switch (inp.__typename) {
    case "OrgEnhet": {
      return {
        kind: "orgenhet",
        navn: inp.navn || "",
        urlPath: "/org/" + inp.id,
        id: inp.id || "",
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

function getBothQuery(searchString: string) {
  return `query searchDemoPage {
              search(term:"${searchString ?? ""}"){
                __typename
                ... on Ressurs {
                  navident
                  visningsnavn
                }
                ... on OrgEnhet {
                  navn
                  id
                }
              }
            }`;
}

function getOrgenhetQuery(searchString: string) {
  return `query searchDemoPage {
    searchOrgEnhet(
      term: "${searchString ?? ""}"
    filter: {orgnivSelection: ORGNIV_ORGENHET, gyldigTomSelection: IKKE_PASSERT}
  ) {
      __typename
      navn
      gyldigTom
      id
    }
  }`;
}

function SearchdemoPage() {
  // eslint-disable-next-line  @typescript-eslint/no-explicit-any
  const [txt, setTxt] = React.useState<any>([]);

  const [searchString, setSearchString] = React.useState("");
  const [dirtyString, setDirtyString] = React.useState("");
  const inputReference = React.useRef<HTMLInputElement>(null);
  const [timeoutId, setTimeoutId] = React.useState<NodeJS.Timeout>();
  const [orgenhetOnly, setOrgenhetOnly] = React.useState(false);
  const [searchState, setSearchState] = React.useState("waiting");

  // to restrict amount of network calls made
  React.useEffect(() => {
    clearTimeout(timeoutId);
    setTxt([]);
    setSearchState("Venter..");
    const timeoutIdTemporary = setTimeout(() => {
      setSearchString(dirtyString);
    }, 600);
    setTimeoutId(timeoutIdTemporary);
  }, [dirtyString]);

  React.useEffect(() => {
    setSearchState("Søker...");
    if (inputReference.current && inputReference.current.value) {
      fetch("/nom-api/graphql", {
        body: JSON.stringify({
          operationName: "searchDemoPage",
          query: orgenhetOnly
            ? getOrgenhetQuery(searchString)
            : getBothQuery(searchString),
          variables: {},
        }),
        method: "POST",
        headers: {
          "content-type": "application/json",
        },
      }).then((it) =>
        it.json().then((tx) => {
          const searchHits = orgenhetOnly
            ? tx.data.searchOrgEnhet
            : tx.data.search;
          setSearchState("Antall treff: " + searchHits.length);
          setTxt(searchHits);
        }),
      );
    }
  }, [searchString, orgenhetOnly]);

  const rows = txt.map((it: SearchRowRaw) => transform(it as SearchRowRaw));

  return (
    <div
      css={css`
        padding: 2rem 20%;
      `}
    >
      <span>
        <span>Søk etter medarbeidere og enheter her: </span>
        <input
          onChange={(changeevent) => setDirtyString(changeevent.target.value)}
          ref={inputReference}
        ></input>
        <Switch onChange={(event) => setOrgenhetOnly(event.target.checked)}>
          Bare orgenhet (orgniv orgenhet, status aktiv)
        </Switch>
      </span>
      <span>{searchState}</span>
      <div
        css={css`
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
                <Link as={ReactRouterLink} to={r.urlPath}>
                  {r.navn}
                </Link>
              </>
            )}
            {r.kind === "orgenhet" && (
              <>
                <span>{r.kind}</span>
                <span>{r.id}</span>
                <Link as={ReactRouterLink} to={r.urlPath}>
                  {r.navn}
                </Link>
              </>
            )}
          </>
        ))}
      </div>
    </div>
  );
}

export default SearchdemoPage;
