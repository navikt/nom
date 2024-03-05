import { Button, Modal } from "@navikt/ds-react";
import { useRef } from "react";
import { JsonView } from "react-json-view-lite";

export function JsonModal({ data, text }: { data: object; text: string }) {
  const reference = useRef<HTMLDialogElement>(null);

  return (
    <>
      <Button
        onClick={() => reference.current?.showModal()}
        size="small"
        variant="tertiary"
      >
        {text}
      </Button>
      <Modal
        closeOnBackdropClick
        header={{ heading: "Json data" }}
        ref={reference}
      >
        <Modal.Body>
          <JsonView data={data} />
        </Modal.Body>
      </Modal>
    </>
  );
}
