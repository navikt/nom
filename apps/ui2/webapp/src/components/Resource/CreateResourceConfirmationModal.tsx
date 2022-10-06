import { css } from "@emotion/css";
import { BodyShort, Button, Heading, Label, Modal } from "@navikt/ds-react";
import { useState } from "react";

const TextWithLabelHorizontal = (props: { label: string; text: string }) => {
  return (
    <div
      className={css({
        display: "flex",
        alignContent: "center",
        marginTop: "0.7rem",
      })}
    >
      <BodyShort>{props.label}</BodyShort>&nbsp;
      <Label>{props.text}</Label>
    </div>
  );
};

type ModalResourceProps = {
  formValues: any;
  onSubmit: (values: any) => void;
};

const CreateResourceConfirmationModal = (props: ModalResourceProps) => {
  const { formValues, onSubmit } = props;

  const [showModal, setShowModal] = useState(false);

  return (
    <>
      <Button type="button" onClick={() => setShowModal(true)}>
        Opprett ressurs
      </Button>
      <Modal
        open={showModal}
        onClose={() => setShowModal(false)}
        className={css({ minWidth: "500px", padding: "1rem" })}
      >
        <Modal.Content>
          <Heading spacing level="1" size="medium">
            Opprett ressurs
          </Heading>
          <BodyShort className={css({ marginBottom: "1rem" })}>
            Du er nå i ferd med å opprette ressurs:{" "}
          </BodyShort>
          <TextWithLabelHorizontal
            label="Navn: "
            text={
              formValues.visningsfornavn + " " + formValues.visningsetternavn ||
              "-"
            }
          />
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
                onSubmit(formValues);
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
