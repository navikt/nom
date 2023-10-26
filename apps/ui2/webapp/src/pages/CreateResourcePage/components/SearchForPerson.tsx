import { Search } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { encode } from "js-base64";
import { Controller, useForm } from "react-hook-form";

import { createRessursRoute } from "~/reactRouterRoutes";

export function SearchForPerson() {
  const { personident } = useSearch({ from: createRessursRoute.id });
  const { handleSubmit, control, reset } = useForm({
    defaultValues: {
      personident: personident,
    },
  });
  const navigate = useNavigate();

  return (
    <form
      onSubmit={handleSubmit((values) =>
        navigate({
          search: (oldSearch) => ({
            ...oldSearch,
            personident: encode(values.personident ?? ""),
          }),
        }),
      )}
    >
      <Controller
        control={control}
        name="personident"
        render={({ field: { onChange, value }, fieldState: { error } }) => (
          <Search
            autoComplete="off"
            error={error?.message}
            hideLabel={false}
            label="Fyll inn personnummer/D-nummer så finner vi personen i Folkeregisteret."
            onChange={(newValue) => onChange(newValue)}
            onClear={() => {
              reset({ personident: "" });
              navigate({ search: {} });
            }}
            type="number"
            value={value}
            variant="secondary"
          />
        )}
        rules={{
          validate: (value) => value?.length === 11 || "Må være 11 siffer",
        }}
      />
    </form>
  );
}
