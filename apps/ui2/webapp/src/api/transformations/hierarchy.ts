import * as Gq from '../generated/graphql_generated'
import {agressoIdDataToUrl} from "../../resources/utils/url_utils";
import {funcIsNotDuplicateByKey} from "../../resources/utils/utils";

namespace ApiTypes {
    export type ApiRessurs = Pick<Gq.Ressurs,"navident" | "fornavn" | "etternavn">
        & Pick<Gq.RessursLeder["ressurs"],"fornavn" | "navident" | "etternavn">

    export type ApiEnhet = Pick<Gq.OrgEnhet,"orgNiv" | "agressoId" | "navn">

}

export namespace Hierarchy {

    export type OrgEnhet = {
        name: string
        ledere: Ressurs[]
        url: string
    }

    export type Ressurs = {
        name: string
        navident: string
        url: string
    }

    export type FocusedRessurs = {
        tel: string
        email: string
        ledere: {
            leder: Ressurs,
            orgenhet: Omit<OrgEnhet, "ledere">
        }[]
    } & Ressurs

    export type OrgHierarchy = {
        kind: "hierarchy"
        enhet: OrgEnhet,
        overEnheter: OrgEnhet[],
        underEnheter: OrgEnhet[],
        ressurser: Ressurs[]
    }

    export type OrgHierarchyWithFocusedRessurs = {
        kind: "hierarchy_focused_ressurs"
        overEnheter: OrgEnhet[],
        focusedRessurs: FocusedRessurs
    }

    export type Err = {
        kind: "err",
        ctx?: any
        msg: string,
    }

}

function nameFromRessurs(ressurs: ApiTypes.ApiRessurs) {
    if (ressurs.fornavn && ressurs.etternavn) {
        return `${ressurs.fornavn} ${ressurs.etternavn}`
    }
    return "Uten navn"
}

function gqlEnhetToUrl(enhet: ApiTypes.ApiEnhet){
    return "/org2/" + agressoIdDataToUrl(enhet?.agressoId, enhet?.orgNiv, enhet?.navn)
}

function translateRessurs(ress: ApiTypes.ApiRessurs, enhetCtx: ApiTypes.ApiEnhet): Hierarchy.Ressurs {
    const name = (ress.fornavn && ress.etternavn) ? `${ress.fornavn} ${ress.etternavn}` : "Uten navn";
    return {
        name,
        url: gqlEnhetToUrl(enhetCtx) + "/" + ress.navident,
        navident: ress.navident,
    }
}

function formatTlf(nr: string){
    const tmpNr = nr.startsWith("47") && nr.length === 10  ? nr.substring(2) : nr;
    let out = []
    for(let k = 0; k < 8; k+= 2){
        out.push(tmpNr.substring(k,k+2))
    }
    return out.join(" ");
}


export function focusedEnhetHierarchy(qryRes: Gq.HentHierarkiQueryResult): Hierarchy.OrgHierarchy | Hierarchy.Err{
    if(!qryRes.data) {
        throw "Wrong usage focusedenhethierarchy"
    }

    const qryData = structuredClone(qryRes.data)

    if(!qryData.enhet) return {kind: "err",msg: "mangler enhet" }

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
        (a.ressurs.fornavn || '').localeCompare(b.ressurs.fornavn || '', "no"))

    const focusedEnhetUrl = gqlEnhetToUrl(qryData.enhet)

    const underenheter = (!qryData.enhetsKoblinger?.underenheter) ? [] :
        qryData.enhetsKoblinger.underenheter.filter(funcIsNotDuplicateByKey(it => gqlEnhetToUrl(it.organisasjonsenhet)))

    const ressurser = (!qryData.ressursKoblinger?.koblinger) ? [] :
        qryData.ressursKoblinger?.koblinger.filter(funcIsNotDuplicateByKey(it => it.ressurs.navident))


    return {
        kind: "hierarchy",
        enhet: {
            name: qryData.enhet?.navn || "Mangler enhets-navn",
            url: focusedEnhetUrl,
            ledere: qryData.enhet.leder.map(it => ({
                name: nameFromRessurs(it.ressurs),
                navident: it.ressurs.navident,
                url: focusedEnhetUrl + "/" + it.ressurs.navident
            }))

        },
        overEnheter:  flattenedOverenheter.map(it => ({
            name: it.navn,
            url: gqlEnhetToUrl(it),
            ledere: it.leder.map(it2 => translateRessurs(it2.ressurs, it))
        })),
        underEnheter: underenheter.map(it => ({
            name: it.organisasjonsenhet.navn,
            url: gqlEnhetToUrl(it.organisasjonsenhet),
            ledere: it.organisasjonsenhet.leder.map(it2 => translateRessurs(it2.ressurs, it.organisasjonsenhet))
        })),
        ressurser: ressurser.map(it => translateRessurs(it.ressurs,qryData.enhet!!))

    }

}

export function focusedRessursHierarchy(
    orgdata: Hierarchy.OrgHierarchy,
    qryData: Gq.HentRessursQueryResult
): Hierarchy.OrgHierarchyWithFocusedRessurs | Hierarchy.Err {
    if(!qryData.data){
        throw "Wrong usage focusedressurs"
    }
    const res = qryData.data.ressurs;
    if(!res){
        return {kind: "err", msg: "null ressurs"}
    }

    const ledere: { leder: Hierarchy.Ressurs, orgenhet: Hierarchy.OrgEnhet }[] = []
    for (const l of res.ledere) {
        for (const o of l.ressurs.orgTilknytning.map(it => it.orgEnhet)) {
            const oUrl = agressoIdDataToUrl(o.agressoId,o.orgNiv,o.navn)
            const closestOverenhet = orgdata.overEnheter[orgdata.overEnheter.length-1]
            const isImplicitLederOfClosestOverenhet = orgdata.overEnheter.map(it => it.url).includes(oUrl)
            console.log({l: l.ressurs.navident, o: oUrl, closestOverenhet, isImplicitLederOfClosestOverenhet})

            ledere.push({
                leder: {
                    name: nameFromRessurs(l.ressurs),
                    url: gqlEnhetToUrl(o) + "/" + l.ressurs.navident,
                    navident: l.ressurs.navident
                },
                orgenhet: isImplicitLederOfClosestOverenhet ? {
                        name: closestOverenhet.name,
                        url: closestOverenhet.url,
                        ledere: []
                    }
                    : {
                        name: o.navn,
                        url: gqlEnhetToUrl(o),
                        ledere: []
                    }
            })
        }
    }

    return {
        kind: "hierarchy_focused_ressurs",
        overEnheter: [...orgdata.overEnheter, orgdata.enhet],
        focusedRessurs: {
            name: nameFromRessurs({...res}),
            tel: res.telefon.map(it => formatTlf(it.nummer)).join(", ") || "Mangler tlf",
            email: res.epost || "Mangler epost",
            navident: res.navident,
            url: orgdata.enhet.url + "/" + res.navident,
            ledere
        },
    };


}