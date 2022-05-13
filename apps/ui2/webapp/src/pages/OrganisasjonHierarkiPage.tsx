import BannerSmall from "../components/BannerSmall"
import ContentWrapper from "../components/ContentWrapper"
import * as Gq from "../api/generated/graphql_generated"
import OrgEnhetList from "../components/OrgHierarki/OrgEnhetList"
import { useParams } from "react-router"
import { agressoIdDataToUrl, agressoUrlToData } from "../resources/utils/url_utils"
import { Link } from "react-router-dom"
import React, { useEffect, useState } from "react"

const UnderEnheter = (props: {underenheter: UnderEnheterList}) => (
    <ul>
        {props.underenheter.map(ue => (
            <li>{ue.navn} - <Link to={'/org/' + agressoIdDataToUrl(ue.agressoId, ue.orgNiv, ue.navn)}>
                                    {agressoIdDataToUrl(ue.agressoId, ue.orgNiv, ue.navn)}
                            </Link>
            </li>
        ))}
    </ul>
)

const useOverEnheter = (props: {variables: {agressoId: string, orgNiv: string}}) => {
    const [overEnheter, setOverEnheter] = useState<OverEnhet[]>()
    const [currentOverenhet, setCurrentOverenhet] = useState<any>(props)
    const currentQuery = Gq.useHentOrganisasjonsenhetMedOverenhetQuery({...currentOverenhet, skip: !currentOverenhet})

    // console.log({overEnheter, currentOverenhet, currentQuery})

    useEffect(() => {
        if (!currentQuery.data) return

        const oe = extractOverEnhet(currentQuery.data)
        if (!currentQuery.loading && oe) {
            console.log({oe, currentOverenhet})
            setOverEnheter([oe, ...overEnheter || []])
            setCurrentOverenhet({ variables: {agressoId: oe.agressoId, orgNiv: oe.orgNiv}})
        } else { 
            console.log("I Else", overEnheter)
            setCurrentOverenhet(undefined)
        }

    }, [currentQuery.data])

    
    return {loading: !currentOverenhet, data: !currentOverenhet ? overEnheter : undefined, error: currentQuery.error}
}

const extractUnderenheter = (queryresult: Gq.HentOrganisasjonsenhetOgOrganiseringQuery) => {
    return queryresult.organisasjonsenhet?.organiseringer.filter(org => org.retning === Gq.Retning.Under).map(it => it.organisasjonsenhet) || []
}

const extractOverEnhet = (queryresult: Gq.HentOrganisasjonsenhetMedOverenhetQuery) => {
    return queryresult.organisasjonsenhet?.organiseringer[0]?.organisasjonsenhet
}

type UnderEnheterList = ReturnType<typeof extractUnderenheter>;
type OverEnhet = ReturnType<typeof extractOverEnhet>;

const OrganisasjonHierarkiPage = () => {
    const {id} = useParams()
    const agrId = agressoUrlToData(id || '') 

    //const query = Gq.useHentOrganisasjonsenhetOgOrganiseringQuery({ variables: {agressoId: agrId.agressoId, orgNiv: agrId.agressoNiva}})
    const overenheterQuery = useOverEnheter({ variables: {agressoId: agrId.agressoId, orgNiv: agrId.agressoNiva}})
    console.log(overenheterQuery, "QUERY")

    // if (query.error)
    //     console.error(query.error)

    //const underenheter=query.data?.organisasjonsenhet?.organiseringer.map(it => it.organisasjonsenhet) || []

    return (
        <>
            <BannerSmall title="Organisasjonshierarkiet"/>
            <ContentWrapper>
                <>
                    <p>{JSON.stringify(agrId)}</p>
                    {/* {query.loading && <p>Loading...</p>}
                    {query.error && <p>Feil</p>}
                    
                    {query.data && (
                        <>
                            <pre>{JSON.stringify(query.data, null, 2)}</pre>) 
                             <UnderEnheter underenheter={extractUnderenheter(query.data)} /> 
                        </>
                    )} */}
                    
                </>
            </ContentWrapper>
        </>
    )
} 

export default OrganisasjonHierarkiPage