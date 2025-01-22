import { queryOptions } from "@tanstack/react-query";
import ky from "ky";

import type { OrgEnhetsType } from "~/types/nom-admin/nom-admin-models";
import type { U4Diff } from "~/types/nom-api/nom-api-admin-models.ts";
import type { OrgEnhet, Ressurs } from "~/types/nom-api/nom-api-models.ts";

export const API_BASE_URL = "/frackend/api";

export function getRessursOptions(navident: string) {
  return queryOptions({
    enabled: /^[A-Za-z]\d{6}$/.test(navident),
    queryKey: ["RESSURS", navident],
    retry: false,
    queryFn: async () =>
      (
        await ky.get(`${API_BASE_URL}/app/ressurser/byNavident/${navident}`)
      ).json<Ressurs>(),
  });
}

export async function lastOrgenheter() {
  return ky.get(`${API_BASE_URL}/api/admin/dvhLast/loadOrgEnheter`);
}

export async function lastRessurser() {
  return ky.get(`${API_BASE_URL}/api/admin/dvhLast/loadRessurser`);
}

export async function lastOrganiseringerOgLedere() {
  return ky.get(
    `${API_BASE_URL}/api/admin/dvhLast/loadOrganiseringKoblingerOgLedere`,
  );
}

export async function lastOrgtilknytninger() {
  return ky.get(`${API_BASE_URL}/api/admin/dvhLast/loadOrgtilknytninger`);
}

export async function lastOrgtilknytningerForEksterne() {
  return ky.get(
    `${API_BASE_URL}/api/admin/dvhLast/loadOrgtilknytningerForEksterne`,
  );
}

export async function lastSkyggeOrgtilknytningerForKommunale() {
  return ky.get(
    `${API_BASE_URL}/api/admin/dvhLast/loadSkyggeOrgtilknytningerForKommunale`,
  );
}

export async function lastTyperForOrgenheter() {
  return ky.get(`${API_BASE_URL}/api/admin/dvhLast/resolveOrgEnhetsTyper`);
}

export async function reberegnNomnivaa() {
  return ky.get(`${API_BASE_URL}/api/admin/utlednomnivaaer`);
}

export async function oppdaterOrgEnhetType(data: {
  nomId: string | undefined;
  orgEnhetsType: OrgEnhetsType | undefined;
}) {
  return ky.post(`${API_BASE_URL}/app/orgenheter/orgEnhetsType`, {
    json: data,
  });
}

export function getOrgEnhetOptions(orgenhet: string) {
  return queryOptions({
    queryKey: ["ORGENHET", orgenhet],
    retry: false,
    queryFn: async () =>
      (
        await ky.get(`${API_BASE_URL}/app/orgenheter/${orgenhet}`)
      ).json<OrgEnhet>(),
  });
}

export async function identerUtenStillingIU4Options(): Promise<U4Diff[]> {
  return await ky
    .get(`${API_BASE_URL}/api/admin/u4diff/identerUtenStillingIU4`, {
      timeout: 60_000,
    })
    .json<U4Diff[]>();
}

export async function identerUtenStillingIU4ExcelExportOptions() {
  const blob = await ky
    .get(`${API_BASE_URL}/api/admin/u4diff/identerUtenStillingIU4/excel`, {
      timeout: 60_000,
    })
    .blob();
  const today = new Date().toISOString().split("T")[0];
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `Statlige_Identer_Uten_Stilling_I_U4_${today}.xlsx`;
  document.body.append(a);
  a.click();
  a.remove();
  window.URL.revokeObjectURL(url);
}
