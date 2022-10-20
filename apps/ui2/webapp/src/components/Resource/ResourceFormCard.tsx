import { css } from "@emotion/css";
import { Alert, Button, Radio, RadioGroup, TextField } from "@navikt/ds-react";
import { useForm } from "react-hook-form";

import Card from "../Card";
import { radioStyles } from "../Styles";

const rowMarginTop = "20px";

const rowStyles = css({
  marginTop: rowMarginTop,
  width: "100%",
  display: "flex",
  // justifyContent: 'space-between',
  marginBottom: "1em",
});

type ResourceFormProperties = {
  cardTitle: string;
  handleCreate: (values: unknown) => void;
};

const ResourceFormCard = (properties: ResourceFormProperties) => {
  const { cardTitle, handleCreate } = properties;
  const { register, handleSubmit } = useForm();

  return (
    <form onSubmit={handleSubmit((form) => handleCreate(form))}>
      <Card title={cardTitle}>
        <div className={css({ marginTop: rowMarginTop })}>
          <Alert variant="info">
            Navnet under vil bli brukt i alle NAVs applikasjoner. Fjern
            eventuelle navn du ikke vil ha med. Ressursen må ha minst ett
            fornavn og minst ett etternavn.
          </Alert>

          <div className={rowStyles}>
            <TextField
              autoComplete="off"
              className={css({ width: "48%", marginRight: "5px" })}
              label="Fornavn"
              type="text"
              {...register("fornavn")}
            />
            <TextField
              autoComplete="off"
              className={css({ width: "48%" })}
              label="Etternavn"
              type="text"
              {...register("etternavn")}
            />
          </div>

          <div className={css({ display: "flex", marginTop: "2m" })}>
            <TextField
              className={css({ width: "90%" })}
              label="Org.enhet"
              type="text"
              {...register("USIKKER")} // USIKKER HVA SOM SKAL FYLLES INN HER
            />
            {/*<Button variant="tertiary"><img src={searchIcon} className={css({scale: "90%"})}/></Button>*/}
          </div>

          <div className={rowStyles}>
            <RadioGroup legend="Sektor" size="medium">
              {/*<div className={css({ display: 'flex', justifyContent: '', width: '100%'})}>*/}
              <Radio className={radioStyles} value="10">
                NAV statlig
              </Radio>
              <Radio className={radioStyles} value="20">
                NAV kommunal
              </Radio>
              <Radio className={radioStyles} value="30">
                Ekstern
              </Radio>
              {/*</div>*/}
            </RadioGroup>
          </div>
        </div>
      </Card>

      <div className={css({ display: "flex", marginTop: "15px" })}>
        <Button
          className={css({ marginRight: "10px" })}
          type="submit"
          variant="primary"
        >
          Opprett ressurs
        </Button>
        <Button disabled type="button" variant="secondary">
          Avbryt
        </Button>
      </div>
    </form>
  );
};

export default ResourceFormCard;
