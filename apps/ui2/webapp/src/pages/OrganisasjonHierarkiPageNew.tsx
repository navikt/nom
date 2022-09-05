import ContentWrapper from '../components/ContentWrapper'
import * as Gq from '../api/generated/graphql_generated'
import {useParams} from 'react-router'
import {agressoUrlToData} from '../resources/utils/url_utils'
import {Alert, BodyShort, Heading} from '@navikt/ds-react'
import {css} from '@emotion/css'
import React, {Fragment, ReactNode} from "react";
import {building2Icon, peopleFilledIcon} from "../components/Images";
import {Link} from 'react-router-dom'
import {Back, Next} from "@navikt/ds-icons";
import UserImage from "../components/UserImage";
import {focusedEnhetHierarchy, focusedRessursHierarchy, Hierarchy} from "../api/transformations/hierarchy";

const colorPalette = {
    rim: "#236B7D",
    focusedBackground: "#DEECEF",
    unfocusedBackground: "#FFFFFF",
    panelBorder: "#B0B0B0",
    font: "#262626",
}

const BuildingSvg = (props: {className? : string}) => <svg className={props.className || ""} width="24" height="24" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path fillRule="evenodd" clipRule="evenodd" d={`M15.6405 1.611C15.6405 1.611 15.6397 1.6095 15.639 1.6095L14.1397 
        0.11025C14.07 0.03975 13.9747 0 13.875 0H4.125C4.02525 0 3.93 0.03975 3.8595 0.11025L2.3595 1.6095C2.35875
        1.61025 2.35875 1.611 2.358 1.611C2.289 1.68225 2.25 1.7955 2.25 1.875V17.625C2.25 17.832 2.418 18 2.625
        18H7.5V14.625C7.5 14.418 7.668 14.25 7.875 14.25H10.125C10.332 14.25 10.5 14.418 10.5
        14.625V18H15.375C15.582 18 15.75 17.832 15.75 17.625V1.875C15.75 1.79625 15.7095 1.67925 15.6405 1.611ZM6
        12.75H3.75V10.5H6V12.75ZM6 9H3.75V6.75H6V9ZM6 5.25H3.75V3H6
        V5.25ZM10.5 12.75H7.5V10.5H10.5V12.75ZM10.5
        9H7.5V6.75H10.5V9ZM10.5 5.25H7.5V3H10.5V5.25ZM14.25 12.75H12V10.5H14.25V12.75ZM14.25 9H12V6.75H14.25V9ZM14.25
        5.25H12V3H14.25V5.25ZM3.53025 1.5L4.28025 0.75H13.7198L14.4698 1.5H3.53025Z`}/>
</svg>

const commonPanelCss = css`
  border-radius: 5px;
  overflow: hidden;
  display: grid;
  min-height: 1rem;
  min-width: 1rem;
  grid-template-columns: min-content 1fr min-content min-content;
  background-color: ${colorPalette.unfocusedBackground};
  align-items: center;
  border: 1px solid ${colorPalette.panelBorder};
`

const panelCss = {
    currentEnhet: css(commonPanelCss, css`
      grid-template-columns: 1fr;
      grid-template-rows: min-content 1fr;
      margin-bottom: 1rem;
      min-height: 13rem;
      background-color: ${colorPalette.focusedBackground};
      svg { // meant for header prefix icon
        fill: ${colorPalette.rim};
        margin: 0 0.2rem 0 0;
      }
    `),
    focusedRessurs: css(css`
      grid-template-columns: 1fr;
      width: 100%;
      //justify-items: center;
      background-color: ${colorPalette.focusedBackground};
      row-gap: 0.5rem;
      min-height: 15rem;
      margin-bottom: 1rem;
      padding: 2rem;
    `),
    underEnhet: css(commonPanelCss, css`
      height: 100%;
      min-height: 5rem;
      word-wrap: anywhere;
    `),
    panelContent: css`
      padding: 1rem;
    `,
    currentEnhetPanelContent: css`
      padding: 0.5rem 2rem;
      display: grid;
      grid-template-columns: 1fr min-content;
    `,
    panelRim: css`
      min-width: 0.5rem;
      min-height: 0.5rem;
      background-color: ${colorPalette.rim};
      display: grid;
      align-items: center;
      justify-items: center;
      height: 100%;
      width: 100%;
      img {
        padding: 0.2rem
      }
    `,
    textCenter: css`
        text-align: center;
    `,
    textRight: css`
        text-align: right;
    `,
    fullHeight: css`height: 100%`,
    labeledItemsPane: css`
      display: grid;
      grid-template-columns: auto;
      column-gap: 1rem;
      row-gap: 1rem;
      width: 100%;
      padding: 1.5rem 0;
      @media (min-width: 80rem) {
        grid-template-columns: auto auto;
        width: auto;
        //span:nth-child(odd){
        //  text-align: right;
        //}
      }

    `
}

