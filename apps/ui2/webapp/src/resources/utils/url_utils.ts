export function agressoNumberStrToNiva(n: string): string {
  switch (n) {
    case "0":
      return "ORGNIV0";
    case "1":
      return "ORGNIV1";
    case "2":
      return "ORGNIV2";
    case "21":
      return "ORGNIV21";
    case "25":
      return "ORGNIV25";
    case "3":
      return "ORGNIV3";
    case "4":
      return "ORGNIV4";
    default:
      return "ORGENHET";
  }
}

export function agressoNivaToNumberStr(niva: string): string {
  if (niva === "ORGENHET") {
    return "";
  } else {
    return niva.substr("ORGNIV".length);
  }
}

export function agressoIdDataToUrl(
  agressoId: string,
  agressoNiva: string,
  navn: string
): string {
  const niva = agressoNivaToNumberStr(agressoNiva);
  const nivaStr = niva ? niva + "_" : "";
  const agressoIdDataToUrlTmp = nivaStr + agressoId + "_" + navn.replaceAll(" ", "&").replaceAll("/","\\");
  return agressoIdDataToUrlTmp;
}

export function agressoUrlToData(agressoIdUrl: string): {
  agressoId: string;
  agressoNiva: string;
  enhetNavn: string;
} {
  const out = {
    agressoId: "",
    agressoNiva: "",
    enhetNavn: ""
  };

  const arr = agressoIdUrl.split("_");
  if (arr.length !== 3) {
    out.agressoNiva = "ORGENHET";
    out.agressoId = arr[0];
    out.enhetNavn = arr[1].replaceAll("&", " ").replaceAll("\\","/");
  } else {
    out.agressoNiva = agressoNumberStrToNiva(arr[0]);
    out.agressoId = arr[1];
    out.enhetNavn = arr[2].replaceAll("&", " ");
  }

  return out;
}
