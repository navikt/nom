import { css } from "@emotion/css";
import { Alert, Button, Heading, Modal } from "@navikt/ds-react";
import { useState } from "react";

import type { OpprettRessursRequest } from "../../../api/generated/graphql_generated";
import { SektorNameMapping } from "./CreateResourceForm";

export function CreateResourceConfirmationModal({
  formValues,
  onConfirm,
  loading,
}: {
  formValues: OpprettRessursRequest;
  onConfirm: () => void;
  loading: boolean;
}) {
  const [showModal, setShowModal] = useState(false);
  const {
    visningsfornavn,
    visningsetternavn,
    visningsmellomnavn,
    sektor,
    startDato,
  } = formValues;

  const formattedName = [visningsfornavn, visningsmellomnavn, visningsetternavn]
    .filter((navn) => !!navn)
    .join(" ");

  return (
    <>
      <Button
        loading={loading}
        onClick={() => setShowModal(true)}
        type="button"
      >
        Opprett ressurs
      </Button>
      <Modal
        className={css({ minWidth: "500px", padding: "1rem" })}
        onClose={() => setShowModal(false)}
        open={showModal}
      >
        <Modal.Content>
          <Heading level="1" size="medium" spacing>
            Opprett ressurs
          </Heading>
          <div
            className={css`
              margin: var(--navds-spacing-7) 0;
              display: grid;
              grid-template-columns: max-content 1fr;
              column-gap: var(--navds-spacing-4);
              row-gap: var(--navds-spacing-2);
            `}
          >
            <span>Navn:</span>
            <b>{formattedName}</b>
            <span>Sektor:</span>
            <b>{SektorNameMapping[sektor]}</b>
            <span>Første arbeidsdag:</span>
            <b>{startDato.toLocaleDateString()}</b>
          </div>
          <Alert variant="warning">
            Du kan ikke endre opplysningene etter at ressursen er opprettet
          </Alert>
          <div
            className={css({
              display: "flex",
              alignItems: "center",
              marginTop: "2rem",
            })}
          >
            <Button
              className={css({ marginRight: "1rem" })}
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
