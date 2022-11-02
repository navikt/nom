import { css } from "@emotion/css";
import { BodyShort, Heading, Search } from "@navikt/ds-react";
import { encode } from "js-base64";
import { Controller, useForm } from "react-hook-form";
import { Outlet, useNavigate, useParams } from "react-router";

import { opprettRessursIkon } from "../../../components/Images";

export function SearchForPerson() {
  const { personident } = useParams();
  const { handleSubmit, control, reset } = useForm({
    defaultValues: { personident },
  });
  const navigate = useNavigate();

  return (
    <div
      className={css`
        min-height: 300px;
        margin: 0 auto;
        width: 70%;
        padding: var(--navds-spacing-8);
        border: 1px solid var(--navds-global-color-gray-400);
        border-top: 5px solid var(--navds-global-color-lightblue-800);
        border-radius: 5px;
        display: flex;
        flex-direction: column;
        background: white;
        gap: var(--navds-spacing-4);
      `}
    >
      <img
        alt="Opprett ressurs ikon"
        className={css`
          height: 65px;
          width: 65px;
          align-self: center;
          color: var(--nom-primary);
        `}
        src={opprettRessursIkon}
      />
      <Heading
        className={css`
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
        <BodyShort size="medium">
          Fyll inn personnummer/D-nummer så finner vi personen i Folkeregisteret
        </BodyShort>
        <div
          className={css(
            css`
              display: flex;
              gap: var(--navds-spacing-4);
              margin-top: var(--navds-spacing-4);
            `
          )}
        >
          <Controller
            control={control}
            name="personident"
            render={({ field: { onChange, value }, fieldState: { error } }) => (
              <Search
                error={error?.message}
                hideLabel={false}
                label=""
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
        </div>
      </form>
      <Outlet />
    </div>
  );
}
