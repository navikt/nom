import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Detail, Modal, Select, TextField } from "@navikt/ds-react";
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
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import { navidentValidator } from "~/manuelle-meldinger/felles/NavidentTextField.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";
import { Sektor } from "~/types/nom-api/nom-api-models.ts";

const schema = z.object({
  navident: navidentValidator(),
  sektor: z.enum([Sektor.NAV_STATLIG, Sektor.NAV_KOMMUNAL, Sektor.EKSTERN], {
    message: "Obligatorisk",
  }),
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function OppdatereRessursSektor() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      confirm: "",
      sektor: null as unknown as undefined,
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) => {
      return (
        await ky.post(
          `${API_BASE_URL}/api/admin/manuelleEvtsrcMeldinger/endreSektorRessurs?${queryString.stringify({ ...v })}`,
        )
      ).text();
    },
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Oppdater sektor
      </Trigger>

      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Oppdatter sektor" }}
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
                Dette sender ny event for å oppdatere sektoren til en ressurs
              </p>
              <TextField
                error={methods.formState.errors.navident?.message}
                label="Navident"
                {...methods.register("navident")}
              />
              <Select
                label="Ny sektor"
                {...methods.register("sektor")}
                error={methods.formState.errors.sektor?.message}
              >
                <option disabled value="">
                  Velg en sektor
                </option>
                {Object.values(Sektor).map((sektor) => (
                  <option key={sektor} value={sektor}>
                    {sektor}
                  </option>
                ))}
              </Select>
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
