import {
  PadlockLockedFillIcon,
  PadlockUnlockedFillIcon,
} from "@navikt/aksel-icons";
import { Button, TextField } from "@navikt/ds-react";
import type { AnyRoute } from "@tanstack/react-router";
import { useNavigate, useSearch } from "@tanstack/react-router";
import { useForm } from "react-hook-form";

import { useHasAdminAccess } from "~/api/useGetSession.tsx";

export function GenericSearchBar({
  route,
  editButton,
}: {
  route: AnyRoute;
  editButton?: boolean;
}) {
  const search = route.useSearch();

  const methods = useForm({ defaultValues: search });
  const navigate = useNavigate();
  const hasAdminAccess = useHasAdminAccess();

  const submitHandler = methods.handleSubmit((values) => {
    navigate({
      to: route.to,
      search: {
        ...search,
        filterText: values.filterText ?? undefined,
        fromOffset: values.fromOffset
          ? Number.parseInt(values.fromOffset)
          : undefined,
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
          <ToggleEditMode editButton={editButton} route={route} />
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
  route,
}: {
  editButton?: boolean;
  route: AnyRoute;
}) {
  if (!editButton) {
    return <></>;
  }

  const navigate = useNavigate();

  const { allowEdit } = useSearch({ from: "/state/orgenhet" });

  const search = route.useSearch();

  if (allowEdit) {
    return (
      <Button
        icon={<PadlockUnlockedFillIcon aria-hidden />}
        onClick={() =>
          navigate({
            to: route.to,
            search: { ...search, allowEdit: undefined },
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
          to: route.to,
          search: { ...search, allowEdit: true },
        })
      }
      style={{ height: "fit-content", alignSelf: "end" }}
      type="button"
      variant="secondary"
    />
  );
}
