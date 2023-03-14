import { css } from "@emotion/react";
import { EmailFilled } from "@navikt/ds-icons";
import { Link } from "@navikt/ds-react";

import { slackNom } from "./Images";

export const footerHeight = "150px";

export function Footer() {
  return (
    <div
      css={css`
        display: flex;
        height: ${footerHeight};
        background: var(--a-lightblue-900);
        color: var(--a-white);
        align-items: center;
        justify-content: center;
        gap: 60px;

        a {
          color: var(--a-white);
        }
      `}
    >
      <div
        css={css`
          display: flex;
          align-items: center;
          gap: 0.5rem;
        `}
      >
        <img
          alt="slack"
          css={css`
            width: 32px;
            height: 32px;
          `}
          src={slackNom}
        />
        <Link href={"https://nav-it.slack.com/archives/C0362LA7ZLN"}>#nom</Link>
      </div>

      <div
        css={css`
          margin-left: 1rem;
        `}
      >
        <Link
          href="https://navno.sharepoint.com/sites/intranett-organisasjon/SitePages/Organisasjonsmaster-(NOM).aspx?xsdata=MDV8MDF8fDQ5NzZlZGM5MjM5NTQ1MWIyZTU2MDhkYjEzZGU3OWNmfDYyMzY2NTM0MWVjMzQ5NjI4ODY5OWI1NTM1Mjc5ZDBifDB8MHw2MzgxMjU2MTg0NTAzNDU1NTh8VW5rbm93bnxWR1ZoYlhOVFpXTjFjbWwwZVZObGNuWnBZMlY4ZXlKV0lqb2lNQzR3TGpBd01EQWlMQ0pRSWpvaVYybHVNeklpTENKQlRpSTZJazkwYUdWeUlpd2lWMVFpT2pFeGZRPT18MXxNVFkzTmprMk5UQTBORFEwTlRzeE5qYzJPVFkxTURRME5EUTFPekU1T2pGaFlUQTNaVEpoTFdRMFpESXROREV6TlMxaU16TTVMVGd6WW1abU9ERXlNR1psTmw4ME0yVTJNakZoT0MwMk5UZ3lMVFEyWTJJdFlqSXhZeTAyTVRneE1qa3hNbVV6WXpSQWRXNXhMbWRpYkM1emNHRmpaWE09fGNlMjg1M2YzOTY1ZjQ2M2YyZTU2MDhkYjEzZGU3OWNmfDc5ZDFiMWQ3Yzc0NTQ2NTZiYTQ1Y2RmMGYxNjZjMGVj&sdata=MmJ2TjdJbFc2ZDZXSytiVFFUYU41OXZzM0ltcm9ab09xZVR5OEpPeVZ2WT0%3D&ovuser=62366534-1ec3-4962-8869-9b5535279d0b%2ctest%40nav.no&OR=Teams-HL&CT=1676969474366&clickparams=eyJBcHBOYW1lIjoiVGVhbXMtRGVza3RvcCIsIkFwcFZlcnNpb24iOiIyNy8yMzAxMDEwMDkxMyIsIkhhc0ZlZGVyYXRlZFVzZXIiOmZhbHNlfQ%3D%3D"
          rel="noopener noreferrer"
          target="_blank"
        >
          Om NOM på Navet
        </Link>
      </div>

      <div
        css={css`
          display: flex;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        <EmailFilled />
        <div>
          Spørsmål eller tilbakemeldinger? &nbsp;
          <Link href="mailto:mugunthan.dharmadas@nav.no;line.eeg-larsen@nav.no?subject=Tilbakemelding NOM">
            Send oss en e-post
          </Link>
        </div>
      </div>
    </div>
  );
}
