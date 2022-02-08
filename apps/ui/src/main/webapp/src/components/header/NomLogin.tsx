import React from "react";
import useFetchLoggedInName from "../../api/useFetchLoggedInName";

export default function NomLogin(props: any): JSX.Element {
  const [res, retrigger] = useFetchLoggedInName();
  let out: any = <div></div>;
  switch (res.lookupState) {
    case "ERROR":
    case "PENDING":
    case "WAITING":
      out = <span></span>;
      break;
    case "RESOLVED":
      out = <span>{res.result}</span>;
  }
  return (
    <div>
      <span>Login:</span>
      <span>{out}</span>
    </div>
  );
}
