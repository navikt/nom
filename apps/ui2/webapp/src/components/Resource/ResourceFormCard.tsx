import { css } from "@emotion/css";
import { Alert, Button, Radio, RadioGroup, TextField } from "@navikt/ds-react";
import { useForm } from "react-hook-form";
import { OpprettRessursRequest2 } from "../../api/generated/graphql_generated";
import Card from "../Card";
import CustomRadio from "../CustomRadio";
import { radioStyles } from "../Styles";



const rowMarginTop = '20px'

const rowStyles = css({
    marginTop: rowMarginTop,
    width: '100%',
    display: 'flex',
    justifyContent: 'space-between',
})

type ResourceFormProps = {
  cardTitle: string
  handleCreate: (values: any) => void
}

const ResourceFormCard = (props: ResourceFormProps) => {
  const { cardTitle, handleCreate } = props
  const { register, handleSubmit } = useForm()

  return (
    <form onSubmit={handleSubmit((form: any) => handleCreate(form))}>
      <Card title={cardTitle}>
          <div className={css({ marginTop: rowMarginTop})}>
              <Alert variant="info">
                  Navnet under vil bli brukt i alle NAVs applikasjoner. 
                  Fjern eventuelle navn du ikke vil ha med. Ressursen må ha minst ett fornavn og minst ett etternavn.
              </Alert>

              <div className={rowStyles}>
                  <TextField
                      type="text"
                      label="Fornavn"
                      className={css({ width: '48%', marginRight: '5px'})}
                      autoComplete="off"
                      {...register("fornavn")}
                  />
                  <TextField
                      type="text"
                      label="Etternavn"
                      className={css({ width: '48%'})}
                      autoComplete="off"
                      {...register("etternavn")}
                  />
              </div>
          
              <div className={rowStyles}>
                <RadioGroup legend="Sektor" size="medium">
                  <div className={css({ display: 'flex', justifyContent: 'space-between', width: '100%'})}>
                    <Radio className={radioStyles} value="10">NAV statlig</Radio>
                    <Radio className={radioStyles} value="20">NAV kommunal</Radio>
                    <Radio className={radioStyles} value="30">Ekstern</Radio>
                  </div>
                  
                </RadioGroup>
              </div>

          </div>
      </Card>

      <div className={css({ display: 'flex', marginTop: '15px'})}>
        <Button type="submit" variant="primary" className={css({ marginRight: '10px'})}>Opprett ressurs</Button>
        <Button type="button" variant="secondary" disabled>Avbryt</Button>
      </div>
    </form>

  );
};

export default ResourceFormCard
