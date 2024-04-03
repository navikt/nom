import { css } from "@emotion/react";
import { Button, Modal } from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError, AxiosResponse } from "axios";
import axios from "axios";
import { format } from "date-fns";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { ApiResponseError } from "~/components/ApiResponseError";
import { LabelWithValue } from "~/components/LabelWithValue";
import { useFysiskBrukersted } from "~/hooks/useFysiskBrukersted";
import { AnsettelsesTypeNameMapping } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import {
  SektorNameMapping,
  TilknytningtypeNameMapping,
} from "~/pages/CreateResourcePage/SektorStep";
import type {
  OpprettRessursRequest,
  OpprettRessursResponse,
} from "~/types/api-models";
import { DATE_FORMATS, formatVisningsnavn } from "~/utils";

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
  const lederNavn = formatVisningsnavn(selectOrgEnhet?.ledere[0]?.visningsnavn);

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
      // TODO: better type safety here
      const opprettRessursRequest: Partial<OpprettRessursRequest> = {
        personident,
        visningsfornavn,
        visningsetternavn,
        visningsmellomnavn,
        sektor,
        ansettelsesType,
        startDato: gyldigFom,
        sluttDato: gyldigTom,
        orgEnhetId,
        fysiskBrukerstedNr,
        tilknytningtype,
      };
      return axios.post(
        "/frackend/nom-api/app/ressurser",
        opprettRessursRequest,
      );
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
      });
    },
  });

  return (
    <Modal
      css={css`
        width: 600px;
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
        <div>
          <span>
            Ved å bekrefte disse detaljene vil personen blir registrert som
            medarbeider i NOM.{" "}
          </span>
          {navident && (
            <span>NAV-ident blir videreført fra tidligere arbeidsforhold.</span>
          )}
        </div>
        <div
          css={css`
            display: grid;
            grid-template-columns: max-content 1fr;
            gap: var(--a-spacing-4);
          `}
        >
          <LabelWithValue label="Navn" value={formattedName} />
          <LabelWithValue
            label="Sektor"
            value={sektor && SektorNameMapping[sektor]}
          />
          {navident && <LabelWithValue label="NAV-ident" value={navident} />}
          <LabelWithValue
            label="Tilknytningtype"
            value={
              tilknytningtype && TilknytningtypeNameMapping[tilknytningtype]
            }
          />
          <LabelWithValue
            label="Ansettelsestype"
            value={
              ansettelsesType && AnsettelsesTypeNameMapping[ansettelsesType]
            }
          />
          <LabelWithValue label="Org.enhet" value={selectOrgEnhet?.navn} />
          <LabelWithValue label="Leder" value={lederNavn} />

          <LabelWithValue
            label="Fysisk brukersted"
            value={
              selectedFysiskBrukersted &&
              `${selectedFysiskBrukersted.brukerstedNavn} (${selectedFysiskBrukersted.brukerstedNr})`
            }
          />
          <LabelWithValue
            label="Remedy-enhet"
            value={selectOrgEnhet?.remedyEnhetId}
          />
          <LabelWithValue
            label="Startdato (første arbeidsdag)"
            value={gyldigFom && format(new Date(gyldigFom), DATE_FORMATS.no)}
          />
          <LabelWithValue
            label="Sluttdato"
            value={gyldigTom && format(new Date(gyldigTom), DATE_FORMATS.no)}
          />
        </div>
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
