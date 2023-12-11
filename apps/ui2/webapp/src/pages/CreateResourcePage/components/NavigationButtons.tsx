import { css } from "@emotion/react";
import { Button } from "@navikt/ds-react";
import { useNavigate, useParams } from "@tanstack/react-router";

import { createRessursRoute, indexRoute } from "~/reactRouterRoutes";

export const navigationButtonCss = css`
  display: grid;
  grid-template-columns: repeat(2, 150px);
  gap: var(--a-spacing-4);
  justify-content: center;
  margin-top: var(--a-spacing-10);

  button {
    max-width: 150px;
  }

  > :nth-of-type(1) {
    grid-area: 1 / 2 / 1 / 3;
  }

  > :nth-of-type(2) {
    grid-area: 1 / 1 / 1 / 2;
  }

  > :nth-of-type(3) {
    grid-area: 2 / 1 / 2 / 3;
    justify-self: center;
  }
`;

export function NavigationButtons() {
  const { step } = useParams({ from: createRessursRoute.id });
  const navigate = useNavigate();
  return (
    <div css={navigationButtonCss}>
      <Button size="small" type="submit" variant="primary">
        Neste
      </Button>
      {step !== "1" && (
        <Button
          onClick={() =>
            navigate({
              to: createRessursRoute.to,
              params: { step: String(Number(step) - 1) },
              search: (o) => o,
            })
          }
          size="small"
          type="button"
          variant="secondary"
        >
          Forrige
        </Button>
      )}
      <Button
        onClick={() => navigate({ to: indexRoute.to })}
        size="small"
        type="button"
        variant="tertiary"
      >
        Avbryt
      </Button>
    </div>
  );
}
