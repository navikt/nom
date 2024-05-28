import { css } from "@emotion/react";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { Button } from "@navikt/ds-react";
import { useNavigate } from "@tanstack/react-router";
import React from "react";

export function SaveOrAbortButtons({ isLoading }: { isLoading: boolean }) {
  const navigate = useNavigate();

  return (
    <div
      css={css`
        display: flex;
        justify-content: flex-end;
        gap: var(--a-spacing-4);
      `}
    >
      <Button
        data-cy="avbryt-button"
        onClick={() => navigate({ search: {}, params: (previous) => previous })}
        size="small"
        type="button"
        variant="secondary"
      >
        Avbryt
      </Button>
      <Button
        data-cy="lagre-button"
        icon={<FloppydiskIcon aria-hidden />}
        loading={isLoading}
        size="small"
        type="submit"
        variant="primary"
      >
        Lagre
      </Button>
    </div>
  );
}
