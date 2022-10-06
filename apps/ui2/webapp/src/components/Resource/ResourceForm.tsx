import {
  Alert,
  Button,
  Heading,
  Radio,
  RadioGroup,
  Search,
  TextField,
} from "@navikt/ds-react";
import { useForm } from "react-hook-form";
import { css } from "@emotion/css";
import CreateResourceConfirmationModal from "./CreateResourceConfirmationModal";
import * as Gq from "../../api/generated/graphql_generated";

const rowStyles = css({
  display: "flex",
  marginTop: "1.2rem",
  gap: "1rem",

  "& > *": {
    flex: 1,
  },
});

export type InitialValuesResourceForm = {
  visningsfornavn: string;
  mellomnavn?: string;
  visningsetternavn: string;
  sektorTxt: string;
};

type ResourceFormProps = {
  initialValues: InitialValuesResourceForm;
  onSubmit: (values: Gq.OpprettRessursRequest) => void;
};

const ResourceForm = (props: ResourceFormProps) => {
  const { initialValues, onSubmit } = props;
  const { register, handleSubmit, getValues } = useForm({
    defaultValues: { ...initialValues },
  });

  return (
    <form onSubmit={handleSubmit((form: any) => onSubmit(form))}>
      <div className={css({ marginTop: "2rem" })}>
        <Heading size="small">Opplysninger til NOM</Heading>
        <Alert
          variant="info"
          size="medium"
          className={css({ marginTop: "1rem" })}
        >
          Navnet under vil bli brukt i alle NAVs applikasjoner. Fjern eventuelle
          navn du ikke vil ha med. Ressursen må ha minst ett fornavn og minst
          ett etternavn.
        </Alert>

        <div className={rowStyles}>
          <TextField
            type="text"
            label="Fornavn"
            autoComplete="off"
            {...register("visningsfornavn")}
          />
          <TextField
            type="text"
            label="Etternavn"
            autoComplete="off"
            {...register("visningsetternavn")}
          />
        </div>

        <div className={rowStyles}>
          <Search
            label="Org.enhet"
            variant="secondary"
            hideLabel={false}
            disabled
          />
        </div>

        <div className={rowStyles}>
          <RadioGroup legend="Sektor" size="medium" {...register("sektorTxt")}>
            <Radio value="NAV_STATLIG" checked>
              NAV statlig
            </Radio>
            <Radio value="NAV_KOMMUNAL" disabled>
              NAV kommunal
            </Radio>
            <Radio value="EKSTERN" disabled>
              Ekstern
            </Radio>
          </RadioGroup>
        </div>

        <div className={rowStyles}>
          <CreateResourceConfirmationModal
            formValues={getValues()}
            onSubmit={onSubmit}
          />
          <Button variant="secondary">Avbryt</Button>
        </div>
      </div>
    </form>
  );
};

export default ResourceForm;
