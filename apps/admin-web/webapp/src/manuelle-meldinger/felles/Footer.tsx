import { PaperplaneIcon } from "@navikt/aksel-icons";
import { Button, Modal } from "@navikt/ds-react";

export function Footer({
  isPending,
  onClose,
}: {
  isPending: boolean;
  onClose: () => void;
}) {
  return (
    <Modal.Footer>
      <Button icon={<PaperplaneIcon />} loading={isPending} type="submit">
        Send
      </Button>
      <Button onClick={onClose} type="button" variant="secondary">
        Avbryt
      </Button>
    </Modal.Footer>
  );
}
