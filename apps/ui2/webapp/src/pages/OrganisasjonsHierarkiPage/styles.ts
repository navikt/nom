import { css } from "@emotion/css";

export const colorPalette = {
  rim: "var(--navds-global-color-lightblue-800)",
  focusedBackground: "var(--nom-secondary-hover)",
  unfocusedBackground: "var(--navds-global-color-white)",
  panelBorder: "var(--navds-global-color-gray-400)",
  font: "var(--navds-global-color-gray-900)",
};

export const commonPanelCss = css`
  border-radius: 5px;
  overflow: hidden;
  display: grid;
  min-height: 1rem;
  min-width: 1rem;
  grid-template-columns: min-content 1fr min-content min-content;
  background-color: ${colorPalette.unfocusedBackground};
  align-items: center;
  border: 1px solid ${colorPalette.panelBorder};

  :hover {
    background-color: ${colorPalette.focusedBackground};
  }
`;

export const panelCss = {
  currentEnhet: css(
    commonPanelCss,
    css`
      grid-template-columns: 1fr;
      grid-template-rows: min-content 1fr;
      margin-bottom: 1rem;
      min-height: 13rem;
      background-color: ${colorPalette.focusedBackground};

      svg {
        // meant for header prefix icon
        fill: ${colorPalette.rim};
        margin: 0 0.2rem 0 0;
      }
    `
  ),
  focusedRessurs: css(css`
    grid-template-columns: 1fr;
    width: 100%;
    //justify-items: center;
    background-color: ${colorPalette.focusedBackground};
    row-gap: 0.5rem;
    min-height: 10rem;
    margin-bottom: 0;
    padding: var(--navds-spacing-8) var(--navds-spacing-8) 0;
  `),
  underEnhet: css(
    commonPanelCss,
    css`
      height: 100%;
      min-height: 5rem;
      word-wrap: anywhere;
    `
  ),
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
      padding: 0.2rem;
    }
  `,
  fullHeight: css`
    height: 100%;
  `,
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
    }
  `,
};

export const lederStyling = {
  lederTekst: css`
    color: ${colorPalette.font};
    margin-left: 0.3rem;
  `,
  lederDiv: css`
    display: flex;
    align-items: center;
  `,
};

export const hierarkiCss = {
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
    @media (min-width: 60rem) {
      grid-template-columns: 1fr 1fr;
    }
  `,
};
