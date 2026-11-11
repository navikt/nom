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
import { IdentType, Tilknytningtype } from "~/types/api-models";
import { formatDisplayDate, formatDisplayFysiskBrukersted } from "~/utils";

export function ConfirmStep() {
  const navigate = useNavigate({ from: "/ressurs/create/$step" });

  const { gyldigFom, gyldigTom, navident, ...requestFields } = useSearch({
    from: "/ressurs/create/$step",
  });

  const request: OpprettRessursRequest = {
    ...requestFields,
    startDato: gyldigFom,
    sluttDato: gyldigTom,
    identType: IdentType.STANDARD,
  };

  const formattedName = [
    request.visningsfornavn,
    request.visningsmellomnavn,
    request.visningsetternavn,
  ]
    .filter((navn) => !!navn)
    .join(" ");

  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(request.orgEnhetId),
    queryFn: () => getOrgEnhet.queryFn(request.orgEnhetId),
    enabled: !!request.orgEnhetId,
  });

  const selectOrgEnhet = orgEnhetQuery.data;

  const lederNavn = selectOrgEnhet?.leder?.visningsnavn;

  const selectedFysiskBrukersted = useFysiskBrukersted(
    request.fysiskBrukerstedNr,
  );

  const onClose = () =>
    navigate({
      to: "/ressurs/create/$step",
      params: { step: "3" },
      search: (oldSearch: any) => oldSearch,
    });

  const opprettRessursMutation = useMutation<
    AxiosResponse<OpprettRessursResponse>,
    AxiosError<Error>,
    void
  >({
    mutationFn: () => {
      return createRessurs(request);
    },

    onSuccess: (opprettRessursResponse) => {
      navigate({
        to: "/ressurs/create/$step",
        search: (old: any) => ({
          ...old,
          navident: opprettRessursResponse.data.navident,
        }),
        params: { step: "4" },
        replace: true,
      }).then();
    },
  });

  return (
    <Modal
      css={css`
        padding: var(--ax-space-16);
      `}
      header={{ heading: "Bekreft registrering" }}
      onClose={onClose}
      open
    >
      <Modal.Body
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--ax-space-16);
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

        <HGrid columns="max-content max-content" gap="space-8 space-8">
          <LabelWithValue label="Navn" split value={formattedName} />
          <LabelWithValue
            label="Telefonnummer"
            split
            value={request.primaryTelefonnummer}
          />
          <LabelWithValue
            label="Sektor"
            split
            value={SektorNameMapping[request.sektor]}
          />
          <LabelWithValue label="Nav-ident" split value={navident} />
          <LabelWithValue
            label="Tilknytningstype"
            split
            value={
              request.tilknytningtype === Tilknytningtype.INGEN
                ? undefined
                : TilknytningtypeNameMapping[request.tilknytningtype]
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
            value={formatDisplayDate(request.startDato)}
          />
          <LabelWithValue
            label="Sluttdato"
            split
            value={request.sluttDato && formatDisplayDate(request.sluttDato)}
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
