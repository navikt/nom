import React from "react";
import { useReducer, useState } from "react";
import { OrganisasjonsEnhet, Ressurs } from "../common/types";
import { lookups, LookupState } from "./utils_api_hook";
import {
  handleHttpResponse,
  performFetch as doWrappedFetch,
  validateGraphql,
  validateJson,
} from "./utils_fetch_api";
import fetchConfig from "./utils_fetch_configs";

function createQuery(id: string | null): string {
  const whereClause = id ? `where: {navIdent: "${id}"}` : "where: null";
  return `query {
        ressurs(${whereClause}) {
            navIdent
            personIdent
            koblinger {
              organisasjonsenhet{
                navn
                id
              }
            }
        }
    }`;
}

async function doFetch(
  id: string | null,
  signal: AbortSignal,
  setOut: CallableFunction
): Promise<void> {
  try {
    const apiGetres = await doWrappedFetch(
      fetchConfig.GRAPHQL_API_URL,
      signal,
      fetchConfig.POST_GRAPHQL,
      createQuery(id)
    );
    const httpOkRes = await handleHttpResponse(apiGetres);
    const jsonOkRes = await validateJson(httpOkRes);
    const graphqlOkRes = await validateGraphql(jsonOkRes);
    setOut(lookups.createResolved(graphqlOkRes.data.ressurs[0]));
  } catch (err) {
    if (err instanceof DOMException) {
      return; // abortController
    }
    console.error(err);
    setOut(lookups.createError(err.message));
  }
}

const pendingState = {
  lookupState: "PENDING",
} as LookupState<Ressurs>;

export default function useFetchRessurs(
  id: string | null
): [LookupState<Ressurs>, () => void] {
  const [out, setOut] = useState<LookupState<Ressurs>>(pendingState);
  const [toggle, flipToggle] = useReducer((toggle: boolean) => !toggle, false);
  if (!id) {
    return [lookups.createWaiting(), flipToggle];
  }

  React.useEffect(() => {
    if (id === null) {
      return;
    }
    setOut(pendingState);
    const abortController = new AbortController();
    const signal = abortController.signal;

    doFetch(id, signal, setOut);
    return function (): void {
      abortController.abort();
    };
  }, [id, toggle]);

  return [out, flipToggle];
}
