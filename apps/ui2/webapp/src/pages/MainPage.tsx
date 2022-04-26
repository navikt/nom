import Banner from "../components/Banner";
import ContentWrapper from "../components/ContentWrapper";
import CustomLinkPanel from "../components/CustomLinkPanel";
import PageTitle from "../components/PageTitle";

const MainPage = () => {
  return (
    <div>
      <Banner />
      <ContentWrapper>
        <>
          <CustomLinkPanel
            title="Ressurser"
            description="Oversikt over alle ressurser i NAV og mulighet for å opprette nye ressurser."
            linkTo="/ressurs/create"
          />
        </>
      </ContentWrapper>
    </div>
  )
}

export default MainPage;
