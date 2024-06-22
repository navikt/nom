import { Button, TextField } from "@navikt/ds-react";
import { useNavigate } from "@tanstack/react-router";
import { useForm } from "react-hook-form";

import type { searchType } from "~/api/queries.ts";

export function GenericSearchBar({ search }: { search: searchType }) {
  const methods = useForm({ defaultValues: search });
  const navigate = useNavigate();

  const submitHandler = methods.handleSubmit((values) => {
    navigate({
      search: (prev) => ({ ...prev, ...values }),
    });
  });

  const tilbakestill = () => {
    methods.reset({ filterText: "", fromOffset: undefined });
    navigate({
      search: {},
      replace: true,
    });
  };

  return (
    <form className="flex gap-4 p-6" onSubmit={submitHandler}>
      <TextField label="Vilkårlig tekst" {...methods.register("filterText")} />
      <TextField label="Fra offset" {...methods.register("fromOffset")} />
      <Button className="h-fit mt-8" type="submit">
        Søk
      </Button>
      <Button
        className="h-fit mt-8"
        onClick={tilbakestill}
        type="button"
        variant="tertiary"
      >
        Tilbakestill
      </Button>
    </form>
  );
}
