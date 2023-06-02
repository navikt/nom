import { css } from "@emotion/react";
import { Alert, Button, Heading, Modal } from "@navikt/ds-react";
import { format } from "date-fns";
import { useState } from "react";
import { useFormContext } from "react-hook-form";

import type { OrgEnhet } from "~/api/generated/graphql_generated";
import { DATE_FORMATS } from "~/utils";

import type { FormValues } from "./CreateResourceForm";
import { SektorNameMapping } from "./CreateResourceForm";

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
    startDato,
    orgEnhetId,
  } = formValues;

  const formattedName = [visningsfornavn, visningsmellomnavn, visningsetternavn]
    .filter((navn) => !!navn)
    .join(" ");
  const selectOrgEnhet = orgEnheter.find(({ id }) => id === orgEnhetId);

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
        Opprett ressurs
      </Button>
      <Modal
        css={css`
          width: 600px;
          padding: var(--a-spacing-4);
        `}
        onClose={() => setShowModal(false)}
        open={showModal}
      >
        <Modal.Content>
          <Heading level="1" size="medium" spacing>
            Opprett ressurs
          </Heading>
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
            <span>Org enhet:</span>
            <b>{selectOrgEnhet?.navn}</b>
            <span>Første arbeidsdag:</span>
            <b>{startDato && format(new Date(startDato), DATE_FORMATS.no)}</b>
          </div>
          <Alert variant="info">
            Du kan ikke endre opplysningene i NOM når ressursen er opprettet.
            Dette må gjøres i Unit4 og Identrutina i Remedy.
          </Alert>
          <div
            css={css`
              margin-top: var(--a-spacing-5);
              display: flex;
              gap: var(--a-spacing-4);

              button {
                flex: 1;
              }
            `}
          >
            <Button
              onClick={() => {
                onConfirm();
                setShowModal(false);
              }}
            >
              Opprett ressurs
            </Button>
            <Button onClick={() => setShowModal(false)} variant="secondary">
              Avbryt
            </Button>
          </div>
        </Modal.Content>
      </Modal>
    </>
  );
}
