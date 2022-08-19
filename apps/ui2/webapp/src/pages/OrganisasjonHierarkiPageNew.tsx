import ContentWrapper from '../components/ContentWrapper'
import * as Gq from '../api/generated/graphql_generated'
import {useParams} from 'react-router'
import {agressoIdDataToUrl, agressoUrlToData} from '../resources/utils/url_utils'
import {BodyShort, Heading, Loader} from '@navikt/ds-react'
import {css} from '@emotion/css'
import React, {Fragment, ReactNode} from "react";
import {building2Icon, peopleFilledIcon} from "../components/Images";
import { Link } from 'react-router-dom'
import {Back, Next} from "@navikt/ds-icons";
import UserImage from "../components/UserImage";

function structuredClone<T>(arg: T): T{
  // @ts-ignore
  const cloneFunc = window.structuredClone;
  if(cloneFunc){
    return cloneFunc(arg)
  }else{
    return JSON.parse(JSON.stringify(arg))
  }
}

const colorPalette = {
    rim: "#236B7D",
    focusedBackground: "#DEECEF",
    unfocusedBackground: "#FFFFFF",
    panelBorder: "#333333",
    font: "#262626",
    border: "#B0B0B0"
}

const BuildingSvg = (props: {className? : string}) => <svg className={props.className || ""} width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg">
    <path fill-rule="evenodd" clip-rule="evenodd" d={`M15.6405 1.611C15.6405 1.611 15.6397 1.6095 15.639 1.6095L14.1397 
        0.11025C14.07 0.03975 13.9747 0 13.875 0H4.125C4.02525 0 3.93 0.03975 3.8595 0.11025L2.3595 1.6095C2.35875
        1.61025 2.35875 1.611 2.358 1.611C2.289 1.68225 2.25 1.7955 2.25 1.875V17.625C2.25 17.832 2.418 18 2.625
        18H7.5V14.625C7.5 14.418 7.668 14.25 7.875 14.25H10.125C10.332 14.25 10.5 14.418 10.5
        14.625V18H15.375C15.582 18 15.75 17.832 15.75 17.625V1.875C15.75 1.79625 15.7095 1.67925 15.6405 1.611ZM6
        12.75H3.75V10.5H6V12.75ZM6 9H3.75V6.75H6V9ZM6 5.25H3.75V3H6
        V5.25ZM10.5 12.75H7.5V10.5H10.5V12.75ZM10.5
        9H7.5V6.75H10.5V9ZM10.5 5.25H7.5V3H10.5V5.25ZM14.25 12.75H12V10.5H14.25V12.75ZM14.25 9H12V6.75H14.25V9ZM14.25
        5.25H12V3H14.25V5.25ZM3.53025 1.5L4.28025 0.75H13.7198L14.4698 1.5H3.53025Z`}/>
</svg>


function extractSortAndFlattenHierarkiData(qryRes: Gq.HentHierarkiQueryResult){
  if(!qryRes.data) {
    throw "Wrong usage"
  }
  const qryData = structuredClone(qryRes.data);

  const flattenedOverenheter = function DO(){
    const extractOverEnhet = () => qryData.enhetsKoblinger?.overenheter[0]?.organisasjonsenhet!!
    let currentOverenhet = extractOverEnhet()
    let out: ReturnType<typeof extractOverEnhet>[] = []
    while(currentOverenhet){
      out = [currentOverenhet!!, ...out]
      currentOverenhet = currentOverenhet?.organiseringer[0]?.organisasjonsenhet!! as any
    }
    return out
  }()

  qryData.enhetsKoblinger?.underenheter.sort((a, b) =>
      (a?.organisasjonsenhet?.navn || '').localeCompare(b?.organisasjonsenhet?.navn || '', "no"))
  qryData.ressursKoblinger?.koblinger.sort((a, b) =>
      (a.ressurs.visningsNavn || '').localeCompare(b.ressurs.visningsNavn || '', "no"))

  return {
    enhet: qryData.enhet!!,
    underenheter: qryData.enhetsKoblinger?.underenheter.map(it => it.organisasjonsenhet!!) || [],
    overenheter: flattenedOverenheter || [],
    ledere: qryData.lederkoblinger?.leder || [],
      ressurser: qryData.ressursKoblinger?.koblinger.map(it => it.ressurs) || []
  }
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
    const gotData = !!(qry.data && !qry.loading && !qry.error)

    return <ContentWrapper><div className={css`display: grid`}>
        <Heading size='xlarge' className={css({ marginBottom: '20px' })}>
            NAV Organisasjon
        </Heading>
        {!gotData && <>
            <div>Loading...</div>
            <pre>{JSON.stringify(qry.error, null, 2)}</pre>
        </>}
        {gotData && <OrganisasjonHierarkiPageInner data={extractSortAndFlattenHierarkiData(qry)} focusedNavIdent={resourceId}/>}
    </div>
    </ContentWrapper>
}


