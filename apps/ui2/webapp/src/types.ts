import type {
  AnsettelsesType,
  Sektor,
} from "~/api/generated/graphql_generated";

export type OpprettRessursRequest = {
  ansettelsesType: AnsettelsesType;
  orgEnhetId?: string;
  personident: string;
  sektor: Sektor;
  startDato?: string;
  visningsetternavn: string;
  visningsfornavn: string;
  visningsmellomnavn?: string;
};

/**  Responses */
export type OpprettRessursResponse = {
  __typename?: "OpprettRessursResponse";
  navident: string;
  personident: string;
  sektor: Sektor;
  visningsetternavn: string;
  visningsfornavn: string;
};
