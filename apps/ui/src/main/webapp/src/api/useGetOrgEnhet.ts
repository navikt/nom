import { OrgEnhetData } from "../common/types";
import React, { useReducer, useState } from "react";
import {
  handleHttpResponse,
  startApiGet,
  validateJson,
  API_URL,
} from "./utils_fetch_api";
import { lookups, LookupState } from "./utils_api_hook";

interface ApiRes {
  enhetsNr: number;
  navn: string;
  enhetsType: string;
  gyldigFra: string;
  gyldigTil: string;
  sti: { navn: string; enhetsNr: number }[];
  underEnheter: { navn: string; enhetsNr: number }[];
}

function translateFromApi(apiRes: ApiRes): OrgEnhetData {
  const out: OrgEnhetData = {
    data: {
      enhetNavn: apiRes.navn,
      enhetType: apiRes.enhetsType,
      enhetNr: "" + apiRes.enhetsNr,
    },
    koblinger: {
      sti: apiRes.sti.map((it) => {
        return {
          enhetNr: "" + it.enhetsNr,
          enhetType: "NAVENHET",
          enhetNavn: it.navn,
        };
      }),
      underEnheter: apiRes.underEnheter.map((it) => {
        return {
          enhetNr: "" + it.enhetsNr,
          enhetType: "NAVENHET",
          enhetNavn: it.navn,
        };
      }),
    },
  };

  return out;
}

async function doFetch(
  id: string | null,
  signal: AbortSignal,
  setOut: CallableFunction
): Promise<void> {
  try {
    const apiGetres = await startApiGet(
      API_URL + "/api/v1/enhet/" + id,
      signal
    );
    const httpOkRes = await handleHttpResponse(apiGetres);
    const jsonOkRes = await validateJson(httpOkRes);
    // const out: OrgEnhetData = jsonOkRes as any;
    const tmp: ApiRes = jsonOkRes as any;
    const out: OrgEnhetData = translateFromApi(tmp);
    setOut(lookups.createResolved(out));
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
} as LookupState<OrgEnhetData>;

export default function useGetOrgEnhet(
  id: string | null
): [LookupState<OrgEnhetData>, () => void] {
  const [out, setOut] = useState<LookupState<OrgEnhetData>>(pendingState);
  const [toggle, flipToggle] = useReducer((toggle: boolean) => !toggle, false);
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
