import { css } from "@emotion/css";
import { EmailFilled } from "@navikt/ds-icons";

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
      <a
        href="https://nav-it.slack.com/archives/C0362LA7ZLN"
        rel="noopener noreferrer"
        target="_blank"
      >
        <img alt="nom slack" src={slackNom}></img>
      </a>
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
            href="mailto:mugunthan.dharmadas@nav.no;linn.renate.fredriksen@nav.no"
          >
            e-post
          </a>
        </div>
      </div>
    </div>
  );
}
