import React, { useContext } from "react";

import { ModalContext } from "./ModalProvider";
import ModalWrapper from "./ModalWrapper";

const ModalComponent = () => {
  const context = useContext(ModalContext);

  if (!context || !context.modal) return null;

  const { modal, closeModal } = context;

  const handleConfirm = () => {
    modal.onConfirm();
    closeModal();
  };

  const handleCancel = () => {
    modal.onCancel();
    closeModal();
  };

  if (!React.isValidElement(modal.content)) {
    return null;
  }

  return (
    <ModalWrapper onCancel={handleCancel} onConfirm={handleConfirm}>
      {
        modal.content as React.ReactElement<{
          onConfirm: () => void;
          onCancel: () => void;
        }>
      }
    </ModalWrapper>
  );
};

export default ModalComponent;
