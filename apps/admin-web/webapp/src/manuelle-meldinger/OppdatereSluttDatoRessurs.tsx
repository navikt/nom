import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Detail, Modal, TextField } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import { Link } from "@tanstack/react-router";
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
import { navidentValidator } from "~/manuelle-meldinger/felles/NavidentTextField.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  navident: navidentValidator(),
  sluttDato: z.string(),
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function OppdatereSluttDatoRessurs() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      confirm: "",
      sluttDato: "",
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) => {
      const formattedDate =  v.sluttDato && format(v.sluttDato, "yyyy-MM-dd");
      console.log("formattedDate" + formattedDate);

      return (
        await ky.post(
          `${API_BASE_URL}/api/admin/manuelleEvtsrcMeldinger/oppdatereSluttDatoRessurs?${queryString.stringify({ ...v, sluttDato: formattedDate })}`,
        )
      ).text();
    },
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Oppdater sluttdato
      </Trigger>

      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Oppdatter sluttdato" }}
            onClose={() => {
              methods.reset();
              methods.clearErrors();
              mutation.reset();
              ref.current?.close();
            }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <p>Dette sender ny Ressurs sluttdato oppdatert event</p>
              <TextField
                error={methods.formState.errors.navident?.message}
                label="Navident"
                {...methods.register("navident")}
              />
              <DatePickerWrapped
                label="Sluttdato på Ressurs"
                name="sluttDato"
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
                        Meldingen ble sendt.{" "}
                        <Detail>{JSON.parse(mutation.data).message}</Detail>
                        <Link
                          search={{ filterText: JSON.parse(mutation.data).fid }}
                          to="/sinks/ressurs"
                        >
                          Trykk her for å se historikk
                        </Link>
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
