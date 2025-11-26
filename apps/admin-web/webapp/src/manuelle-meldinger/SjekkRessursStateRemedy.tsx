import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Checkbox, Detail, Modal } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import type { HTTPError } from "ky";
import ky from "ky";
import { useRef } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { z } from "zod";

import { NOM_REMEDY_INTEGRASJON_BASE_URL } from "~/api/queries.ts";
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import {
  NavidentTextField,
  navidentValidator,
} from "~/manuelle-meldinger/felles/NavidentTextField.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  navident: navidentValidator(),
  isAdmin: z.boolean(),
});

type SchemaType = z.infer<typeof schema>;

export function SjekkRessursStateRemedy() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      navident: "",
      isAdmin: false,
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) =>
      (
        await ky.post(
          `${NOM_REMEDY_INTEGRASJON_BASE_URL}/admin/lesOgEksekverRessursState/${v.navident}`,
          {
            headers: {
              "Content-Type": "application/json",
            },
            searchParams: { isAdmin: v.isAdmin.toString() },
          },
        )
      ).text(),
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Send nyeste state til Remedy
      </Trigger>
      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Send nyeste state til Remedy" }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <NavidentTextField />
              <Controller
                control={methods.control}
                name="isAdmin"
                render={({ field }) => (
                  <Checkbox
                    checked={field.value}
                    onChange={(e) => field.onChange(e.target.checked)}
                  >
                    Bekrefter at jeg sender duplikat state
                  </Checkbox>
                )}
              />
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
