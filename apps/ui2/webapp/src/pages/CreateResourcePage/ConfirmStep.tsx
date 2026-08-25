import { css } from "@emotion/react";
import { Button, HGrid, Modal } from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError, AxiosResponse } from "axios";

import { createRessurs, getOrgEnhet } from "~/api/react-query-endpoints";
import { ApiResponseError } from "~/components/ApiResponseError";
import { LabelWithValue } from "~/components/LabelWithValue";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import {
  SektorNameMapping,
  TilknytningtypeNameMapping,
} from "~/pages/CreateResourcePage/SektorStep";
import type {
  OpprettRessursRequest,
  OpprettRessursResponse,
} from "~/types/api-models";
import {
  AnsettelsesType, IdentType,
  Tilknytningtype,
} from "~/types/api-models";
import { formatDisplayDate, formatDisplayFysiskBrukersted } from "~/utils";

export function ConfirmStep() {
  const navigate = useNavigate({ from: "/ressurs/create/$step" });
  const {
    personident,
    navident,
    visningsfornavn,
    visningsetternavn,
    visningsmellomnavn,
    sektor,
    ansettelsesType,
    gyldigFom,
    gyldigTom,
    orgEnhetId,
    fysiskBrukerstedNr,
    primaryTelefonnummer,
    tilknytningtype,
  } = useSearch({ from: "/ressurs/create/$step" });

  const formattedName = [visningsfornavn, visningsmellomnavn, visningsetternavn]
    .filter((navn) => !!navn)
    .join(" ");

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId as string),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId as string),
    enabled: !!orgEnhetId,
  });

  const selectOrgEnhet = orgEnhetQuery.data;

  const lederNavn = selectOrgEnhet?.leder?.visningsnavn;

  const selectedFysiskBrukersted = useFysiskBrukersted(
    fysiskBrukerstedNr ?? "",
  );

  const onClose = () =>
    navigate({
      to: "/ressurs/create/$step",
      params: { step: "3" },
      search: (oldSearch) => oldSearch,
    });

  const opprettRessursMutation = useMutation<
    AxiosResponse<OpprettRessursResponse>,
    AxiosError<Error>,
    void
  >({
    mutationFn: () => {
      const opprettRessursRequest = {
        personident,
        visningsfornavn,
        visningsetternavn,
        visningsmellomnavn,
        sektor,
        ansettelsesType,
        startDato: gyldigFom,
        sluttDato: gyldigTom,
        orgEnhetId,
        primaryTelefonnummer,
        fysiskBrukerstedNr,
        tilknytningtype,
        identType: IdentType.STANDARD,
      };

      // TODO: better type safety here.
      // We assume that each step has verified that these values exist.
      // Using a type assertion here is cheating, ideally we would use something like zod/yup to parse and validate the data instead.
      return createRessurs(opprettRessursRequest as OpprettRessursRequest);
    },

    onSuccess: (opprettRessursResponse) => {
      // Because ansettelsesType is not yet persisted in NOM we must retrieve it from the form.
      navigate({
        to: "/ressurs/create/$step",
        search: (old) => ({
          ...old,
          navident: opprettRessursResponse.data.navident,
          ansettelsesType,
        }),
        params: { step: "4" },
        replace: true,
      }).then();
    },
  });

  return (
    <Modal
      css={css`
        padding: var(--a-spacing-4);
      `}
      header={{ heading: "Bekreft registrering" }}
      onClose={onClose}
      open
    >
      <Modal.Body
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-9);
        `}
      >
        <span
          css={css`
            max-width: 400px;
          `}
        >
          {`Ved å bekrefte disse detaljene vil personen bli registrert som
            medarbeider i NOM.${!!navident ? " Nav-ident blir videreført fra tidligere arbeidsforhold." : ""}`}
        </span>

        <HGrid columns="max-content max-content" gap="2 2">
          <LabelWithValue label="Navn" split value={formattedName} />
          <LabelWithValue
            label="Telefonnummer"
            split
            value={primaryTelefonnummer}
          />
          <LabelWithValue
            label="Sektor"
            split
            value={sektor && SektorNameMapping[sektor]}
          />
          <LabelWithValue label="Nav-ident" split value={navident} />
          <LabelWithValue
            label="Tilknytningstype"
            split
            value={
              tilknytningtype &&
              (tilknytningtype === Tilknytningtype.INGEN
                ? undefined
                : TilknytningtypeNameMapping[tilknytningtype])
            }
          />
          <LabelWithValue
            label="Ansettelsestype"
            split
            value={
              ansettelsesType && AnsettelsesTypeNameMapping[ansettelsesType]
            }
          />
          <LabelWithValue
            label="Org.enhet"
            split
            value={selectOrgEnhet?.navn}
          />
          <LabelWithValue label="Leder" split value={lederNavn} />
          <LabelWithValue
            label="Fysisk brukersted"
            split
            value={
              selectedFysiskBrukersted &&
              formatDisplayFysiskBrukersted(selectedFysiskBrukersted)
            }
          />
          <LabelWithValue
            label="Tilgangsenhet"
            split
            value={selectOrgEnhet?.tilgangsenhetId}
          />
          <LabelWithValue
            label="Startdato (første arbeidsdag)"
            split
            value={gyldigFom && formatDisplayDate(gyldigFom)}
          />
          <LabelWithValue
            label="Sluttdato"
            split
            value={gyldigTom && formatDisplayDate(gyldigTom)}
          />
        </HGrid>
        <ApiResponseError error={opprettRessursMutation.error} />
      </Modal.Body>

      <Modal.Footer>
        <Button
          loading={opprettRessursMutation.isPending}
          onClick={() => opprettRessursMutation.mutate()}
          type="button"
        >
          Opprett medarbeider
        </Button>
        <Button onClick={onClose} type="button" variant="secondary">
          Avbryt
        </Button>
      </Modal.Footer>
    </Modal>
  );
}

export const AnsettelsesTypeNameMapping = {
  [AnsettelsesType.FAST]: "Fast",
  [AnsettelsesType.MIDLERTIDIG]: "Midlertidig",
};
