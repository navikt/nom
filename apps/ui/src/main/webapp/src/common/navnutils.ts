import { GqlNavn, GqlRessurs } from "./gqltypes";

export function fulltNavnForRessurs(ressurs: GqlRessurs): string {
  if (!ressurs.person) {
    return "[ukjent]";
  }
  const navn = ressurs.person.navn;
  const fnavn = navn.fornavn ?? "";
  const mnavn = navn.mellomnavn ? " " + navn.mellomnavn : "";
  const enavn = navn.etternavn ? " " + navn.etternavn : "";

  return fnavn + mnavn + enavn;
}

export function oppdeltNavnForRessurs(ressurs: GqlRessurs): GqlNavn {
  if (!ressurs.person) {
    return {
      fornavn: "[ukjent]",
      mellomnavn: "[ukjent]",
      etternavn: "[ukjent]",
    };
  }
  const navn = ressurs.person.navn;

  return {
    fornavn: navn.fornavn ?? "",
    mellomnavn: navn.mellomnavn ?? "",
    etternavn: navn.etternavn ?? "",
  };
}
