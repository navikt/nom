import { css } from "@emotion/css";
import { Link } from "react-router-dom";
import { CurrentEnhet } from "../../pages/OrganisasjonHierarkiPage";
import { agressoIdDataToUrl } from "../../resources/utils/url_utils";
import CustomLinkPanel2, { LinkPanelType } from "../CustomLinkPanel2";
import EnhetInfo from "./EnhetInfo";
import { OrganisasjonsenhetsLeder, Organisering, Ressurs } from "../../api/generated/graphql_generated";
import { BodyShort, Heading } from "@navikt/ds-react";
import RessursInfo from "./RessursInfo";
import { Back } from "@navikt/ds-icons";
import { sortItems } from "../../resources/utils/utils";
import { useEffect, useRef } from "react";

const scrollToRef = (ref: any) => window.scrollTo(0, ref.current.offsetTop - 100) 

const linkStyles = css({ textDecoration: "none", color: "#262626" })
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
  const refDiv = useRef(null)
  const { currentEnhet, underEnheter, overEnheter, ressurs} = props

  const sortedUnderEnheter = underEnheter.length > 0 ? [...underEnheter].sort((a, b) => sortItems(a.organisasjonsenhet.navn, b.organisasjonsenhet.navn)) : []
  const sortedKoblinger = currentEnhet ? [...currentEnhet.koblinger].sort((a, b) => sortItems(a.ressurs.visningsNavn || '', b.ressurs.visningsNavn || '')) : []

  const getFornavnAndEtternavnForLeder = (leder: OrganisasjonsenhetsLeder) => {
    if (!leder) return 'Fant ikke leder'
    return (leder.ressurs.fornavn || '') + ' ' + (leder.ressurs.etternavn || '') 
  }

  useEffect(() => {
    scrollToRef(refDiv) 
  }, [])

  console.log(currentEnhet?.leder, "LEDERRRR")

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
                          lederNavn={getFornavnAndEtternavnForLeder(oe.organisasjonsenhet.leder[0]) || "Mangler Navn"}
                          type={LinkPanelType.OVERENHET}
                          leder={oe.organisasjonsenhet.leder[0].ressurs as Ressurs}
                        />
                      </CustomLinkPanel2>
                    </Link>
                    <Divider />
                  </div>
                ))
        }
      </div>
      
      {/* Den valgte enheten - når det ikke vises et ressurskort*/}
      {currentEnhet && !ressurs && (
          <div ref={refDiv}>
            <CustomLinkPanel2 type={LinkPanelType.CURRENT}>
              <EnhetInfo 
                  title={currentEnhet?.navn || 'Feil i henting av navn'} 
                  lederNavn={currentEnhet ? getFornavnAndEtternavnForLeder(currentEnhet?.leder[0] as OrganisasjonsenhetsLeder) : 'Mangler navn'}
                  ident={currentEnhet?.leder[0]?.ressurs?.navIdent}
                  type={LinkPanelType.CURRENT}
                  leder={currentEnhet?.leder[0].ressurs as Ressurs}
              />
            </CustomLinkPanel2>
          </div>
      )}
      
      {/* Den valgte enheten - vises som en overenhet når det vises et ressurskort - gjøres om til en lenke siden den ikke finnses i overenhetlista */}
      {currentEnhet && ressurs && (
        <Link to={"/org/" + agressoIdDataToUrl(currentEnhet.agressoId, currentEnhet.orgNiv, currentEnhet.navn)} className={linkStyles}>
            <CustomLinkPanel2 type={LinkPanelType.OVERENHET}>
              <EnhetInfo 
                  title={currentEnhet?.navn || 'Feil i henting av navn'} 
                  lederNavn={currentEnhet ? getFornavnAndEtternavnForLeder(currentEnhet?.leder[0] as OrganisasjonsenhetsLeder) : 'Mangler navn'}
                  type={LinkPanelType.OVERENHET}
                  leder={currentEnhet?.leder[0].ressurs as Ressurs}
              />
            </CustomLinkPanel2>
          </Link>
      )}

      {/* Liste av underenheter */}
      {underEnheter.length > 0 && (
        <div className={css({ display: "grid", gridTemplateColumns: underEnheter.length > 5 ? '1fr 1fr' : '1fr', columnGap: '10px'})}>
          {sortedUnderEnheter.map((ue: Organisering) => (
            <div key={agressoIdDataToUrl(ue.organisasjonsenhet.agressoId, ue.organisasjonsenhet.orgNiv, ue.organisasjonsenhet.navn)}>
              <Link to={"/org/" + agressoIdDataToUrl(ue.organisasjonsenhet.agressoId, ue.organisasjonsenhet.orgNiv, ue.organisasjonsenhet.navn)} className={linkStyles}>
                <CustomLinkPanel2 type={LinkPanelType.LARGE}>
                  <EnhetInfo
                    title={ue.organisasjonsenhet.navn}
                    lederNavn={getFornavnAndEtternavnForLeder(ue.organisasjonsenhet.leder[0])}
                    ident={ue.organisasjonsenhet.leder[0]?.ressurs?.navIdent}
                    type={LinkPanelType.LARGE}
                    leder={ue.organisasjonsenhet.leder[0].ressurs as Ressurs}
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
            {sortedKoblinger.map(k =>
              <Link to={"/org/" + agressoIdDataToUrl(currentEnhet.agressoId, currentEnhet.orgNiv, currentEnhet.navn) + `/${k.ressurs.navIdent}`} className={linkStyles}>
                  <CustomLinkPanel2 type={LinkPanelType.KOBLING}>
                    <Heading size="medium" className={css({ width: '100%' })}>
                        {k.ressurs?.fornavn && k.ressurs?.etternavn ? (k.ressurs.fornavn + ' ' + k.ressurs.etternavn) : 'Mangler navn'}
                    </Heading>
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
                      <BodyShort size="medium" className={css({fontWeight: '600'})}>Se alle ressurser i {currentEnhet?.navn}</BodyShort>
                  </div>
              </Link>
            </>
      )}
    </div>
  );
}

export default OrgHierarkiList;
