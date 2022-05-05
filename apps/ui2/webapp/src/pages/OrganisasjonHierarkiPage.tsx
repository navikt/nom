import BannerSmall from "../components/BannerSmall"
import ContentWrapper from "../components/ContentWrapper"
import * as Gq from "../api/generated/graphql_generated"

const OrganisasjonHierarkiPage = () => {
    const query = Gq.useHentAlleOrganisasjonsenheterQuery() 
    

    return (
        <>
            <BannerSmall title="Organisasjonshierarkiet"/>
            <ContentWrapper>
                <>
                </>
            </ContentWrapper>
        </>
    )
} 

export default OrganisasjonHierarkiPage