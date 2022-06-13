import ContentWrapper from "../components/ContentWrapper"
import * as Gq from "../api/generated/graphql_generated"
import { useParams } from "react-router"
import { agressoUrlToData } from "../resources/utils/url_utils"
import OrgHierarkiList from "../components/OrgHierarki/OrgHierarkiList"
import { Heading, Loader } from "@navikt/ds-react"
import { css } from "@emotion/css"

const mapOverEnheter = (data: Gq.HentOrganisasjonsEnheterHierarkiQuery) => {
    if (!data.organisasjonsenhet) return []
    else if (data.organisasjonsenhet.overenheter.length === 0) return []
    else {
        let enheter = [data.organisasjonsenhet.overenheter[0] as Gq.Organisering]
        while (enheter[0].organisasjonsenhet.organiseringer.length > 0) {
            enheter = [enheter[0].organisasjonsenhet.organiseringer[0] as Gq.Organisering, ...enheter]
        }
        return enheter 
    }
}

const extractUnderenheter = (queryresult: Gq.HentOrganisasjonsenhetOgOrganiseringQuery) => {
    return queryresult.organisasjonsenhet?.organiseringer.filter(org => org.retning === Gq.Retning.Under).map(it => it.organisasjonsenhet) || []
}
const extractOverEnhet = (queryresult: Gq.HentOrganisasjonsenhetMedOverenhetQuery) => {
    return queryresult.organisasjonsenhet?.organiseringer[0]?.organisasjonsenhet
}
const extractCurrentEnhet = (queryresult: Gq.HentOrganisasjonsEnheterHierarkiQuery) => {
    return queryresult.organisasjonsenhet || undefined
}

export type UnderEnheterList = ReturnType<typeof extractUnderenheter>;
export type OverEnhet = ReturnType<typeof extractOverEnhet>;
export type CurrentEnhet = ReturnType<typeof extractCurrentEnhet>

const OrganisasjonHierarkiPage = () => {
    const {id, resourceId} = useParams()

    const agrId = agressoUrlToData(id || '') 

    const query = Gq.useHentOrganisasjonsEnheterHierarkiQuery({ variables: {agressoId: agrId.agressoId, orgNiv: agrId.agressoNiva}, errorPolicy: 'all'})
    console.log(query, "QUERY")
    const resourceQuery = Gq.useHentRessursMedNavIdentQuery({variables: { navIdent: resourceId}, skip: !resourceId})

    return (
        <>
            <ContentWrapper>
                <>
                    <Heading size="xlarge" className={css({ marginBottom: '20px'})} >NAV Organisasjon</Heading>

                    {query.error && <p>Feil</p>}
                    
                    {!query.data && <Loader />}
                    
                    {query.data  && (
                        <OrgHierarkiList 
                            currentEnhet={extractCurrentEnhet(query.data)}
                            underEnheter={query.data.organisasjonsenhet?.underenheter as Gq.Organisering[]}
                            overEnheter={mapOverEnheter(query.data)}
                            ressurs={resourceQuery.data ? resourceQuery.data.ressurs as Gq.Ressurs : undefined}
                        />
                    )}
                    
                </>
            </ContentWrapper>
        </>
    )
} 

export default OrganisasjonHierarkiPage