import { zodResolver } from "@hookform/resolvers/zod";
import { Button, Modal } from "@navikt/ds-react";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { z } from "zod";

import { deleteAllErrorStates } from "~/api/queries.ts";
import { confirmTextValidator } from "~/manuelle-meldinger/felles/ConfirmTextField.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function SlettAlleErrorStates() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      confirm: "",
    },
    resolver: zodResolver(schema),
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Slett alle ErrorStates
      </Trigger>
      <FormProvider {...methods}>
        <Modal
          className="w-fit"
          header={{ heading: "Slett alle error states" }}
          ref={ref}
        >
          <Modal.Body className="flex gap-4 flex-col">
            <p>Er du sikker på at du vil slette alle ErrorStates?</p>
            <Button onClick={() => ref.current?.close()} type="button">
              Avbryt
            </Button>
            <Button onClick={() => deleteAllErrorStates()} type="submit">
              Bekreft
            </Button>
          </Modal.Body>
        </Modal>
      </FormProvider>
    </>
  );
}
