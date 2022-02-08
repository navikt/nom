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

async function doFetchGet(
  signal: AbortSignal,
  setOut: CallableFunction
): Promise<void> {
  try {
    const apiGetres = await doWrappedFetch(
      "/api/v1/me",
      signal,
      fetchConfig.GET_SIMPLE
    );
    const httpOkRes = await handleHttpResponse(apiGetres);
    const json = await validateJson(httpOkRes);
    const txt = json.name;

    setOut(lookups.createResolved(txt));
  } catch (err) {
    if (err instanceof DOMException) {
      return; // abortController
    }
    console.error(err);
    setOut(lookups.createError((err as any).message));
  }
}

const pendingState = {
  lookupState: "PENDING",
} as LookupState;

export default function useFetchLoggedInName(): [LookupState, () => void] {
  const [out, setOut] = useState<LookupState>(pendingState);
  const [toggle, flipToggle] = useReducer((toggle: boolean) => !toggle, false);

  React.useEffect(() => {
    setOut(pendingState);
    const abortController = new AbortController();
    const signal = abortController.signal;

    doFetchGet(signal, setOut);
    return function (): void {
      abortController.abort();
    };
  }, [toggle]);

  return [out, flipToggle];
}
