import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Detail, Modal, TextField } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import { format } from "date-fns";
import type { HTTPError } from "ky";
import ky from "ky";
import queryString from "query-string";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { z } from "zod";

import { API_BASE_URL } from "~/api/apiQueries.ts";
import { DatePickerWrapped } from "~/components/DatepickerWrapped.tsx";
import {
  ConfirmTextField,
  confirmTextValidator,
} from "~/manuelle-meldinger/felles/ConfirmTextField.tsx";
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  lederNavident: z
    .string({
      invalid_type_error: "Obligatorisk",
    })
    .min(1, "Obligatorisk"),
  delegertNavident: z
    .string({
      invalid_type_error: "Obligatorisk",
    })
    .min(1, "Obligatorisk"),
  orgenhetId: z
    .string({
      invalid_type_error: "Obligatorisk",
    })
    .min(1, "Obligatorisk"),
  gyldigTom: z.string().nullable(),
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function OpprettDelegering() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      lederNavident: "",
      delegertNavident: "",
      orgenhetId: "",
      gyldigTom: undefined,
      confirm: "",
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) => {
      const formattedDate = v.gyldigTom
        ? format(v.gyldigTom, "yyyy-MM-dd")
        : null;
      const params = { ...v };

      const formattedParams = {
        ...params,
        gyldigTom: formattedDate || undefined,
      };

      return (
        await ky.post(
          `${API_BASE_URL}/api/admin/manuelleEvtsrcMeldinger/opprettAuthorization?${queryString.stringify(formattedParams)}`,
        )
      ).text();
    },
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Opprett delegering
      </Trigger>

      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Opprett delegering" }}
            onClose={() => {
              methods.reset();
              methods.clearErrors();
              mutation.reset();
              ref.current?.close();
            }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <p>
                Dette sender ny authorize event for å opprette en delegering
              </p>
              <TextField
                error={methods.formState.errors.lederNavident?.message}
                label="Delegerende leder"
                {...methods.register("lederNavident")}
              />
              <TextField
                error={methods.formState.errors.delegertNavident?.message}
                label="Delegert til"
                {...methods.register("delegertNavident")}
              />
              <TextField
                error={methods.formState.errors.orgenhetId?.message}
                label="Org.-enhet"
                {...methods.register("orgenhetId")}
              />
              <DatePickerWrapped
                label="Gyldig til og med-dato på delegering"
                name="gyldigTom"
              />
              <ConfirmTextField />
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
                      <Alert variant="error">
                        Noe gikk galt. <Detail>{mutation.error.message}</Detail>
                      </Alert>
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
