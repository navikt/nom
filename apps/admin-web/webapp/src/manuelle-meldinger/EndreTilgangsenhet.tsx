import { zodResolver } from "@hookform/resolvers/zod";
import { Alert, Modal, TextField, UNSAFE_Combobox } from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import type { HTTPError } from "ky";
import ky from "ky";
import { useRef, useState } from "react";
import { Controller, FormProvider, useForm } from "react-hook-form";
import { z } from "zod";

import { API_BASE_URL } from "~/api/apiQueries.ts";
import {
  ConfirmTextField,
  confirmTextValidator,
} from "~/manuelle-meldinger/felles/ConfirmTextField.tsx";
import { Footer } from "~/manuelle-meldinger/felles/Footer.tsx";
import { Trigger } from "~/manuelle-meldinger/felles/Trigger.tsx";

type Tilgangsenhet = {
  enhetId: string;
  navn: string;
};

const schema = z.object({
  orgEnhetId: z.string().min(1, "Må oppgis"),
  tilgangsenhetId: z.string().min(1, "Må velges"),
  confirm: confirmTextValidator(),
});

type SchemaType = z.infer<typeof schema>;

export function EndreTilgangsenhet() {
  const ref = useRef<HTMLDialogElement>(null);
  const [searchTerm, setSearchTerm] = useState("");

  const methods = useForm<SchemaType>({
    defaultValues: {
      confirm: "",
      orgEnhetId: "",
      tilgangsenhetId: "",
    },
    resolver: zodResolver(schema),
  });

  const tilgangsenheterQuery = useQuery({
    queryKey: ["TILGANGSENHETER"],
    queryFn: async () =>
      (await ky.get(`${API_BASE_URL}/app/tilgangsenhet`)).json<
        Tilgangsenhet[]
      >(),
  });

  const alleOptions = (tilgangsenheterQuery.data ?? []).map(
    (tilgangsenhet) => ({
      label: `${tilgangsenhet.navn} (${tilgangsenhet.enhetId})`,
      value: tilgangsenhet.enhetId,
    }),
  );

  const filteredOptions = alleOptions.filter((option) =>
    option.label.toLowerCase().includes(searchTerm.toLowerCase()),
  );

  const mutation = useMutation<void, HTTPError, SchemaType>({
    mutationFn: async (v) => {
      await ky.patch(`${API_BASE_URL}/api/admin/tilgangsenhetednringer`, {
        json: {
          orgEnhetId: v.orgEnhetId,
          tilgangsenhetId: v.tilgangsenhetId,
        },
      });
    },
  });

  return (
    <>
      <Trigger onClick={() => ref.current?.showModal()}>
        Endre tilgangsenhet
      </Trigger>

      <FormProvider {...methods}>
        <form
          autoComplete="off"
          onSubmit={methods.handleSubmit((v) => mutation.mutate(v))}
        >
          <Modal
            className="w-fit"
            header={{ heading: "Endre tilgangsenhet" }}
            onClose={() => {
              methods.reset();
              methods.clearErrors();
              mutation.reset();
              setSearchTerm("");
              ref.current?.close();
            }}
            ref={ref}
          >
            <Modal.Body className="flex gap-4 flex-col">
              <p>
                Setter tilgangsenhet på en orgenhet. Velg en gyldig
                tilgangsenhet fra listen.
              </p>
              <TextField
                error={methods.formState.errors.orgEnhetId?.message}
                label="Orgenhet ID"
                {...methods.register("orgEnhetId")}
              />
              <Controller
                control={methods.control}
                name="tilgangsenhetId"
                render={({ field, fieldState }) => {
                  const selectedOption = alleOptions.find(
                    (option) => option.value === field.value,
                  );

                  return (
                    <UNSAFE_Combobox
                      error={fieldState.error?.message}
                      filteredOptions={filteredOptions}
                      id="tilgangsenhet-velger"
                      isLoading={tilgangsenheterQuery.isLoading}
                      label="Tilgangsenhet"
                      onChange={(value) => setSearchTerm(value ?? "")}
                      onToggleSelected={(value, isSelected) => {
                        field.onChange(isSelected ? value : "");
                      }}
                      options={alleOptions}
                      selectedOptions={selectedOption ? [selectedOption] : []}
                    />
                  );
                }}
              />
              <ConfirmTextField />
              {tilgangsenheterQuery.isError ? (
                <Alert variant="warning">
                  Kunne ikke hente listen over gyldige tilgangsenheter.
                </Alert>
              ) : null}
              {(() => {
                switch (mutation.status) {
                  case "idle":
                  case "pending": {
                    return null;
                  }
                  case "success": {
                    return (
                      <Alert variant="success">Meldingen ble sendt.</Alert>
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
