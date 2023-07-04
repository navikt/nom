import { css } from "@emotion/react";
import { Heading, Search } from "@navikt/ds-react";
import { encode } from "js-base64";
import { Controller, useForm } from "react-hook-form";
import { Outlet, useNavigate, useParams } from "react-router";

import opprettRessursIcon from "~/resources/opprettRessursIcon.svg";

export function SearchForPerson() {
  const { personident } = useParams();
  const { handleSubmit, control, reset } = useForm({
    defaultValues: { personident },
  });
  const navigate = useNavigate();

  return (
    <div
      css={css`
        min-height: 300px;
        margin: 0 auto;
        width: 70%;
        padding: var(--a-spacing-8);
        border: 1px solid var(--a-gray-400);
        border-top: 5px solid var(--a-lightblue-800);
        display: flex;
        flex-direction: column;
        background: white;
        gap: var(--a-spacing-4);
      `}
    >
      <img
        alt=""
        css={css`
          height: 65px;
          width: 65px;
          align-self: center;
        `}
        src={opprettRessursIcon}
      />
      <Heading
        css={css`
          align-self: center;
        `}
        size="large"
      >
        Personopplysninger
      </Heading>
      <form
        onSubmit={handleSubmit((values) =>
          navigate(`/ressurs/create/${encode(values.personident ?? "")}`)
        )}
      >
        <Controller
          control={control}
          name="personident"
          render={({ field: { onChange, value }, fieldState: { error } }) => (
            <Search
              autoComplete="off"
              css={css`
                label {
                  font-weight: var(--a-font-weight-regular);
                }
              `}
              error={error?.message}
              hideLabel={false}
              label="Fyll inn personnummer/D-nummer så finner vi personen i Folkeregisteret"
              onChange={(newValue) => onChange(newValue)}
              onClear={() => {
                reset({ personident: "" });
                navigate("/ressurs/create");
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
      <Outlet />
    </div>
  );
}
