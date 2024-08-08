import type { ReactNode } from "react";
import React, { createContext, useCallback, useContext, useState } from "react";

interface ModalContextType {
  showModal: (content: ReactNode) => Promise<void>;
  closeModal: () => void;
  modal: {
    content: ReactNode;
    onConfirm: () => void;
    onCancel: () => void;
  } | null;
}

export const ModalContext = createContext<ModalContextType | null>(null);

export const ModalProvider = ({ children }: { children: ReactNode }) => {
  const [modal, setModal] = useState<{
    content: ReactNode;
    onConfirm: () => void;
    onCancel: () => void;
  } | null>(null);

  const showModal = useCallback((content: ReactNode) => {
    return new Promise<void>((resolve, reject) => {
      setModal({
        content,
        onConfirm: resolve,
        onCancel: reject,
      });
    });
  }, []);

  const closeModal = useCallback(() => {
    setModal(null);
  }, []);

  return (
    <ModalContext.Provider value={{ modal, showModal, closeModal }}>
      {children}
    </ModalContext.Provider>
  );
};

export const useModal = () => {
  const context = useContext(ModalContext);
  if (!context) {
    throw new Error("useModal must be used within a ModalProvider");
  }
  return context;
};
