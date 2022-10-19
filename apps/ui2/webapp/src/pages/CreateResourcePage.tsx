import { css } from "@emotion/css";
import { Controller, useForm } from "react-hook-form";
import { Outlet, useNavigate, useParams } from "react-router";
import ContentWrapper from "../components/ContentWrapper";
import ResourceFormPanel from "../components/Resource/ResourceFormPanel";
import { BodyShort, Heading, Search } from "@navikt/ds-react";
import { opprettRessursIkon } from "../components/Images";

export function SearchForPerson() {
  const { personIdent } = useParams();
  const { handleSubmit, control, reset } = useForm({
    defaultValues: { personIdent },
  });
  const navigate = useNavigate();

  return (
    <ContentWrapper>
      <ResourceFormPanel>
        <img
          className={css`
            height: 65px;
            width: 65px;
            align-self: center;
            color: var(--nom-primary);
          `}
          alt="Opprett ressurs ikon"
          src={opprettRessursIkon}
        />
        <Heading
          size="large"
          className={css`
            align-self: center;
          `}
        >
          Personalopplysninger
        </Heading>
        <form
          onSubmit={handleSubmit((values) =>
            navigate(`/ressurs/create/${values.personIdent}`)
          )}
        >
          <BodyShort size="medium">
            Fyll inn personnummer/D-nummer så finner vi personen i
            folkeregisteret
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
              rules={{
                validate: (value) =>
                  value?.length === 11 || "Må være 11 siffer",
              }}
              render={({
                field: { onChange, value },
                fieldState: { error },
              }) => (
                <Search
                  value={value}
                  type="number"
                  onChange={(newValue) => onChange(newValue)}
                  label=""
                  variant="secondary"
                  hideLabel={false}
                  error={error?.message}
                  onClear={() => {
                    reset({ personIdent: "" });
                    navigate("/ressurs/create");
                  }}
                />
              )}
            />
          </div>
        </form>
        <Outlet />
      </ResourceFormPanel>
    </ContentWrapper>
  );
}
