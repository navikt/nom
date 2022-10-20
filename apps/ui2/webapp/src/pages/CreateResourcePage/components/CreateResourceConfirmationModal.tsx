import { css } from "@emotion/css";
import { BodyShort, Button, Heading, Label, Modal } from "@navikt/ds-react";
import { useState } from "react";

import type { OpprettRessursFormTypes } from "./CreateResourceForm";

const TextWithLabelHorizontal = (properties: {
  label: string;
  text: string;
}) => {
  return (
    <div
      className={css({
        display: "flex",
        alignContent: "center",
        marginTop: "0.7rem",
      })}
    >
      <BodyShort>{properties.label}</BodyShort>&nbsp;
      <Label>{properties.text}</Label>
    </div>
  );
};

const CreateResourceConfirmationModal = ({
  formValues,
  onConfirm,
  loading,
}: {
  formValues: OpprettRessursFormTypes;
  onConfirm: () => void;
  loading: boolean;
}) => {
  const [showModal, setShowModal] = useState(false);
  const { fornavn, etternavn, mellomnavn } = formValues;

  const formattedName = [fornavn, mellomnavn, etternavn]
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
          <BodyShort className={css({ marginBottom: "1rem" })}>
            Du er nå i ferd med å opprette ressurs:{" "}
          </BodyShort>
          <TextWithLabelHorizontal label="Navn: " text={formattedName} />
          <TextWithLabelHorizontal label="Org.enhet: " text="Mangler enhet" />
          <TextWithLabelHorizontal label="Sektor: " text="Statlig" />

          <BodyShort className={css({ marginTop: "2rem" })}>
            Du kan ikke endre opplysningene etter at ressursen er opprettet.
          </BodyShort>

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
};

export default CreateResourceConfirmationModal;
