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
import { fidValidator } from "~/manuelle-meldinger/felles/FidTextField.tsx";
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  fid: fidValidator(),
  nyGyldigTom: z.string().nullable(),
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function FornyDelegering() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      fid: "",
      nyGyldigTom: undefined,
      confirm: "",
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) => {
      const formattedDate = v.nyGyldigTom
        ? format(v.nyGyldigTom, "yyyy-MM-dd")
        : null;
      const params = { ...v };
      const formattedParams = {
        ...params,
        nyGyldigTom: formattedDate || undefined,
      };
      return (
        await ky.post(
          `${API_BASE_URL}/api/admin/manuelleEvtsrcMeldinger/fornyAuthorization?${queryString.stringify(formattedParams)}`,
        )
      ).text();
    },
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Forny delegering
      </Trigger>

      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Forny delegering" }}
            onClose={() => {
              methods.reset();
              methods.clearErrors();
              mutation.reset();
              ref.current?.close();
            }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <p>Dette sender ny authorize event for å oppdatere gyldigTom</p>
              <TextField
                error={methods.formState.errors.fid?.message}
                label="authorize-fid"
                {...methods.register("fid")}
              />
              <DatePickerWrapped
                label="Gyldig til og med-dato på delegering"
                name="nyGyldigTom"
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
