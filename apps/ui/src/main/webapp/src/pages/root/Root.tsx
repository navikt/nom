import React from "react";
import NomUiHeader from "../../components/header/NomUiHeader";

function resetData(): void {
  fetch("/api/reset").then((it) => {
    /* eslint-disable no-console */
    console.log(it.status);
    /* eslint-enable no-console */
  });
}

export default function Root(props: any): JSX.Element {
  return (
    <div className="Root">
      <NomUiHeader />
      Her settes data tilbake til default:
      <button onClick={() => resetData()}>Reset data</button>
    </div>
  );
}
