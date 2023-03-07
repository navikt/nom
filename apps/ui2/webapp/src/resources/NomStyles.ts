import { css } from "@emotion/react";

export const widthBreakpoints = {
  bp0: "0rem",
  bp1: "21.5rem",
  bp2: "100rem",
  bp3: "120rem",
};
export const contentWidthByBreakpoint = {
  bp0: "90%",
  bp1: "60%",
  bp2: "50%",
  bp3: "60rem",
};

export const mediaQueryContentWidth = css`
  width: ${contentWidthByBreakpoint.bp0};
  @media (min-width: ${widthBreakpoints.bp1}) {
    width: ${contentWidthByBreakpoint.bp1};
  }
  @media (min-width: ${widthBreakpoints.bp2}) {
    width: ${contentWidthByBreakpoint.bp2};
  }
  @media (min-width: ${widthBreakpoints.bp3}) {
    width: ${contentWidthByBreakpoint.bp3};
  }
`;
