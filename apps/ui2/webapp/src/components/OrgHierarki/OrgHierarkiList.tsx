import { css } from "@emotion/css";
import { Link } from "react-router-dom";
import { CurrentEnhet } from "../../pages/OrganisasjonHierarkiPage";
import { agressoIdDataToUrl } from "../../resources/utils/url_utils";
import CustomLinkPanel2, { LinkPanelType } from "../CustomLinkPanel2";
import EnhetInfo from "./EnhetInfo";
import { Organisering } from "../../api/generated/graphql_generated";
import { Heading } from "@navikt/ds-react";

const linkStyles = css({ textDecoration: "none", color: "black" });
const dividerStyles = css({ 
    background: '#00347D', 
    height: '20px', 
    width: '2px',
    border: '1px solid #00347D', 
    margin: '0 auto'
})
const Divider = () => <div className={dividerStyles}></div>

type OrgHierarkiListProps = {
  currentEnhet: CurrentEnhet 
  underEnheter: Organisering[]
  overEnheter: Organisering[]
};

const OrgHierarkiList = (props: OrgHierarkiListProps) => {
  const { currentEnhet, underEnheter, overEnheter} = props

  return (
    <div>
      <div>
        {overEnheter.map((oe: Organisering) => (
                  <>
                    <Link to={"/org/" + agressoIdDataToUrl(oe.organisasjonsenhet.agressoId, oe.organisasjonsenhet.orgNiv, oe.organisasjonsenhet.navn)} className={linkStyles}>
                      <CustomLinkPanel2 type={LinkPanelType.OVERENHET}>
                        <EnhetInfo
                          title={oe.organisasjonsenhet.navn}
                          lederNavn={oe.organisasjonsenhet.leder[0].ressurs.visningsNavn || "Mangler Navn"}
                          ident={oe.organisasjonsenhet.leder[0].ressurs.navIdent}
                        />
                      </CustomLinkPanel2>
                    </Link>
                    <Divider />
                  </>
                ))
        }
      </div>
      

      <CustomLinkPanel2 type={LinkPanelType.CURRENT}>
        <EnhetInfo 
            title={currentEnhet?.navn || 'Feil i henting av navn'} 
            lederNavn={currentEnhet?.leder[0].ressurs.visningsNavn || "Mangler navn"}
            ident={currentEnhet?.leder[0].ressurs.navIdent} 
        />
      </CustomLinkPanel2>

      {underEnheter.length > 0 && (
        <div className={css({ display: "grid", gridTemplateColumns: underEnheter.length >= 10 ? '1fr 1fr' : '1fr', columnGap: '10px'})}>
          {underEnheter.map((ue: Organisering) => (
            <>
              <Link to={"/org/" + agressoIdDataToUrl(ue.organisasjonsenhet.agressoId, ue.organisasjonsenhet.orgNiv, ue.organisasjonsenhet.navn)} className={linkStyles}>
                <CustomLinkPanel2 type={LinkPanelType.LARGE}>
                  <EnhetInfo
                    title={ue.organisasjonsenhet.navn}
                    lederNavn={ue.organisasjonsenhet.leder[0].ressurs.visningsNavn || "Mangler Navn"}
                    ident={ue.organisasjonsenhet.leder[0].ressurs.navIdent}
                  />
                </CustomLinkPanel2>
              </Link>
            </>
          ))}
        </div>
      )}

      {underEnheter.length < 1 && currentEnhet && (
          <div className={css({ display: "grid", gridTemplateColumns: currentEnhet.koblinger.length >= 10 ? '1fr 1fr' : '1fr', columnGap: '10px'})}>
            {currentEnhet.koblinger.map(k => 
              <CustomLinkPanel2 type={LinkPanelType.KOBLING}>
                <Heading size="medium" className={css({ width: '100%' })}>{k.ressurs.visningsNavn}</Heading>
              </CustomLinkPanel2>
            )}
          </div>
      )}
    </div>
  );
};

export default OrgHierarkiList;