const lederStyling = {
    lederTekst: css`
      color: ${colorPalette.font};
      margin-left: 0.3rem
    `,
    lederDiv: css`
      display: flex;
      align-items: center
    `
}

const hierarkiCss = {
    underEnheter: css`
      display: grid;
      grid-template-rows: min-content;
      column-gap: 1rem;
      row-gap: 1rem;
      grid-template-columns: 1fr;
    `,
    link: css`
      text-decoration: none;
      color: inherit;
      display: grid;
    `,
    dynamicSecondColumn: css`
      @media (min-width:  60rem) {
        grid-template-columns: 1fr 1fr;
      }`,
}


function OverenheterDivider() {
  return <div className={css`
    background-color: inherit;
    display: grid;
    justify-items: center;
  `}>
    <div className={css`
      min-height: 1rem;
      height: 1rem;
      background-color: ${colorPalette.rim};
      width: 0.08rem;
    `}>
    </div>
  </div>
}

const HorizontallyAlignedPanel = (props: { children: ReactNode, iconSrc: string, className: string, navidentForImg?: string, fornavnAndEtternavnForImg?: string }) =>
    <div className={props.className}>
        <div className={panelCss.panelRim}>
            {props.iconSrc && <img src={props.iconSrc} alt={"src="+props.iconSrc}/>}
        </div>
        <div className={panelCss.panelContent}>{props.children}</div>
        {props.navidentForImg && <UserImage navIdent={props.navidentForImg} navn={props.fornavnAndEtternavnForImg}  />}
        <Next height={undefined} width={undefined} className={css`margin: 0 1rem 0 0.2rem; height: 2rem; width: 2rem;`}/>
    </div>

const Enhet = (props: { children: ReactNode, navIdentForImg: string, fornavnAndEtternavnForImg?: string }) => <HorizontallyAlignedPanel
    className={panelCss.underEnhet} iconSrc={building2Icon} navidentForImg={props.navIdentForImg} fornavnAndEtternavnForImg={props.fornavnAndEtternavnForImg}>
    {props.children}
</HorizontallyAlignedPanel>

const Ressurs = (props: { children: ReactNode }) => <HorizontallyAlignedPanel
    className={css(commonPanelCss, panelCss.fullHeight)} iconSrc={peopleFilledIcon}>{props.children}
</HorizontallyAlignedPanel>

const OrgenhetLederLink = (props: {navident: string, name:string, orgenhetCtxUrl: string}) => <Link className={lederStyling.lederTekst}
    to={`${props.orgenhetCtxUrl}/${props.navident}`}
>{props.name}</Link>

const BackFromRessurs = (props: {valgtEnhetUrl: string, ressursIsLeder?: boolean}) => <Link
    className={css`display: grid; grid-template-columns: min-content auto;`}
    to={props.valgtEnhetUrl}>
    <Back className='navds-link-panel__chevron' color={colorPalette.font} />
    <BodyShort size='medium' className={css({ fontWeight: '600', color: colorPalette.font })}>
        {props.ressursIsLeder ? "Se alle underenhetene for enheten " : "Se alle ressurser i enheten"}
    </BodyShort>
