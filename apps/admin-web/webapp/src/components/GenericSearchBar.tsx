import {
  PadlockLockedFillIcon,
  PadlockUnlockedFillIcon,
} from "@navikt/aksel-icons";
import { Button, TextField } from "@navikt/ds-react";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useForm } from "react-hook-form";

import type { searchType } from "~/api/queries.ts";
import { useHasAdminAccess } from "~/api/useGetSession.tsx";

export function GenericSearchBar({
  search,
  editButton,
}: {
  search: searchType;
  editButton?: boolean;
}) {
  const methods = useForm({ defaultValues: search });
  const navigate = useNavigate();
  const hasAdminAccess = useHasAdminAccess();

  const submitHandler = methods.handleSubmit((values) => {
    navigate({
      search: (prev) => {
        const overwriteOffset = {
          fromOffset: values.fromOffset
            ? Number.parseInt(values.fromOffset as never)
            : undefined,
        };

        const overwriteFilterText = {
          filterText: values.filterText ?? undefined,
        };

        return {
          ...prev,
          ...values,
          ...overwriteFilterText,
          ...overwriteOffset,
        };
      },
    }).then();
  });

  const tilbakestill = () => {
    methods.reset({
      filterText: "",
      fromOffset: null as unknown as undefined,
    });

    const newSearch = {
      ...search,
      filterText: undefined,
      fromOffset: undefined,
    };

    navigate({
      search: { ...newSearch },
      replace: true,
    }).then();
  };

  return (
    <>
      <form className="flex gap-4 p-6" onSubmit={submitHandler}>
        {hasAdminAccess && (
          <ToggleEditMode editButton={editButton} search={search} />
        )}
        <TextField
          label="Vilkårlig tekst"
          {...methods.register("filterText")}
        />
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
    </>
  );
}

function ToggleEditMode({
  editButton,
  search,
}: {
  editButton?: boolean;
  search: searchType;
}) {
  if (!editButton) {
    return <></>;
  }

  const navigate = useNavigate();

  const { allowEdit } = useSearch({ from: "/state/orgenhet" });

  if (allowEdit) {
    return (
      <Button
        icon={<PadlockUnlockedFillIcon aria-hidden />}
        onClick={() =>
          navigate({
            search: { ...search, allowEdit: undefined },
            params: (previous) => previous,
          })
        }
        style={{ height: "fit-content", alignSelf: "end" }}
        type="button"
        variant="secondary"
      />
    );
  }

  return (
    <Button
      icon={<PadlockLockedFillIcon aria-hidden />}
      onClick={() =>
        navigate({
          search: { ...search, allowEdit: true },
          params: (previous) => previous,
        })
      }
      style={{ height: "fit-content", alignSelf: "end" }}
      type="button"
      variant="secondary"
    />
  );
}
