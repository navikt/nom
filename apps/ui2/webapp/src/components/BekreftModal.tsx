import { css } from "@emotion/react";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Button, Modal } from "@navikt/ds-react";
import type { ReactNode } from "react";
import React from "react";

interface BekreftModalProperties {
  onCancel?: () => void;
  onConfirm?: () => void;
  heading: string;
  content: ReactNode;
  confirmButtonLabel?: string;
  cancelButtonLabel?: string;
  confirmOnly?: boolean;
}

export function BekreftModal({
  onCancel,
  onConfirm,
  heading,
  content,
  confirmButtonLabel,
  cancelButtonLabel,
  confirmOnly,
}: BekreftModalProperties) {
  return (
    <Modal
      css={css`
        padding: var(--a-spacing-6);
        border-radius: 0;
      `}
      header={{ heading: heading, closeButton: false }}
      onClose={onCancel}
      open
      width="548px"
    >
      <Modal.Body>{content}</Modal.Body>
      <Modal.Footer>
        <Button
          icon={<FloppydiskIcon aria-hidden />}
          onClick={onConfirm}
          size="small"
        >
          {confirmButtonLabel ?? "Bekreft"}
        </Button>
        {!confirmOnly && (
          <Button onClick={onCancel} size="small" variant="secondary">
            {cancelButtonLabel ?? "Avbryt"}
          </Button>
        )}
      </Modal.Footer>
    </Modal>
  );
}
