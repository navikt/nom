Hei ${innsenderVisningsnavn},

Masseflyttingen av ${total} ressurser fra ${gammelOrgEnhet} til ${nyOrgEnhet} er nå gjennomført.
Navidenter som er flyttet er:
${flyttedeNavidenter}

<#if feiletAntall == 0>

Alle ressurser ble flyttet.
<#else>
${antallFlyttet} av ${total} orgtilknytninger ble flyttet.

Følgende ressurser ble ikke flyttet (${feiletAntall}):
${feiletNavidenter}
</#if>

Du mottar denne e-posten fordi du har nylig utført en masseflytting.
Dette er en autogenerert e-post fra NAV Organisasjonsmaster (NOM). Du kan ikke svare på denne e-posten.
