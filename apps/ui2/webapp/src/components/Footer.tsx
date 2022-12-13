import { css } from "@emotion/css";
import { EmailFilled } from "@navikt/ds-icons";
import { Link } from "@navikt/ds-react";

import { slackNom } from "./Images";

export const footerHeight = "150px";

export function Footer() {
  return (
    <div
      className={css`
        display: flex;
        height: ${footerHeight};
        background: var(--navds-global-color-lightblue-900);
        color: var(--navds-global-color-white);
        align-items: center;
        justify-content: center;
        gap: 60px;
      `}
    >
      <div
        className={css`
          display: flex;
          align-items: center;
          gap: 0.5rem;
        `}
      >
        <img
          alt="slack"
          className={css`
            width: 32px;
            height: 32px;
          `}
          src={slackNom}
        />
        <a
          className={css`
            color: white;
          `}
          href={"https://nav-it.slack.com/archives/C0362LA7ZLN"}
        >
          #nom
        </a>
      </div>
      <div
        className={css`
          display: flex;
          align-items: center;
          gap: var(--navds-spacing-2);
        `}
      >
        <EmailFilled />
        <div>
          Spørsmål eller tilbakemeldinger? Send oss en{" "}
          <a
            className={css`
              color: var(--navds-global-color-white);
            `}
            href="mailto:mugunthan.dharmadas@nav.no;linn.renate.fredriksen@nav.no?subject=Tilbakemelding NOM"
          >
            e-post
          </a>
        </div>
      </div>
    </div>
  );
}
