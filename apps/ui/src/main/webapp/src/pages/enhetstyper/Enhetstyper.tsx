import React, { Children } from "react";
import Contentbox from "../../components/contentbox/Contentbox";
import NomUiHeader from "../../components/header/NomUiHeader";
import Nompagewrap from "../../components/nompagewrap/Nompagewrap";

const Enhetstype = (props: {
  longName: string;
  shortName: string;
  agressonavn: string;
  id: string;
  children: React.ReactNode;
}): JSX.Element => {
  return (
    <div>
      <h4 id={props.id} className="enhetstypeTittel">
        Enhetstype: {props.longName}
      </h4>
      <h5 className="enhetstypeForkortetNavn">
        Forkortet navn: {props.shortName}
      </h5>
      <h5 className="enhetstypeAgressonavn">
        Agressonavn: {props.agressonavn}
      </h5>
      {props.children}
    </div>
  );
};

const EnhetParagraf = (props: { children: React.ReactNode }): JSX.Element => {
  return <p className="enhetstypeParagraf">{props.children}</p>;
};

export default function Enhetstyper(props: any): JSX.Element {
  return (
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //
    //

    <Nompagewrap pagetitle="Enhetstyper">
      <ul className="pageToB">
        <li>
          <a href="#AA">AA-reg</a>
        </li>
        <li>
          <a href="#ALS">Arbeidslivssenter</a>
        </li>
        <li>
          <a href="#NAY">Arbeid_og_Ytelser</a>
        </li>
        <li>
          <a href="#ARK">Arbeidsrådgivningskontor</a>
        </li>
        <li>
          <a href="#Dir">Direktorat</a>
        </li>
        <li>
          <a href="#Doksenter">Dokumentsenter</a>
        </li>
        <li>
          <a href="#FP">Familie- og pensjonsytelser</a>
        </li>
        <li>
          <a href="#Fylke">Fylkeskontor</a>
        </li>
        <li>
          <a href="#HMS">Hjelpemidler og tilrettelegging</a>
        </li>
        <li>
          <a href="#Klage">Klageinstans</a>
        </li>
        <li>
          <a href="#KS">Kontaktsenter</a>
        </li>
        <li>
          <a href="#Kontroll">Kontrollenhet</a>
        </li>
        <li>
          <a href="#NAV-ktr">NAV-kontor</a>
        </li>
        <li>
          <a href="#ROL">Rådgivende overlege</a>
        </li>
        <li>
          <a href="#Tiltak">Tiltak</a>
        </li>
        <li>
          <a href="#ØK1">Økonomitjeneste</a>
        </li>
        <li>
          <a href="#ØK2">Økonomi pensjon</a>
        </li>
        <li>
          <a href="#ØK3">Økonomi stønad</a>
        </li>
        <li>
          <a href="#Utland">Oppfølging utland</a>
        </li>
      </ul>

      <Enhetstype longName="AA-reg" shortName="AA" agressonavn="" id="">
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Arbeidslivssenter"
        shortName="ALS"
        agressonavn="ALS"
        id="ALS"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Arbeid og Ytelser"
        shortName="NAY"
        agressonavn="Arbeids og Ytelser"
        id="NAY"
      >
        <EnhetParagraf>
          NAV Arbeid og ytelser er et resultatområde i Ytelseslinjen i Arbeid-
          og velferdsdirektoratet. Sammen med NAV Familie- og pensjonsytelser
          har vi ansvar for å behandle søknader om ulike ytelser og
          hjelpemidler.
        </EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Arbeidsrådgivningskontor"
        shortName="ARK"
        agressonavn="ARK"
        id="ARK"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Direktoratet"
        shortName="Dir"
        agressonavn="Dir"
        id="Dir"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Dokumentsenter"
        shortName="Doksenter"
        agressonavn="Doksenter"
        id="Doksenter"
      >
        <EnhetParagraf>
          Dokumentsenter er enhetstypen for enheter med følgende oppgaver:
          <ul>
            <li>
              Postmottak, registrering, kvalitetssikring av journal, publisering
              av offentlig journal
            </li>
            <li>
              Rutiner, følge opp enheters etterlevelse og gevinsteiere,
              restansekontroll, avlevering papirarkiv, lokal arkivplan
            </li>
            <li>
              Opplæring og veiledning av saksbehandlere og ledere i Websak,
              arkivfaglig veiledning
            </li>
          </ul>
        </EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Familie- og pensjonsytelser"
        shortName="FP"
        agressonavn="Familie og pensjonsytelser"
        id="FP"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Fylkeskontor"
        shortName="Fylke"
        agressonavn="Fylke"
        id="Fylke"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Hjelpemidler og tilrettelegging"
        shortName="HMS (HOT)"
        agressonavn="HMS"
        id="HMS"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Klageinstans"
        shortName="Klage"
        agressonavn="Klageinstans"
        id="Klage"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Kontaktsenter"
        shortName="KS"
        agressonavn="Kontaktsenter"
        id="KS"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Kontrollenhet"
        shortName="Kontroll"
        agressonavn="Spes"
        id="Kontroll"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>
      <Enhetstype
        longName="NAV-kontor"
        shortName="NAV-ktr"
        agressonavn="Lokal"
        id="NAV-ktr"
      >
        <EnhetParagraf>
          NAV-kontor er er enhetstypen for fysiske lokaler åpne for brukere hos
          NAV. Dette er hoved arenaen for brukernes kontakt med NAV. Det er her
          NAV-veiledere holder til. I utgangspunktet er det minst ett NAV-kontor
          i hver kommune.
        </EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Rådgivende overlege"
        shortName="ROL"
        agressonavn="Rådgivende overlege"
        id="ROL"
      >
        <EnhetParagraf>
          Bistår NAV-kontor ved helefaglig problemstillinger der saksbehandling
          avhenger av heleserelaterte spørsmål.
        </EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Tiltak"
        shortName="Tiltak"
        agressonavn="Tiltak"
        id="Tiltak"
      >
        <EnhetParagraf>
          Handlinger satt på grunnlag av stønader gitt va NAV.
        </EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Økonomitjeneste"
        shortName="ØK"
        agressonavn="Økonomi"
        id="ØK1"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Økonomi pensjon"
        shortName=""
        agressonavn=""
        id="ØK2"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Økonomi stønad"
        shortName=""
        agressonavn=""
        id="ØK3"
      >
        <EnhetParagraf>placeholder</EnhetParagraf>
      </Enhetstype>

      <Enhetstype
        longName="Oppfølging utland"
        shortName="UTL"
        agressonavn=""
        id="Utland"
      >
        <EnhetParagraf>
          Oppfølging utland er enhetstypen for enheten NAV oppfølging utland.
          Enheten har ansvar for brukere som har jobbet og/eller bodd i Norge,
          og er bosatt i utlandet med rett til sykepenger eller
          arbeidsavklaringspenger (AAP) fra NAV. Disse brukerne skal få
          behandlet sakene sine og motta arbeidsrettet oppfølging i henhold til
          standard for arbeidsrettet brukeroppfølging i NAV. Brukerne bor
          fortrinnsvis i EØS-land, og etter EØS-forordningens prinsipper skal de
          primært gjennomføre tiltak i landet de bor i. De er underlagt de samme
          prinsippene for aktivitet og medvirkning som brukere bosatt i Norge.
        </EnhetParagraf>
      </Enhetstype>
    </Nompagewrap>
  );
}
