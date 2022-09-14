import * as Gq from "../generated/graphql_generated";
import { Sektor } from "../generated/graphql_generated";
import { agressoIdDataToUrl } from "../../resources/utils/url_utils";
import { funcIsNotDuplicateByKey } from "../../resources/utils/utils";

export namespace ApiTypes {
  export type ApiRessurs = Pick<
    Gq.Ressurs,
    "navident" | "fornavn" | "etternavn"
  > &
    Pick<Gq.RessursLeder["ressurs"], "fornavn" | "navident" | "etternavn">;

  export type ApiEnhet = Pick<Gq.OrgEnhet, "orgNiv" | "agressoId" | "navn">;
}

export namespace Hierarchy {
  export type OrgEnhet = {
    name: string;
    ledere: Ressurs[];
    url: string;
  };

  export type Ressurs = {
    name: string;
    navident: string;

    url: string;
  };

  export type FocusedRessurs = {
    tel: string;
    email: string;
    sektor: "Statlig ansatt" | "Eksternt ansatt" | "Kommunalt ansatt";
    tilknytninger: {
      leder: Ressurs;
      orgenhet: Omit<OrgEnhet, "ledere">;
    }[];
  } & Ressurs;

  export type OrgHierarchy = {
    kind: "hierarchy";
    enhet: OrgEnhet;
    overEnheter: OrgEnhet[];
    underEnheter: OrgEnhet[];
    ressurser: Ressurs[];
  };

  export type OrgHierarchyWithFocusedRessurs = {
    kind: "hierarchy_focused_ressurs";
    overEnheter: OrgEnhet[];
    focusedRessurs: FocusedRessurs;
  };

  export type Err = {
    kind: "err";
    ctx?: any;
    msg: string;
  };
}

function nameFromRessurs(ressurs: ApiTypes.ApiRessurs) {
  if (ressurs.fornavn && ressurs.etternavn) {
    return `${ressurs.fornavn} ${ressurs.etternavn}`;
  }
  return "Uten navn";
}

function gqlEnhetToUrl(enhet: ApiTypes.ApiEnhet) {
  return (
    "/org/" + agressoIdDataToUrl(enhet?.agressoId, enhet?.orgNiv, enhet?.navn)
  );
}

function translateRessurs(
  ress: ApiTypes.ApiRessurs,
  enhetCtx: ApiTypes.ApiEnhet
): Hierarchy.Ressurs {
  const name =
    ress.fornavn && ress.etternavn
      ? `${ress.fornavn} ${ress.etternavn}`
      : "Uten navn";
  return {
    name,
    url: gqlEnhetToUrl(enhetCtx) + "/" + ress.navident,
    navident: ress.navident,
  };
}

function sektorToTxt(sektor: Gq.Sektor) {
  switch (sektor) {
    case Sektor.Ekstern:
      return "Eksternt ansatt";
    case Sektor.NavKommunal:
      return "Kommunalt ansatt";
    case Sektor.NavStatlig:
      return "Statlig ansatt";
  }
}

function formatTlf(nr: string) {
  const tmpNr = nr.startsWith("47") && nr.length === 10 ? nr.substring(2) : nr;
  const out = [];
  for (let k = 0; k < 8; k += 2) {
    out.push(tmpNr.substring(k, k + 2));
  }
  return out.join(" ");
}

