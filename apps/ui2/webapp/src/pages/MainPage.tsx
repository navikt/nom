import Banner from "../components/Banner";
import ContentWrapper from "../components/ContentWrapper";
import CustomLinkPanel from "../components/CustomLinkPanel";
import { orghierarkietLogo, ressursLogo } from "../components/Images";

const MainPage = () => {
  return (
    <div>
      <Banner />
      <ContentWrapper>
        <>
          <CustomLinkPanel
            title="Ressurser"
            description="Oversikt over alle ressurser i NAV og mulighet for å opprette nye ressurser."
            icon={ressursLogo}
            linkTo="/ressurs/create"
          />

          <CustomLinkPanel
            title="Organisasjonshierarkiet"
            description="Oversikt over alle organisasjonsenheter i NAV."
            icon={orghierarkietLogo}
            linkTo="/org"
          />
        </>
      </ContentWrapper>
    </div>
  )
}

export default MainPage;
