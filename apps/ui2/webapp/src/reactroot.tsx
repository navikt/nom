import { StrictMode } from "react";
import { createRoot } from "react-dom/client";

import { Main } from "./main";

createRoot(document.querySelector("#root") as HTMLElement).render(
  <StrictMode>
    <Main />
  </StrictMode>
);