export function focusedEnhetHierarchy(
  qryRes: Gq.HentHierarkiQueryResult
): Hierarchy.OrgHierarchy | Hierarchy.Err {
  if (!qryRes.data) {
    throw "Wrong usage focusedenhethierarchy";
  }

  const qryData = structuredClone(qryRes.data);

  if (!qryData.enhet) return { kind: "err", msg: "mangler enhet" };

  const flattenedOverenheter = (function DO() {
    const extractOverEnhet = () =>
      qryData.enhetsKoblinger?.overenheter[0]?.organisasjonsenhet;
    let currentOverenhet = extractOverEnhet();
    let out: NonNullable<ReturnType<typeof extractOverEnhet>>[] = [];
    while (currentOverenhet) {
      out = [currentOverenhet as any, ...out];
      currentOverenhet = currentOverenhet.organiseringer[0]
        ?.organisasjonsenhet as any;
    }
    return out;
  })();

  qryData.enhetsKoblinger?.underenheter.sort((a, b) =>
    (a?.organisasjonsenhet?.navn || "").localeCompare(
      b?.organisasjonsenhet?.navn || "",
      "no"
    )
  );
  qryData.ressursKoblinger?.koblinger.sort((a, b) =>
    (a.ressurs.fornavn || "").localeCompare(b.ressurs.fornavn || "", "no")
  );

  const focusedEnhetUrl = gqlEnhetToUrl(qryData.enhet);

  const underenheter = !qryData.enhetsKoblinger?.underenheter
    ? []
    : qryData.enhetsKoblinger.underenheter.filter(
        funcIsNotDuplicateByKey((it) => gqlEnhetToUrl(it.organisasjonsenhet))
      );

  const ressurser = !qryData.ressursKoblinger?.koblinger
    ? []
    : qryData.ressursKoblinger?.koblinger.filter(
        funcIsNotDuplicateByKey((it) => it.ressurs.navident)
      );

  const enhet = qryData.enhet;

  return {
    kind: "hierarchy",
    enhet: {
      name: enhet.navn || "Mangler enhets-navn",
      url: focusedEnhetUrl,
      ledere: enhet.leder.map((it) => ({
        name: nameFromRessurs(it.ressurs),
        navident: it.ressurs.navident,
        url: focusedEnhetUrl + "/" + it.ressurs.navident,
      })),
    },
    overEnheter: flattenedOverenheter.map((it) => ({
      name: it.navn,
      url: gqlEnhetToUrl(it),
      ledere: it.leder.map((it2) => translateRessurs(it2.ressurs, it)),
    })),
    underEnheter: underenheter.map((it) => ({
      name: it.organisasjonsenhet.navn,
      url: gqlEnhetToUrl(it.organisasjonsenhet),
      ledere: it.organisasjonsenhet.leder.map((it2) =>
        translateRessurs(it2.ressurs, it.organisasjonsenhet)
      ),
    })),
    ressurser: ressurser.map((it) => translateRessurs(it.ressurs, enhet)),
  };
}

export function focusedRessursHierarchy(
  orgdata: Hierarchy.OrgHierarchy,
  qryData: Gq.HentRessursQueryResult
): Hierarchy.OrgHierarchyWithFocusedRessurs | Hierarchy.Err {
  if (!qryData.data) {
    throw "Wrong usage focusedressurs";
  }
  const res = qryData.data.ressurs;
  if (!res) {
    return { kind: "err", msg: "null ressurs" };
  }

  const overEnheter = [...orgdata.overEnheter, orgdata.enhet];
  const tilknytninger: {
    leder: Hierarchy.Ressurs;
    orgenhet: Hierarchy.OrgEnhet;
  }[] = [];
  for (const l of res.ledere) {
    const closestOverenhet = overEnheter[overEnheter.length - 1];
    const isImplicitLederOfClosestOverenhet = overEnheter.some((it) =>
      it.ledere.some((it2) => it2.navident === l.ressurs.navident)
    );
    if (isImplicitLederOfClosestOverenhet) {
      const firstLederOrgtilknytning = l.ressurs.orgTilknytning[0].orgEnhet;
      tilknytninger.push({
        leder: {
          name: nameFromRessurs(l.ressurs),
          url:
            gqlEnhetToUrl(firstLederOrgtilknytning) + "/" + l.ressurs.navident,
          navident: l.ressurs.navident,
        },
        orgenhet: {
          name: closestOverenhet.name,
          url: closestOverenhet.url,
          ledere: [],
        },
      });
      // only list the leder once
      continue;
    }

    for (const o of l.ressurs.orgTilknytning.map((it) => it.orgEnhet)) {
      tilknytninger.push({
        leder: {
          name: nameFromRessurs(l.ressurs),
          url: gqlEnhetToUrl(o) + "/" + l.ressurs.navident,
          navident: l.ressurs.navident,
        },
        orgenhet: {
          name: o.navn,
          url: gqlEnhetToUrl(o),
          ledere: [],
        },
      });
    }
  }

  return {
    kind: "hierarchy_focused_ressurs",
    overEnheter: [...orgdata.overEnheter, orgdata.enhet],
    focusedRessurs: {
      sektor: sektorToTxt(res.sektor[0]),
      name: nameFromRessurs({ ...res }),
      tel:
        res.telefon.map((it) => formatTlf(it.nummer)).join(", ") ||
        "Mangler tlf",
      email: res.epost || "Mangler epost",
      navident: res.navident,
      url: orgdata.enhet.url + "/" + res.navident,
      tilknytninger: tilknytninger,
    },
  };
}
