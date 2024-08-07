import type { ReactNode } from "react";
import React, { forwardRef } from "react";

interface ModalWrapperProperties {
  children: ReactNode;
  onConfirm: () => void;
  onCancel: () => void;
}

const ModalWrapper = forwardRef<HTMLDivElement, ModalWrapperProperties>(
  ({ children, onConfirm, onCancel }, reference) => (
    <div ref={reference}>
      {React.cloneElement(children as React.ReactElement, {
        onConfirm,
        onCancel,
      })}
    </div>
  ),
);

export default ModalWrapper;
