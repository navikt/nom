import { css } from "@emotion/react";

export const defaultWidth = css`
  margin-left: auto;
  margin-right: auto;
  padding: 0 var(--a-spacing-6);
  width: 100%;
  max-width: 1008px;
`;

export const defaultFormStyle = css`
  display: flex;
  flex-direction: column;
  margin-top: var(--a-spacing-8);

  > div {
    margin-bottom: var(--a-spacing-6);
  }
`;

export const fullwidthRow = css`
  display: flex;
  gap: var(--a-spacing-4);
  align-items: flex-start;

  & > * {
    flex: 1;
  }
`;

export const blueTopBorder = css`
  border: 1px solid var(--a-icon-info);
  border-top: 7px solid var(--a-icon-info);
  border-radius: 5px;
  background-color: var(--nom-secondary-hover);
`;
