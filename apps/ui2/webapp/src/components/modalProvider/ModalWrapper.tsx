import type { ReactElement } from "react";
import React, { forwardRef } from "react";

interface ModalChildProperties {
  onConfirm: () => void;
  onCancel: () => void;
}

interface ModalWrapperProperties {
  children: ReactElement<ModalChildProperties>;
  onConfirm: () => void;
  onCancel: () => void;
}

const ModalWrapper = forwardRef<HTMLDivElement, ModalWrapperProperties>(
  ({ children, onConfirm, onCancel }, reference) => (
    <div ref={reference}>
      {React.cloneElement(children, {
        onConfirm,
        onCancel,
      })}
    </div>
  ),
);

export default ModalWrapper;