</Link>

const scrollToRef = (ref: any) =>
    window.scrollTo(0, ref.current.offsetTop - 100)

const OrganisasjonHierarkiPageInner = function (props: {
    data: Hierarchy.OrgHierarchy,
    focusedRessurs?: Hierarchy.OrgHierarchyWithFocusedRessurs
}) {
    const overEnheter = props.focusedRessurs?.overEnheter || props.data.overEnheter;
    const scrollRef = React.useRef<any>();
    React.useEffect(() => {
        if(overEnheter.length !== 0) scrollRef.current && scrollToRef(scrollRef);
    },[])

    const underEnheter = props.data.underEnheter
    const valgtEnhet = props.data.enhet;
    const ressurser = props.data.ressurser;

    const focusedRessurs = props.focusedRessurs?.focusedRessurs
    const valgtEnhetUrl = valgtEnhet.url;



    return <div>
        {overEnheter.map(oe => {
                return <Fragment key={oe.url}><Link className={hierarkiCss.link}  to={oe.url}>
                    <Enhet navIdentForImg={oe.ledere[0].navident} fornavnAndEtternavnForImg={oe.ledere[0].name}>
                        <Heading size={'medium'}>
                            {oe.name}
                        </Heading>
                        <div>
                            Leder: <span
                            className={lederStyling.lederTekst}>{oe.ledere[0]?.name || "Leder ikke funnet"}</span>
                        </div>
                    </Enhet>
                </Link>
                <OverenheterDivider/>
                </Fragment>
            }
        )}

      {focusedRessurs && <>
          <div ref={scrollRef} className={panelCss.currentEnhet}>
              <div className={panelCss.panelRim}/>
              <div className={css`display: grid;
                grid-template-columns: 1fr min-content`}>
                  <div className={panelCss.focusedRessurs}>
                      <Heading size={'large'}>
                          {focusedRessurs.name}
                      </Heading>
                      <div>{focusedRessurs.sektor} <strong>({focusedRessurs.navident})</strong></div>

                      <div className={panelCss.labeledItemsPane}>

                          <span>E-post: <strong>{focusedRessurs.email}</strong></span>
                          <span>Tlf: <strong>{focusedRessurs.tel.length === 0 ? "Mangler tlf" :
                              <span>{focusedRessurs.tel}</span>}</strong>
                        </span>
                          {focusedRessurs.tilknytninger.map(tilkn => {
                              const {orgenhet: oe, leder: l} = tilkn
                              return <Fragment key={l.navident + "_" + oe.url}>
                                      <span>Org.enhet:
                                      <Link className={lederStyling.lederTekst} to={oe.url}><strong>{oe.name}</strong></Link>
                                      </span>
                                              <span>Leder:<Link className={lederStyling.lederTekst}
                                                                to={l.url}><strong>{l.name}</strong></Link>
                                          </span>
                              </Fragment>
                          })}

                      </div>
                  </div>
                  <div className={css`padding: 2.5rem`}>
                  <UserImage size={"130px"} navIdent={focusedRessurs.navident} navn={focusedRessurs.name}/>
                  </div>
              </div>
          </div>
          <BackFromRessurs valgtEnhetUrl={valgtEnhetUrl} ressursIsLeder={ressurser.length === 0} />

      </>}
      {!focusedRessurs && <>
          {valgtEnhet && <div ref={scrollRef} className={panelCss.currentEnhet}>
              <div className={panelCss.panelRim}></div>
              <div className={panelCss.currentEnhetPanelContent}>
              <div className={css`display: grid; align-content: center`}>
                <Heading size={'large'}>
                    <BuildingSvg />
                    {valgtEnhet.name}
                </Heading>
                <div className={lederStyling.lederDiv}>
                    <span>Leder: <OrgenhetLederLink navident={valgtEnhet.ledere[0].navident} orgenhetCtxUrl={valgtEnhetUrl} name={valgtEnhet.ledere[0]?.name} /></span>
                </div>
              </div>
                  <UserImage size={"130px"} navIdent={valgtEnhet.ledere[0]?.navident} navn={valgtEnhet.ledere[0]?.name} />
              </div>
          </div>}
          <div className={css(hierarkiCss.underEnheter, underEnheter.length > 5 && hierarkiCss.dynamicSecondColumn)}>
              {underEnheter.map(ue =>
                  <Link className={hierarkiCss.link} key={ue.url} to={ue.url}>
                      <Enhet navIdentForImg={ue.ledere[0].navident}>
                          <Heading size={'medium'}>
                              {ue.name}
                          </Heading>
                          <span>
                              Leder: <span className={lederStyling.lederTekst}>{ue.ledere[0]?.name || "Leder ikke funnet"}</span>
                          </span>
                      </Enhet>
                  </Link>
              )}
          </div>
          <div className={css(hierarkiCss.underEnheter, ressurser.length > 5 && hierarkiCss.dynamicSecondColumn)}>
              {ressurser.map(it =>
                  <Link className={hierarkiCss.link} key={it.navident}
                        to={valgtEnhetUrl + "/" + it.navident}><Ressurs>
                      <Heading size={"medium"}
                               className={css`font-size: 24px; font-weight: 600;`}>{it.name}</Heading></Ressurs>
                  </Link>
              )}
          </div>
      </>
      }
    </div>
}

