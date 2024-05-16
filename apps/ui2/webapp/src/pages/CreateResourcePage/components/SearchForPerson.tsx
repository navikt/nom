import { Search } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { Controller, useForm } from "react-hook-form";

export function SearchForPerson() {
  const { personident } = useSearch({ from: "/ressurs/create/$step" });
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
          params: (previous) => previous,
          search: (oldSearch) => ({
            ...oldSearch,
            personident: values.personident,
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
            data-cy="fnr-search"
            error={error?.message}
            hideLabel={false}
            label="Fyll inn personnummer/D-nummer så finner vi personen i Folkeregisteret."
            onChange={(newValue) => onChange(newValue)}
            onClear={() => {
              reset({ personident: "" });
            }}
            value={value}
            variant="secondary"
          />
        )}
        rules={{
          validate: (value) =>
            value?.length === 11 || "Fødselsnummer må inneholde 11 siffer",
        }}
      />
    </form>
  );
}
