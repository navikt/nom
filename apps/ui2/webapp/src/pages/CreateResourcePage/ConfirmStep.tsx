import { css } from "@emotion/react";
import { Alert, Button, Heading, Modal } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError, AxiosResponse } from "axios";
import axios from "axios";
import { format } from "date-fns";
import { useMutation } from "react-query";

import { useGetOrgEnhetQuery } from "~/api/generated/graphql_generated";
import { ReactQueryErrorAlert } from "~/components/ReactQueryErrorAlert";
import { AnsettelsesTypeNameMapping } from "~/pages/CreateResourcePage/CreateResourcePageRoutes";
import { SektorNameMapping } from "~/pages/CreateResourcePage/SektorStep";
import { createRessursRoute } from "~/reactRouterRoutes";
import type { OpprettRessursRequest, OpprettRessursResponse } from "~/types";
import { DATE_FORMATS } from "~/utils";

export function ConfirmStep() {
  const navigate = useNavigate({ from: createRessursRoute.id });
  const {
    personident,
    visningsfornavn,
    visningsetternavn,
    visningsmellomnavn,
    sektor,
    ansettelsesType,
    startDato,
    sluttDato,
    orgEnhetId,
  } = useSearch({ from: createRessursRoute.id });

  const formattedName = [visningsfornavn, visningsmellomnavn, visningsetternavn]
    .filter((navn) => !!navn)
    .join(" ");

  const orgenheterQuery = useGetOrgEnhetQuery({
    variables: {
      id: orgEnhetId || "",
    },
  });

  const selectOrgEnhet = orgenheterQuery.data?.orgEnhet;
  const lederNavn = selectOrgEnhet?.leder[0]?.ressurs.visningsnavn;

  const onClose = () =>
    navigate({
      // eslint-disable-next-line @typescript-eslint/ban-ts-comment
      // @ts-ignore https://github.com/TanStack/router/issues/730
      search: (o) => o,
      params: { step: "3" },
    });

  const opprettRessursMutation = useMutation<
    AxiosResponse<OpprettRessursResponse>,
    AxiosError<Error>,
    void
  >(
    () => {
      // TODO: better type safety here
      const opprettRessursRequest: Partial<OpprettRessursRequest> = {
        personident,
        visningsfornavn,
        visningsetternavn,
        visningsmellomnavn,
        sektor,
        ansettelsesType,
        startDato,
        sluttDato,
        orgEnhetId,
      };
      return axios.post("/nom-api/app/ressurs", opprettRessursRequest);
    },
    {
      onSuccess: (opprettRessursResponse) => {
        // Because ansettelsesType is not yet persisted in NOM we must retrieve it from the form.
        navigate({
          to: createRessursRoute.to,
          search: (old) => ({
            ...old,
            navident: opprettRessursResponse.data.navident,
            ansettelsesType,
          }),
          params: { step: "4" },
          replace: true,
        });
      },
    },
  );

  return (
    <Modal
      css={css`
        width: 600px;
        padding: var(--a-spacing-4);
      `}
      onClose={onClose}
      open
    >
      <Modal.Header>
        <Heading level="1" size="medium" spacing>
          Bekreft registrering
        </Heading>
      </Modal.Header>
      <Modal.Body
        css={css`
          display: flex;
          flex-direction: column;
          gap: var(--a-spacing-9);
        `}
      >
        <span>
          Ved å bekrefte disse detaljene oppretter du en NAV-ident, og personen
          blir registrert som medarbeider i NOM.
        </span>
        <div
          css={css`
            display: grid;
            grid-template-columns: max-content 1fr;
            gap: var(--a-spacing-4);
          `}
        >
          <span>Navn:</span>
          <b>{formattedName}</b>
          <span>Sektor:</span>
          <b>{sektor && SektorNameMapping[sektor]}</b>
          {ansettelsesType && (
            <>
              <span>Ansettelsestype:</span>
              <b>{AnsettelsesTypeNameMapping[ansettelsesType]}</b>
            </>
          )}
          <span>Org enhet:</span>
          <b>{selectOrgEnhet?.navn}</b>
          <span>Leder:</span>
          <b>{lederNavn}</b>
          <span>Remedy-enhet:</span>
          <b>{selectOrgEnhet?.remedyEnhetId}</b>
          <span>Startdato (første arbeidsdag):</span>
          <b>{startDato && format(new Date(startDato), DATE_FORMATS.no)}</b>
          {sluttDato && (
            <>
              <span>Sluttdato:</span>
              <b>{format(new Date(sluttDato), DATE_FORMATS.no)}</b>
            </>
          )}
        </div>
        <Alert variant="info">
          Du kan ikke endre opplysningene i NOM når ressursen er opprettet.
          Dette må gjøres i Unit4 og Identrutina i Remedy.
        </Alert>
        <ReactQueryErrorAlert error={opprettRessursMutation.error} />
      </Modal.Body>
      <Modal.Footer>
        <Button
          loading={opprettRessursMutation.isLoading}
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
