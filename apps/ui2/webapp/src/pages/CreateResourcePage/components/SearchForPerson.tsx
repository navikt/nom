import { css } from "@emotion/react";
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
        src={opprettRessursIkon}
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
        <BodyShort size="medium">
          Fyll inn personnummer/D-nummer så finner vi personen i Folkeregisteret
        </BodyShort>
        <div
          css={css(
            css`
              display: flex;
              gap: var(--a-spacing-4);
              margin-top: var(--a-spacing-4);
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
