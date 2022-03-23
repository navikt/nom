import { Button, Panel, TextField } from "@navikt/ds-react";
import { Pagewrap } from "../components/Pagewrap";
import React from "react";
import { useForm } from "react-hook-form";
import { css } from "@emotion/css";

const formFieldsStyle = css({
  display: "grid",
  rowGap: "1rem",
});

export function FormTestPage() {
  const [formView, setFormView] = React.useState<any>(undefined);
  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm();
  const onSubmit = (data: any) => {
    console.log(data);
    setFormView(data);
  };

  console.log({ errors });

  return (
    <Pagewrap>
      <form onSubmit={handleSubmit(onSubmit)}>
        <Panel className={formFieldsStyle}>
          <TextField
            label="Har du noen uviktige tilbakemeldinger?"
            size="medium"
            {...register("example")}
          />
          <TextField
            label="Har du noen obligatoriske tilbakemeldinger?"
            size="medium"
            error={
              errors.exampleRequired && <span>This field is required</span>
            }
            {...register("exampleRequired", { required: true })}
          />
          <Button variant="primary" size="medium" type="submit">
            Button
          </Button>
          {/* <input type="submit" /> */}
        </Panel>
      </form>
      <hr />
      Form output, use in API request body instead of printing:
      <pre>{JSON.stringify(formView, null, 2)}</pre>
    </Pagewrap>
  );
}
