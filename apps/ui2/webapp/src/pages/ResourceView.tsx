import * as Gq from "../api/generated/graphql_generated"
import { useParams } from "react-router";
import PageTitle from "../components/PageTitle";

const ResourceView = () => {
  const { id } = useParams<any>();
  const query = Gq.useHentRessursMedNavIdentQuery({ variables: { navIdent: id}})

  return (
      <div>  
        <PageTitle title="Ressurs" />
        {query.loading && <div>...laster graphql data fra nom-api</div>}
        {query.data && 
            <div>
                <p>Nav-Ident: {query.data.ressurs?.navIdent}</p>
                <p>Fornavn: {query.data.ressurs?.fornavn}</p>
                <p>Etternavn: {query.data.ressurs?.etternavn}</p>
                <p>Epost: {query.data.ressurs?.epost}</p>
            </div>
        }
      </div>

  )
}

export default ResourceView;