function OrganisasjonsHierarkiPageOuter(){
    const {id, resourceId} = useParams()
    const agrId = agressoUrlToData(id || '')

    const qry = Gq.useHentHierarkiQuery({
        variables: {
            agressoId: agrId.agressoId,
            orgNiv: agrId.agressoNiva,
        }, errorPolicy: "all",
    })

    const ressursQry = Gq.useHentRessursQuery({
        variables: {
            navident: resourceId
        },
        skip: !resourceId
    })

    const gotHierarkiData = !!(qry.data && !qry.loading)
    const gotRessursData = !resourceId || !!(ressursQry.data?.ressurs && !ressursQry.loading)

    if(qry.error){
        console.error(qry.error, "GRAPHQL ERROR")
    }
    if(ressursQry.error){
        console.error(ressursQry.error, "GRAPHQL ERROR")
    }

    const enhetHierarchy = gotHierarkiData ? focusedEnhetHierarchy(qry) : null;
    const enhetHierarchyOk = enhetHierarchy && enhetHierarchy.kind !== "err"

    const focusedResourceHierarchy = resourceId && gotRessursData && enhetHierarchyOk ? focusedRessursHierarchy(enhetHierarchy,ressursQry) : null
    const focusedResourceHierarchyOk = focusedResourceHierarchy && focusedResourceHierarchy?.kind !== "err"

    const missingData = !qry.loading && !ressursQry.loading && (!gotRessursData || !gotRessursData)

    return <ContentWrapper>
        <div className={css`display: grid`}>
            <Heading size='xlarge' className={css({marginBottom: '20px'})}>
                NAV Organisasjon
            </Heading>
            {(qry.error || ressursQry.error) && <Alert variant={"warning"}>Feil i graphql-respons</Alert>}
            {!gotHierarkiData && !gotRessursData && <>
                <div>Loading...</div>
            </>}
            {missingData && <Alert variant={"warning"}>Fant ikke data på IDer <br/>{JSON.stringify(resourceId ? {orgenhetId: id,ressursId: resourceId} : {orgenhetId: id},null,2)}</Alert>}
            {gotHierarkiData && gotRessursData && enhetHierarchyOk &&
                <OrganisasjonHierarkiPageInner data={enhetHierarchy}
                                               focusedRessurs={focusedResourceHierarchyOk ? focusedResourceHierarchy: undefined}
                />}
        </div>
    </ContentWrapper>
}


export default OrganisasjonsHierarkiPageOuter;