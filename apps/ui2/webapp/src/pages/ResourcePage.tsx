import { Pagewrap } from "../components/Pagewrap";
import * as Gq from "../api/generated/graphql_generated"
import { useParams } from "react-router";

const ResourcePage = () => {
  const { id } = useParams<any>();
  const query = Gq.useHentRessursMedNavIdentQuery({ variables: { navIdent: id}})

  return (
    <Pagewrap>
      <h1>Resource page</h1>

      {query.data && <h1>{query.data.ressurs?.navIdent}</h1>}
      {query.error && <p>Fant feil</p>}
    </Pagewrap>
  );
};

export default ResourcePage;
