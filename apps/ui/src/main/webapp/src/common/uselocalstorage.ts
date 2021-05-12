import React from "react";

export function useLocalstorageState(
  varname: string,
  defaultVal: string
): [any, (any) => void] {
  const [out, setOut] = React.useState<any>(undefined);

  React.useEffect(() => {
    const value = window.localStorage.getItem(varname);
    setOut(value);
  }, [varname]);

  function setValue(newvalue: any): void {
    if (newvalue === out) {
      return;
    }

    window.localStorage.setItem(varname, newvalue);
    setOut(newvalue);
  }

  return [out ?? defaultVal, setValue];
}
