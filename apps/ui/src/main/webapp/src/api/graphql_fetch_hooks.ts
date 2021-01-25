import React from "react";
import { useReducer, useState } from "react";
import { OrganisasjonsEnhet, Ressurs } from "../common/types";
import {
  createOrganisasjonsenhetQuery,
  createRessursQuery,
} from "./graphql_queries";
import { lookups, LookupState } from "./utils_api_hook";
import {
  handleHttpResponse,
  performFetch as doWrappedFetch,
  validateGraphql,
  validateJson,
} from "./utils_fetch_api";
import fetchConfig from "./utils_fetch_configs";

//
//
// HOOK UTILS
//
//

async function doPostGraphql(
  signal: AbortSignal,
  setOut: CallableFunction,
  queryBody: string
): Promise<void> {
  try {
    const apiGetres = await doWrappedFetch(
      fetchConfig.GRAPHQL_API_URL,
      signal,
      fetchConfig.POST_GRAPHQL,
      queryBody
    );
    const httpOkRes = await handleHttpResponse(apiGetres);
    const jsonOkRes = await validateJson(httpOkRes);
    const graphqlOkRes = await validateGraphql(jsonOkRes);
    setOut(lookups.createResolved(graphqlOkRes.data));
  } catch (err) {
    if (err instanceof DOMException) {
      return; // abortController
    }
    console.error(err);
    setOut(lookups.createError(err.message));
  }
}

//
//
// HOOKS
//
//

// -- HOOK

export function useFetchRessurs(id: string | null): [LookupState, () => void] {
  const [out, setOut] = useState<LookupState>(lookups.createPending());
  const [toggle, flipToggle] = useReducer((toggle: boolean) => !toggle, false);
  if (!id) {
    return [lookups.createWaiting(), flipToggle];
  }

  React.useEffect(() => {
    if (id === null) {
      return;
    }
    setOut(lookups.createPending());
    const abortController = new AbortController();
    const signal = abortController.signal;

    doPostGraphql(signal, setOut, createRessursQuery(id.toUpperCase()));
    return function (): void {
      abortController.abort();
    };
  }, [id, toggle]);

  return [out, flipToggle];
}

// -- HOOK

export function useGetOrganisasjonsEnhet(
  id: string | null
): [LookupState, () => void] {
  const [out, setOut] = useState<LookupState>(lookups.createPending());
  const [toggle, flipToggle] = useReducer((toggle: boolean) => !toggle, false);

  React.useEffect(() => {
    if (id === null) {
      return;
    }
    setOut(lookups.createPending());
    const abortController = new AbortController();
    const signal = abortController.signal;

    doPostGraphql(signal, setOut, createOrganisasjonsenhetQuery(id));
    return function (): void {
      abortController.abort();
    };
  }, [id, toggle]);

  return [out, flipToggle];
}
