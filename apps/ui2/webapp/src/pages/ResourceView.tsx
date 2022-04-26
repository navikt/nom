import * as Gq from "../api/generated/graphql_generated";
import { useLocation, useParams } from "react-router";
import PageTitle from "../components/PageTitle";
import Card from "../components/Card";
import { css } from "@emotion/css";
import TextWithLabel from "../components/TextWithLabel";
import { Alert } from "@navikt/ds-react";
import ContentWrapper from "../components/ContentWrapper";


const rowStyles = css({
  marginTop: '20px',
  display: "flex",
  div: {
    width: '70%'
  }
})

const ResourceView = () => {
  const { id } = useParams<any>();
  const location = useLocation() as any
  const query = Gq.useHentRessursMedNavIdentQuery({
    variables: { navIdent: id },
  })


  const showAlreadyExistsAlert = () => {
    if (!location.state) return null
    if (location.state.hasOwnProperty('alreadyExists')) 
      return (
        <Alert variant="info">Ressurs finnes allerede i NAV. Endringer kan gjøres i Remedy og Unit4 (Agresso). </Alert>
      )
      
    return null
  }

  return (
    <div>
      {query.loading && <div>...laster graphql data fra nom-api</div>}
      {showAlreadyExistsAlert()}
      {query.error && <p>{query.error.message}</p>}
      {query.data && (
        <ContentWrapper>
          <>
          <PageTitle
            title={
              query.data.ressurs?.visningsNavn ||
              query.data.ressurs?.fornavn + " " + query.data.ressurs?.etternavn
            }
          />
          <Card>
            <div >
              <div className={rowStyles}>
                <TextWithLabel label="Fornavn" text={query.data.ressurs?.fornavn || ''} />
                <TextWithLabel label="Etternavn"  text={query.data.ressurs?.etternavn || ''}/>
                <TextWithLabel label="NAV-ident"  text={query.data.ressurs?.navIdent || ''}/>
              </div>
              <div className={rowStyles}>
                <TextWithLabel label="Kjønn"  text='-'/> 
                <TextWithLabel label="E-post"  text={query.data.ressurs?.epost || ''}/>
                <TextWithLabel label="Telefonnummer"  text='-'/>
              </div>
              <div className={rowStyles}>
                <TextWithLabel label="Org.enhet"  text='-'/> 
                <TextWithLabel label="Leder"  text='-'/>
                <TextWithLabel label="Sektor"  text='-'/>
              </div>
            </div>
          </Card>
          </>
        </ContentWrapper>
      )}
    </div>
  );
};

export default ResourceView;
