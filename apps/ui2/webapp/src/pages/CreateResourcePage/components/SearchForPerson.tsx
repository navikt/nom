import { Search } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { Controller, useForm } from "react-hook-form";

import { Route } from "~/routes/ressurs.create.$step";

export function SearchForPerson() {
  const { personident } = useSearch({ from: "/ressurs/create/$step" });
  const { handleSubmit, control, reset } = useForm({
    defaultValues: {
      personident: personident,
    },
  });
  const navigate = useNavigate();

  const { step } = Route.useParams();

  return (
    <form
      onSubmit={handleSubmit((values) => {
        const trimmedPersonident =
          values.personident?.replaceAll(" ", "") ?? "";

        navigate({
          to: `/ressurs/create/${step}`,
          search: {
            personident: trimmedPersonident,
            visningsfornavn: undefined,
            visningsmellomnavn: undefined,
            visningsetternavn: undefined,
            primaryTelefonnummer: undefined,
          },
        }).then();
      })}
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
            label="Fyll inn fødselsnummer/D-nummer, så finner vi personen i Folkeregisteret"
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
            value?.replaceAll(" ", "").length === 11 ||
            "Fødselsnummer må inneholde 11 siffer",
        }}
      />
    </form>
  );
}
