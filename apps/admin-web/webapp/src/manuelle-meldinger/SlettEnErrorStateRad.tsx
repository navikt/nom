import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Detail, Modal } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import type { HTTPError } from "ky";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { z } from "zod";

import { deleteErrorStateById } from "~/api/queries.ts";
import {
  FidTextField,
  fidValidator,
} from "~/manuelle-meldinger/felles/FidTextField.tsx";
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  fid: fidValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function SlettEnErrorStateRad() {
  const ref = useRef<HTMLDialogElement>(null);
  const methods = useForm<SchemaType>({
    defaultValues: {
      fid: "",
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) => (await deleteErrorStateById(v.fid)).text(),
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Slett ErrorState
      </Trigger>
      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Slett ErrorState" }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <p>Er du sikker på at du vil slette ErrorState?</p>
              <FidTextField />
              {(() => {
                switch (mutation.status) {
                  case "pending":
                  case "idle": {
                    return null;
                  }
                  case "success": {
                    return (
                      <Alert variant="success">
                        Meldingen ble sendt. <Detail>{mutation.data}</Detail>
                      </Alert>
                    );
                  }
                  case "error": {
                    return (
                      <Alert variant="error">{mutation.error.message}</Alert>
                    );
                  }
                }
              })()}
            </Modal.Body>
            <Footer
              isPending={mutation.isPending}
              onClose={() => ref.current?.close()}
            />
          </Modal>
        </form>
      </FormProvider>
    </>
  );
}
