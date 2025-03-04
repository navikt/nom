import { BodyShort, Box, Button, Modal } from "@navikt/ds-react";
import type { ReactNode } from "react";
import { createRoot } from "react-dom/client";

function dialogHolder() {
  let close: () => void;

  return {
    closeDialog: () => close(),
    showDialog: (node: ReactNode) => {
      const dialogElement = document.createElement("dialog", {});
      document.body.append(dialogElement);
      close = () => dialogElement.close();

      const root = createRoot(dialogElement);

      dialogElement.addEventListener("close", () => {
        root.unmount();
        dialogElement.remove();
      });

      root.render(node);
      dialogElement.showModal();
    },
  };
}

export function dynamicConfirmBeforeAction(
  onConfirm: () => void,
  prompt?: ReactNode,
) {
  const di = dialogHolder();
  const promptIsString = typeof prompt === "string";
  return di.showDialog(
    <Box padding={"1"}>
      {promptIsString ? <BodyShort>{prompt}</BodyShort> : prompt}
      <Modal.Footer>
        <Button autoFocus={true} onClick={di.closeDialog} variant={"secondary"}>
          Avbryt
        </Button>
        <Button
          onClick={() => {
            di.closeDialog();
            onConfirm();
          }}
        >
          Bekreft
        </Button>
      </Modal.Footer>
    </Box>,
  );
}

export function dynamicConfirmMessage(prompt: ReactNode) {
  const di = dialogHolder();
  const promptIsString = typeof prompt === "string";
  return di.showDialog(
    <Box padding={"1"}>
      {promptIsString ? <BodyShort>{prompt}</BodyShort> : prompt}
      <Modal.Footer>
        <Button autoFocus={true} onClick={di.closeDialog} variant={"secondary"}>
          Ok
        </Button>
      </Modal.Footer>
    </Box>,
  );
}

export async function copyToClipboard(jsonObject: unknown) {
  try {
    await window.navigator.clipboard.writeText(
      JSON.stringify(jsonObject, null, 2),
    );
    dynamicConfirmMessage(<BodyShort>JSON kopiert til clipboard</BodyShort>);
  } catch {
    dynamicConfirmMessage(
      <BodyShort>
        !feil!
        <br />
        JSON til clipboard feilet
      </BodyShort>,
    );
  }
}
