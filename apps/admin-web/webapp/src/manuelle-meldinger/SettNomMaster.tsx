import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Modal, Radio, RadioGroup } from "@navikt/ds-react";
import { useMutation } from "@tanstack/react-query";
import type { HTTPError } from "ky";
import ky from "ky";
import queryString from "query-string";
import { useRef } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { z } from "zod";

import { API_BASE_URL } from "~/api/apiQueries.ts";
import {
  ConfirmTextField,
  confirmTextValidator,
} from "~/manuelle-meldinger/felles/ConfirmTextField.tsx";
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import {
  NavidentTextField,
  navidentValidator,
} from "~/manuelle-meldinger/felles/NavidentTextField.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

const schema = z.object({
  navident: navidentValidator(),
  action: z.enum(["MERK", "AVMERK"]),
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function SettNomMaster() {
  const ref = useRef<HTMLDialogElement>(null);

  const methods = useForm<SchemaType>({
    defaultValues: {
      navident: "",
      action: "MERK",
      confirm: "",
    },
    resolver: zodResolver(schema),
  });

  const mutation = useMutation<string, HTTPError, SchemaType>({
    mutationFn: async (v) => {
      const endpoint =
        v.action === "MERK"
          ? `${API_BASE_URL}/api/admin/manuelleEvtsrcMeldinger/merkNavidentSomMastret`
          : `${API_BASE_URL}/api/admin/manuelleEvtsrcMeldinger/avmerkNavidentSomMastret`;
      return (await ky.post(`${endpoint}?${queryString.stringify(v)}`)).text();
    },
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Merk Ressurs som NOM-mastret
      </Trigger>

      <FormProvider {...methods}>
        <form onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}>
          <Modal
            className="w-fit"
            header={{ heading: "Merk eller avmerk Ressurs som NOM-mastret" }}
            onClose={() => {
              methods.reset();
              methods.clearErrors();
              mutation.reset();
              ref.current?.close();
            }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <NavidentTextField />
              <Controller
                control={methods.control}
                name="action"
                render={({ field }) => (
                  <RadioGroup
                    defaultValue="MERK"
                    legend="Velg handling"
                    onChange={field.onChange}
                    value={field.value}
                  >
                    <Radio value="MERK">Merk som NOM-master</Radio>
                    <Radio value="AVMERK">Avmerk som NOM-master</Radio>
                  </RadioGroup>
                )}
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
                        Ressursen er nå markert som NOM-masteret
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
              onClose={() => {
                methods.reset();
                methods.clearErrors();
                mutation.reset();
                ref.current?.close();
              }}
            />
          </Modal>
        </form>
      </FormProvider>
    </>
  );
}
