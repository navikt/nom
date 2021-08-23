import { gql, useQuery } from "@apollo/client";
import React from "react";
import { GqlOrganisasjonsenhet, GqlRessurs } from "../../common/gqltypes";
import { Link } from "react-router-dom";
import { agressoIdDataToUrl } from "../../common/url_utils";

const query = gql`
  query search($term: String!) {
    search(term: $term) {
      __typename
      ... on Ressurs {
        navIdent
      }
      ... on Organisasjonsenhet {
        navn
        agressoId
        orgNiv
      }
    }
  }
`;

function SearchboxLine(
  props: {
    res: GqlRessurs | GqlOrganisasjonsenhet;
  },
  ref?: any
): JSX.Element {
  const lineProps = {
    className: "Searchbox3__searchboxline",
    tabIndex: -1,
  };
  const lineLinkProps = {
    tabIndex: -1,
    ref: ref,
  };

  const { res } = props;
  const resType: "Org" | "Res" | undefined = (res as any).navn
    ? "Org"
    : (res as any).navIdent
    ? "Res"
    : undefined;

  switch (resType) {
    case "Res": {
      const ressurs = res as GqlRessurs;
      return (
        <div {...lineProps}>
          <Link {...lineLinkProps} to={"/medarbeidere/" + ressurs.navIdent}>
            Ressurs: {ressurs.visningsnavn || ressurs.navIdent}
          </Link>
        </div>
      );
    }

    case "Org": {
      const orgenhet = res as GqlOrganisasjonsenhet;
      return (
        <div {...lineProps}>
          <Link
            {...lineLinkProps}
            to={"/organisasjonsenheter/" + agressoIdDataToUrl(orgenhet.agressoId,orgenhet.orgNiv)}
          >
            Orgenhet: {orgenhet.navn}
          </Link>
        </div>
      );
    }

    default: {
      return <div {...lineProps}> ugyldig</div>;
    }
  }
}

const FwdSearchboxLine = React.forwardRef(SearchboxLine);

export function Searchbox3(props: any): JSX.Element {
  const [searchString, setSearchString] = React.useState("");
  const [dirtyStr, setDirtyStr] = React.useState("");
  const inputRef = React.useRef<HTMLInputElement>(null);
  const lineRefs = React.useRef<HTMLElement[]>([]);
  const [timeoutId, setTimeoutId] = React.useState<any>(null);
  const [focusedIdx, setFocusedIdx] = React.useState<number>(-1);
  const [requestShowDropdown, setRequestShowDropdown] = React.useState(false);

  const searchStringHasSufficientLength = searchString.length > 3;

  const qryRes = useQuery<
    { search: (GqlRessurs | GqlOrganisasjonsenhet)[] },
    { term: string }
  >(query, {
    variables: { term: searchString },
    skip: !searchStringHasSufficientLength || dirtyStr !== searchString,
  });

  // to restrict amount of network calls made
  React.useEffect(() => {
    clearTimeout(timeoutId);
    const timeoutIdTmp = setTimeout(() => {
      setFocusedIdx(-1);
      setSearchString(dirtyStr);
    }, 600);
    setTimeoutId(timeoutIdTmp);
  }, [dirtyStr]);

  // show dropdown on search update
  React.useEffect(() => {
    if (!searchStringHasSufficientLength) {
      setRequestShowDropdown(false);
    } else if (qryRes.data || qryRes.error || qryRes.loading) {
      setRequestShowDropdown(true);
    }
  }, [qryRes.data, qryRes.error, qryRes.loading, searchString]);

  const resultList =
    searchString === "" ? [] : qryRes.data ? qryRes.data.search : [];

  // fix focus
  React.useEffect(() => {
    if (focusedIdx === -1 || resultList.length === 0) {
      return;
    }

    if (lineRefs.current[focusedIdx]) {
      lineRefs.current[focusedIdx].focus();
    } else {
      console.error({ focusedIdx, lineRefs });
    }
  }, [focusedIdx, resultList.length]);

  // Request focus on first render
  React.useEffect(() => {
    inputRef.current?.focus();
  }, []);

  function handleBlur(ev: any): void {
    const focusOutsideSearchbox =
      // ev.relatedTarget && !ev.currentTarget.contains(ev.relatedTarget as any);
      !ev.currentTarget?.contains(ev.relatedTarget as any);
    if (focusOutsideSearchbox) {
      setRequestShowDropdown(false);
    }
  }

  const show = !qryRes.loading && !qryRes.error && requestShowDropdown;

  return (
    <div
      className="Searchbox3"
      onFocus={() => setRequestShowDropdown(searchStringHasSufficientLength)}
      onMouseLeave={() => setRequestShowDropdown(false)}
      onBlur={handleBlur}
      onKeyDownCapture={(ev) => {
        if ("ArrowDown" === ev.key) {
          setFocusedIdx(Math.min(focusedIdx + 1, resultList.length - 1));
          ev.preventDefault();
        } else if ("ArrowUp" === ev.key) {
          setFocusedIdx(Math.max(focusedIdx - 1, -1));
          ev.preventDefault();
        } else if ("Enter" === ev.key) {
          // enable following links with enter key
        } else if (focusedIdx !== -1) {
          setFocusedIdx(-1);
          (inputRef.current as any).focus();
        }
      }}
    >
      <div className="Searchbox3__bar">
        <input
          ref={inputRef}
          onChange={(ev) => setDirtyStr(ev.target.value)}
          onMouseEnter={(ev) => {
            if (document.activeElement === ev.target) {
              setRequestShowDropdown(searchStringHasSufficientLength);
            }
          }}
          placeholder="søk etter enheter og ressurser her..."
        />
        <div className="Searchbox3__label">
          Treff: {JSON.stringify((qryRes.data as any)?.search?.length ?? 0)}
        </div>
      </div>
      <div className="Searchbox3__dropdown__container">
        {requestShowDropdown && (
          <div className="Searchbox3__dropdown" tabIndex={-1}>
            {qryRes.loading && <span>Loading...</span>}
            <button
              tabIndex={-1}
              className="Searchbox__closebutton"
              onClick={() => setRequestShowDropdown(false)}
            >
              x
            </button>
            {show &&
              resultList.map((it: any, k) => (
                <FwdSearchboxLine
                  key={k}
                  res={it}
                  ref={(el) => {
                    lineRefs.current[k] = el as any;
                  }}
                ></FwdSearchboxLine>
              ))}
            {qryRes.error && <pre>{JSON.stringify(qryRes.error, null, 2)}</pre>}
          </div>
        )}
      </div>
    </div>
  );
}

export default Searchbox3;
