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
import { useState } from "react";
import ModalResource from "./ModalResource";
import * as Gq from "../../api/generated/graphql_generated";

const rowStyles = css({
  display: "flex",
  marginTop: "1.2rem",
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
  const [showModal, setShowModal] = useState<boolean>(false);

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
            className={css({ width: "48%", marginRight: "1rem" })}
            autoComplete="off"
            {...register("visningsfornavn")}
          />
          <TextField
            type="text"
            label="Etternavn"
            className={css({ width: "48%" })}
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
          <Button
            type="button"
            className={css({ marginRight: "1rem" })}
            onClick={() =>
              showModal ? setShowModal(false) : setShowModal(true)
            }
          >
            Opprett ressurs
          </Button>
          <Button variant="secondary">Avbryt</Button>
        </div>
      </div>
      <ModalResource
        isOpen={showModal}
        formValues={getValues()}
        onSubmit={onSubmit}
        closeModal={() => setShowModal(false)}
      />
    </form>
  );
};

export default ResourceForm;
