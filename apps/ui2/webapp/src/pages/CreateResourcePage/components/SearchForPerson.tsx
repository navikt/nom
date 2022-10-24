import { css } from "@emotion/css";
import { BodyShort, Heading, Search } from "@navikt/ds-react";
import { encode } from "js-base64";
import { Controller, useForm } from "react-hook-form";
import { Outlet, useNavigate, useParams } from "react-router";

import { opprettRessursIkon } from "../../../components/Images";
import ResourceFormPanel from "../../../components/Resource/ResourceFormPanel";

export function SearchForPerson() {
  const { personIdent } = useParams();
  const { handleSubmit, control, reset } = useForm({
    defaultValues: { personIdent },
  });
  const navigate = useNavigate();

  return (
    <ResourceFormPanel>
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
          navigate(`/ressurs/create/${encode(values.personIdent ?? "")}`)
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
            name="personIdent"
            render={({ field: { onChange, value }, fieldState: { error } }) => (
              <Search
                error={error?.message}
                hideLabel={false}
                label=""
                onChange={(newValue) => onChange(newValue)}
                onClear={() => {
                  reset({ personIdent: "" });
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
    </ResourceFormPanel>
  );
}
