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

  return (
    <ModalWrapper onCancel={handleCancel} onConfirm={handleConfirm}>
      {modal.content}
    </ModalWrapper>
  );
};

export default ModalComponent;
