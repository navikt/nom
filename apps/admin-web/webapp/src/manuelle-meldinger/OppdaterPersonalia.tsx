import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Detail, Modal, TextField } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
import type { HTTPError } from "ky";
import ky from "ky";
import queryString from "query-string";
import { useRef } from "react";
import { FormProvider, useForm } from "react-hook-form";
import { z } from "zod";

import { API_BASE_URL } from "~/api/apiQueries.ts";
import {
  ConfirmTextField,
  confirmTextValidator,
} from "~/manuelle-meldinger/felles/ConfirmTextField.tsx";
import {
  FidTextField,
  fidValidator,
} from "~/manuelle-meldinger/felles/FidTextField.tsx";
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  fid: fidValidator(),
  fornavn: z.string().min(1, "Må oppgis"),
  mellomnavn: z.string().optional(),
  etternavn: z.string().optional(),
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function OppdaterPersonalia() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      confirm: "",
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) =>
      (
        await ky.post(
          `${API_BASE_URL}/api/admin/manuelleEvtsrcMeldinger/oppdaterPersonaliaForRessurs?${queryString.stringify(v)}`,
        )
      ).text(),
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Oppdater personalia
      </Trigger>

      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Oppdater personalia" }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <FidTextField />
              <TextField
                error={methods.formState.errors.fornavn?.message}
                label="Fornavn"
                {...methods.register("fornavn")}
              />
              <TextField
                description="Valgfri"
                error={methods.formState.errors.mellomnavn?.message}
                label="Mellomnavn"
                {...methods.register("mellomnavn")}
              />
              <TextField
                description="Valgfri"
                error={methods.formState.errors.etternavn?.message}
                label="Etternavn"
                {...methods.register("etternavn")}
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
                        <Link
                          search={{ filterText: methods.watch("fid") }}
                          to="/sinks/ressurs"
                        >
                          Trykk her for å se historikk
                        </Link>
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
