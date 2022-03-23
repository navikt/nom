import {Pagewrap} from "../components/Pagewrap";
import * as Gq from "../api/generated/graphql_generated"

export function NomApiGraphqlTestPage() {
    const v = Gq.useHentAlleOrganisasjonsenheterQuery()

    return <Pagewrap>
        {/*<Button onClick={() =>{}}>Post graphql</Button>*/}
        <h2>Graphql data fra nom-api</h2>
        v.loading && <div>laster graphql data fra nom-api</div>
        {v.data && <pre>{JSON.stringify(v.data, null, 2)}</pre>}
    </Pagewrap>
}