const commonPanelCss = css`
  border-radius: 5px;
  overflow: hidden;
  display: grid;
  min-height: 1rem;
  min-width: 1rem;
  grid-template-columns: min-content 1fr min-content min-content;
  align-items: center;
  border: 1px solid ${colorPalette.panelBorder};
  background-color: ${colorPalette.unfocusedBackground};
`

const panelCss = {
    currentEnhet: css(commonPanelCss, css`
      grid-template-columns: 1fr;
      grid-template-rows: min-content 1fr;
      margin-bottom: 2rem;
      min-height: 13rem;
      background-color: ${colorPalette.focusedBackground};
      svg { // meant for header prefix icon
        fill: ${colorPalette.rim};
        margin: 0 0.2rem 0 0;
      }
    `),
    focusedRessurs: css(commonPanelCss, css`
      grid-template-columns: 1fr;
      width: 100%;
      justify-items: center;
      background-color: ${colorPalette.focusedBackground};
      min-height: 15rem;
      margin-bottom: 2rem;
    `),
    underEnhet: css(commonPanelCss, css`
      height: 100%;
      min-height: 9rem;
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
    labeledItemsPane: css`
      display: grid;
      column-gap: 1rem;
      row-gap: 1rem;
    `
}

function OverenheterDivider() {
  return <div className={css`
    background-color: inherit;
    display: grid;
    justify-items: center;
  `}>
    <div className={css`
      min-height: 1rem;
      background-color: ${colorPalette.rim};
      width: 0.2rem;
    `}>
    </div>
  </div>
}

const HorizontallyAlignedPanel = (props: { children: ReactNode, iconSrc: string, className: string, navidentForImg?: string }) =>
    <div className={props.className}>
        <div className={panelCss.panelRim}>
            {props.iconSrc && <img src={props.iconSrc}/>}
        </div>
        <div className={panelCss.panelContent}>{props.children}</div>
        {props.navidentForImg && <UserImage navIdent={props.navidentForImg}/>}
        <Next className={css`margin: 0 1rem 0 0.2rem`}/>
    </div>

const UnderEnhet = (props: { children: ReactNode, navIdentForImg: string }) => <HorizontallyAlignedPanel
    className={panelCss.underEnhet} iconSrc={building2Icon} navidentForImg={props.navIdentForImg}>
    {props.children}
</HorizontallyAlignedPanel>

const OverEnhet = (props: {children: ReactNode, navIdentForImg: string}) => <div>
    <UnderEnhet navIdentForImg={props.navIdentForImg}>{props.children}</UnderEnhet>
    <OverenheterDivider/>
</div>

const Ressurs = (props: { children: ReactNode }) => <HorizontallyAlignedPanel
    className={commonPanelCss} iconSrc={peopleFilledIcon}>{props.children}
</HorizontallyAlignedPanel>


function extractLinkFromEnhet(enhet: {agressoId: string, navn: string, orgNiv: string}){
    return "/org2/" + agressoIdDataToUrl(enhet.agressoId, enhet.orgNiv, enhet.navn)
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
    `,
    dynamicSecondColumn: css`
      @media (min-width:  60rem) {
        grid-template-columns: 1fr 1fr;
      }`,
}

const LederLink = (props: {leder: {navIdent: string, visningsNavn?: string | null}, enhetUrlCtx: string}) =>
    <Link to={`/org2/${props.enhetUrlCtx}/${props.leder.navIdent}`}>{props.leder.visningsNavn} ({props.leder.navIdent})</Link>

const BackFromRessurs = (props: {valgtEnhetUrl: string, ressursIsLeder?: boolean}) => <Link
    className={css`display: grid; grid-template-columns: min-content auto;`}
    to={"/org2/" + props.valgtEnhetUrl}>
    <Back className='navds-link-panel__chevron' />
    <BodyShort size='medium' className={css({ fontWeight: '600' })}>
        {props.ressursIsLeder ? "Se alle underenhetene for enheten " : "Se alle ressurser i enheten"}
    </BodyShort>
</Link>

const scrollToRef = (ref: any) =>
    window.scrollTo(0, ref.current.offsetTop - 100)

const OrganisasjonHierarkiPageInner = function (props: { data: ReturnType<typeof extractSortAndFlattenHierarkiData>, focusedNavIdent?: string }) {
    const overEnheter = props.data.overenheter;
    const scrollRef = React.useRef<any>();
    React.useEffect(() => {
        if(overEnheter.length !== 0) scrollRef.current && scrollToRef(scrollRef);
    },[])

    const underEnheter = props.data.underenheter
    const lederKoblinger = props.data.ledere
    const valgtEnhet = props.data.enhet;
    const ressurser = props.data.ressurser;

    const focusedRessurs = props.focusedNavIdent && ressurser.find(it => it.navIdent === props.focusedNavIdent)
        || lederKoblinger.find(it => it.ressurs.navIdent === props.focusedNavIdent)?.ressurs
        || null;
    const valgtEnhetUrl = agressoIdDataToUrl(valgtEnhet.agressoId, valgtEnhet.orgNiv, valgtEnhet.navn);

    return <div>
      {overEnheter.map(it =>{
          const linkPath = extractLinkFromEnhet(it)
                return <Link className={hierarkiCss.link} key={linkPath} to={linkPath }>
                    <OverEnhet navIdentForImg={it.leder[0].ressurs.navIdent}>
                        <Heading size={'medium'}>
                            {it.navn}
                        </Heading>
                        Leder: {it.leder[0]?.ressurs.visningsNavn || "Leder ikke funnet"}
                    </OverEnhet>
                </Link>}
        )}

      {focusedRessurs && <>
          <Link className={hierarkiCss.link} to={valgtEnhetUrl}>
              <OverEnhet navIdentForImg={lederKoblinger[0]?.ressurs?.navIdent}>
                  <Heading size={'medium'}>
                      {valgtEnhet.navn}
                  </Heading>
                  Leder: {lederKoblinger[0]?.ressurs?.visningsNavn || "Leder ikke funnet"}
              </OverEnhet>
          </Link>
          <div ref={scrollRef} className={panelCss.focusedRessurs}>
              <div className={panelCss.panelRim}></div>
                  <UserImage size={"130px"} navIdent={focusedRessurs.navIdent}/>
                  <Heading className={panelCss.textCenter} size={'large'}>
                      {focusedRessurs.visningsNavn}
                  </Heading>
              <div>({focusedRessurs.navIdent})</div>
              <div className={panelCss.currentEnhetPanelContent}>
                  <div className={panelCss.labeledItemsPane}>
                      <div>Epost: <strong>{focusedRessurs.epost}</strong></div>
                      <div>Org.enhet: <strong>{valgtEnhet.navn}</strong></div>
                      <div>Leder: <LederLink leder={lederKoblinger[0]?.ressurs!!} enhetUrlCtx={valgtEnhetUrl}/></div>
                      <div>Tel: <strong>{focusedRessurs.telefon[0]?.nummer || "Mangler tlf"}</strong></div>
                  </div>

              </div>
          </div>
          <BackFromRessurs valgtEnhetUrl={valgtEnhetUrl} ressursIsLeder={ressurser.length === 0} />

      </>}
      {!focusedRessurs && <>
          {valgtEnhet && <div ref={scrollRef} className={panelCss.currentEnhet}>
              <div className={panelCss.panelRim}></div>
              <div className={panelCss.currentEnhetPanelContent}>
              <span>
              <Heading size={'large'}>
                  <BuildingSvg/>
                  {valgtEnhet.navn}
              </Heading>
              Leder: <LederLink leder={lederKoblinger[0]?.ressurs!!} enhetUrlCtx={valgtEnhetUrl}/>
              </span>
                  <UserImage size={"130px"} navIdent={lederKoblinger[0]?.ressurs?.navIdent}/>
              </div>
          </div>}
          <div className={css(hierarkiCss.underEnheter, underEnheter.length > 5 && hierarkiCss.dynamicSecondColumn)}>
              {underEnheter.map(it =>
                  <Link className={hierarkiCss.link} key={extractLinkFromEnhet(it)} to={extractLinkFromEnhet(it)}>
                      <UnderEnhet navIdentForImg={it.leder[0].ressurs.navIdent}>
                          <Heading size={'medium'}>
                              {it.navn}
                          </Heading>
                          Leder: {it.leder[0]?.ressurs.visningsNavn || "Leder ikke funnet"}
                      </UnderEnhet>
                  </Link>
              )}
          </div>
          <div className={css(hierarkiCss.underEnheter, ressurser.length > 5 && hierarkiCss.dynamicSecondColumn)}>
              {ressurser.map(it =>
                  <Link className={hierarkiCss.link} key={it.navIdent}
                        to={"/org2/" + valgtEnhetUrl + "/" + it.navIdent}><Ressurs>{it.visningsNavn}</Ressurs>
                  </Link>)}
          </div>
      </>
      }
    </div>
}

export default OrganisasjonsHierarkiPageOuter;