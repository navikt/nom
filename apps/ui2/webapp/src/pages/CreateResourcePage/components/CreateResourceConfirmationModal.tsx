import { css } from "@emotion/react";
import { Alert, Button, Heading, Modal } from "@navikt/ds-react";
import { format } from "date-fns";
import { useState } from "react";
import { useFormContext } from "react-hook-form";

import type { OrgEnhet } from "~/api/generated/graphql_generated";
import { DATE_FORMATS } from "~/utils";

import type { FormValues } from "./CreateResourceForm";
import {
  AnsettelsesTypeNameMapping,
  SektorNameMapping,
} from "./CreateResourceForm";

export function CreateResourceConfirmationModal({
  formValues,
  onConfirm,
  loading,
  orgEnheter,
}: {
  orgEnheter: OrgEnhet[];
  formValues: Partial<FormValues>;
  onConfirm: () => void;
  loading: boolean;
}) {
  const { trigger } = useFormContext();

  const [showModal, setShowModal] = useState(false);
  const {
    visningsfornavn,
    visningsetternavn,
    visningsmellomnavn,
    sektor,
    ansettelsesType,
    startDato,
    orgEnhetId,
  } = formValues;

  const formattedName = [visningsfornavn, visningsmellomnavn, visningsetternavn]
    .filter((navn) => !!navn)
    .join(" ");
  const selectOrgEnhet = orgEnheter.find(({ id }) => id === orgEnhetId);
  const lederNavn = selectOrgEnhet?.leder[0]?.ressurs.visningsnavn;

  return (
    <>
      <Button
        loading={loading}
        onClick={async () => {
          const validationSuccessful = await trigger();
          if (validationSuccessful) {
            setShowModal(true);
          }
        }}
        type="button"
      >
        Opprett medarbeider
      </Button>
      <Modal
        css={css`
          width: 600px;
          padding: var(--a-spacing-4);
        `}
        onClose={() => setShowModal(false)}
        open={showModal}
      >
        <Modal.Header>
          <Heading level="1" size="medium" spacing>
            Opprett medarbeider
          </Heading>
        </Modal.Header>
        <Modal.Body>
          <div
            css={css`
              margin: var(--a-spacing-7) 0;
              display: grid;
              grid-template-columns: max-content 1fr;
              column-gap: var(--a-spacing-4);
              row-gap: var(--a-spacing-2);
            `}
          >
            <span>Navn:</span>
            <b>{formattedName}</b>
            <span>Sektor:</span>
            <b>{sektor && SektorNameMapping[sektor]}</b>
            <span>Ansettelsestype:</span>
            <b>
              {ansettelsesType && AnsettelsesTypeNameMapping[ansettelsesType]}
            </b>
            <span>Org enhet:</span>
            <b>{selectOrgEnhet?.navn}</b>
            <span>Leder:</span>
            <b>{lederNavn}</b>
            <span>Enhetsnummer:</span>
            <b>{selectOrgEnhet?.remedyEnhetId}</b>
            <span>Første arbeidsdag:</span>
            <b>{startDato && format(new Date(startDato), DATE_FORMATS.no)}</b>
          </div>
          <Alert variant="info">
            Du kan ikke endre opplysningene i NOM når ressursen er opprettet.
            Dette må gjøres i Unit4 og Identrutina i Remedy.
          </Alert>
        </Modal.Body>
        <Modal.Footer>
          <Button
            onClick={() => {
              onConfirm();
              setShowModal(false);
            }}
            type="button"
          >
            Opprett medarbeider
          </Button>
          <Button
            onClick={() => setShowModal(false)}
            type="button"
            variant="secondary"
          >
            Avbryt
          </Button>
        </Modal.Footer>
      </Modal>
    </>
  );
}
