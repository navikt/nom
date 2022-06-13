import { css } from "@emotion/css";
import { Link } from "react-router-dom";
import { CurrentEnhet } from "../../pages/OrganisasjonHierarkiPage";
import { agressoIdDataToUrl } from "../../resources/utils/url_utils";
import CustomLinkPanel2, { LinkPanelType } from "../CustomLinkPanel2";
import EnhetInfo from "./EnhetInfo";
import { Organisering, Ressurs } from "../../api/generated/graphql_generated";
import { BodyShort, Heading } from "@navikt/ds-react";
import RessursInfo from "./RessursInfo";
import { Back } from "@navikt/ds-icons";

const linkStyles = css({ textDecoration: "none", color: "#262626" });
const ressursLinkStyles = css({ 
  display: 'flex',
  marginTop: '30px',
  fontWeight: '600',
  textDecoration: 'none',
  color: '#262626'
})
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
  ressurs?: Ressurs
};

const OrgHierarkiList = (props: OrgHierarkiListProps) => {
  const { currentEnhet, underEnheter, overEnheter, ressurs} = props
  
  return (
    <div>
      <div>
        {/* Liste av alle overenheter */}
        {overEnheter.map((oe: Organisering) => (
                  <div key={agressoIdDataToUrl(oe.organisasjonsenhet.agressoId, oe.organisasjonsenhet.orgNiv, oe.organisasjonsenhet.navn)}>
                    <Link to={"/org/" + agressoIdDataToUrl(oe.organisasjonsenhet.agressoId, oe.organisasjonsenhet.orgNiv, oe.organisasjonsenhet.navn)} className={linkStyles}>
                      <CustomLinkPanel2 type={LinkPanelType.OVERENHET}>
                        <EnhetInfo
                          title={oe.organisasjonsenhet.navn}
                          lederNavn={oe.organisasjonsenhet.leder[0]?.ressurs?.visningsNavn || "Mangler Navn"}
                          overenhet
                          type={LinkPanelType.OVERENHET}
                        />
                      </CustomLinkPanel2>
                    </Link>
                    <Divider />
                  </div>
                ))
        }
      </div>
      
      {/* Den valgte enheten */}
      <CustomLinkPanel2 type={LinkPanelType.CURRENT}>
        <EnhetInfo 
            title={currentEnhet?.navn || 'Feil i henting av navn'} 
            lederNavn={currentEnhet?.leder[0]?.ressurs?.visningsNavn || "Mangler navn"}
            ident={currentEnhet?.leder[0]?.ressurs?.navIdent} 
            type={LinkPanelType.CURRENT}
        />
      </CustomLinkPanel2>
      

      {/* Liste av underenheter */}
      {underEnheter.length > 0 && (
        <div className={css({ display: "grid", gridTemplateColumns: underEnheter.length > 5 ? '1fr 1fr' : '1fr', columnGap: '10px'})}>
          {underEnheter.map((ue: Organisering) => (
            <div key={agressoIdDataToUrl(ue.organisasjonsenhet.agressoId, ue.organisasjonsenhet.orgNiv, ue.organisasjonsenhet.navn)}>
              <Link to={"/org/" + agressoIdDataToUrl(ue.organisasjonsenhet.agressoId, ue.organisasjonsenhet.orgNiv, ue.organisasjonsenhet.navn)} className={linkStyles}>
                <CustomLinkPanel2 type={LinkPanelType.LARGE}>
                  <EnhetInfo
                    title={ue.organisasjonsenhet.navn}
                    lederNavn={ue.organisasjonsenhet.leder[0]?.ressurs?.visningsNavn || "Mangler Navn"}
                    ident={ue.organisasjonsenhet.leder[0]?.ressurs?.navIdent}
                    type={LinkPanelType.LARGE}
                  />
                </CustomLinkPanel2>
              </Link>
            </div>
          ))}
        </div>
      )}

      {/* Liste av ressurser */}
      {underEnheter.length < 1 && currentEnhet && !ressurs && (
          <div className={css({ display: "grid", gridTemplateColumns: currentEnhet.koblinger.length > 5 ? '1fr 1fr' : '1fr', columnGap: '10px'})}>
            {currentEnhet.koblinger.map(k =>
              <Link to={"/org/" + agressoIdDataToUrl(currentEnhet.agressoId, currentEnhet.orgNiv, currentEnhet.navn) + `/${k.ressurs.navIdent}`} className={linkStyles}>
                  <CustomLinkPanel2 type={LinkPanelType.KOBLING}>
                    <Heading size="medium" className={css({ width: '100%' })}>{k.ressurs?.visningsNavn || 'Mangler navn'}</Heading>
                  </CustomLinkPanel2>
              </Link> 
              
            )}
          </div>
      )}

      {/* Visning av en ressurs */}
      {underEnheter.length < 1 && currentEnhet && ressurs && (
            <>
              <RessursInfo ressurs={ressurs}/>

              <Link to={"/org/" + agressoIdDataToUrl(currentEnhet.agressoId, currentEnhet.orgNiv, currentEnhet.navn)}>
                  <div className={ressursLinkStyles}>
                      <Back className="navds-link-panel__chevron"/>
                      <BodyShort size="medium">Se alle ressurser i {currentEnhet?.navn}</BodyShort>
                  </div>
              </Link>
            </>
      )}
    </div>
  );
}

export default OrgHierarkiList;
