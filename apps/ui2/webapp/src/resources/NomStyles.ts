import { css } from "@emotion/react";

export const defaultWidth = css`
  margin-left: auto;
  margin-right: auto;
  padding: 0 var(--ax-space-24);
  width: 100%;
  max-width: 1008px;
`;

export const defaultFormStyle = css`
  display: flex;
  flex-direction: column;
  margin-top: var(--ax-space-32);

  > div {
    margin-bottom: var(--ax-space-24);
  }
`;

export const fullwidthRow = css`
  display: flex;
  gap: var(--ax-space-16);
  align-items: flex-start;

  & > * {
    flex: 1;
  }
`;

export const blueTopBorder = css`
  border: 1px solid var(--nom-primary);
  border-top: 7px solid var(--nom-primary);
  border-radius: 5px;
  background-color: var(--nom-secondary-hover);
`;
