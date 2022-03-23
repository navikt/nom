import {Pagewrap} from "../components/Pagewrap";
import * as Gq from "../api/generated/graphql_generated"

export function NomApiGraphqlTestPage() {
    const queryResult = Gq.useHentAlleOrganisasjonsenheterQuery()

    return <Pagewrap>
        {/*<Button onClick={() =>{}}>Post graphql</Button>*/}
        <h2>Graphql data fra nom-api</h2>
        {queryResult.loading && <div>...laster graphql data fra nom-api</div>}
        {queryResult.data && <pre>{JSON.stringify(queryResult.data, null, 2)}</pre>}
    </Pagewrap>
}
