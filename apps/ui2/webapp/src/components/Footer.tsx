import { css } from "@emotion/react";
import { EnvelopeClosedFillIcon } from "@navikt/aksel-icons";
import { Link } from "@navikt/ds-react";
import type { Ref, SVGProps } from "react";
import { forwardRef } from "react";

import { defaultWidth } from "~/resources/NomStyles";

export const footerHeight = "180px";

export function Footer() {
  return (
    <div
      css={css`
        width: 100%;
        height: ${footerHeight};
        background: var(--a-lightblue-900);
        color: var(--a-white);
        padding: var(--a-spacing-4) 0;
        display: flex;
        justify-content: center;
      `}
    >
      <div
        css={css(
          css`
            align-self: center;
            gap: var(--a-spacing-4) 3rem;
            display: grid;
            grid-template-columns: max-content 1fr;

            a {
              color: var(--a-white);
              width: fit-content;
            }

            svg {
              width: 24px;
              height: 24px;
            }
          `,
          defaultWidth,
        )}
      >
        <Link
          href="https://navno.sharepoint.com/sites/intranett-organisasjon/SitePages/Organisasjonsmaster-(NOM).aspx?xsdata=MDV8MDF8fDQ5NzZlZGM5MjM5NTQ1MWIyZTU2MDhkYjEzZGU3OWNmfDYyMzY2NTM0MWVjMzQ5NjI4ODY5OWI1NTM1Mjc5ZDBifDB8MHw2MzgxMjU2MTg0NTAzNDU1NTh8VW5rbm93bnxWR1ZoYlhOVFpXTjFjbWwwZVZObGNuWnBZMlY4ZXlKV0lqb2lNQzR3TGpBd01EQWlMQ0pRSWpvaVYybHVNeklpTENKQlRpSTZJazkwYUdWeUlpd2lWMVFpT2pFeGZRPT18MXxNVFkzTmprMk5UQTBORFEwTlRzeE5qYzJPVFkxTURRME5EUTFPekU1T2pGaFlUQTNaVEpoTFdRMFpESXROREV6TlMxaU16TTVMVGd6WW1abU9ERXlNR1psTmw4ME0yVTJNakZoT0MwMk5UZ3lMVFEyWTJJdFlqSXhZeTAyTVRneE1qa3hNbVV6WXpSQWRXNXhMbWRpYkM1emNHRmpaWE09fGNlMjg1M2YzOTY1ZjQ2M2YyZTU2MDhkYjEzZGU3OWNmfDc5ZDFiMWQ3Yzc0NTQ2NTZiYTQ1Y2RmMGYxNjZjMGVj&sdata=MmJ2TjdJbFc2ZDZXSytiVFFUYU41OXZzM0ltcm9ab09xZVR5OEpPeVZ2WT0%3D&ovuser=62366534-1ec3-4962-8869-9b5535279d0b%2ctest%40nav.no&OR=Teams-HL&CT=1676969474366&clickparams=eyJBcHBOYW1lIjoiVGVhbXMtRGVza3RvcCIsIkFwcFZlcnNpb24iOiIyNy8yMzAxMDEwMDkxMyIsIkhhc0ZlZGVyYXRlZFVzZXIiOmZhbHNlfQ%3D%3D"
          rel="noopener noreferrer"
          target="_blank"
        >
          Om NOM
        </Link>
        <Link
          css={css`
            svg {
              width: 18px;
              height: 18px;
              margin-right: 4px;
            }
          `}
          href={"https://nav-it.slack.com/archives/C0362LA7ZLN"}
        >
          <SlackIcon />
          #nom
        </Link>
        <Link
          href="https://uustatus.no/nb/erklaringer/publisert/ed146ebd-3afa-428e-95e1-e7dc21f56119"
          rel="noopener noreferrer"
          target="_blank"
        >
          Tilgjengelighetserklæring
        </Link>
        <Link
          css={css`
            grid-row: span 2;
            align-self: flex-start;

            svg {
              align-self: start;
            }
          `}
          href="mailto:mugunthan.dharmadas@nav.no;line.eeg-larsen@nav.no?subject=Tilbakemelding NOM"
        >
          <EnvelopeClosedFillIcon />
          Spørsmål eller tilbakemeldinger?
        </Link>
      </div>
    </div>
  );
}

export const SlackIcon = forwardRef(
  (
    { ...properties }: SVGProps<SVGSVGElement>,
    reference: Ref<SVGSVGElement>,
  ) => {
    return (
      <svg
        fill="none"
        focusable={false}
        height="1em"
        ref={reference}
        role="img"
        viewBox="0 0 24 24"
        width="1em"
        xmlns="http://www.w3.org/2000/svg"
        {...properties}
      >
        <path
          d="M5.05947 15.1721C5.05947 16.567 3.93301 17.6944 2.53926 17.6944C1.1455 17.6944 0.019043 16.567 0.019043 15.1721C0.019043 13.7772 1.1455 12.6498 2.53926 12.6498H5.05947V15.1721Z"
          fill="#F8F8F8"
        />
        <path
          d="M6.32031 15.1721C6.32031 13.7772 7.44677 12.6498 8.84052 12.6498C10.2343 12.6498 11.3607 13.7772 11.3607 15.1721V21.4778C11.3607 22.8727 10.2343 24 8.84052 24C7.44677 24 6.32031 22.8727 6.32031 21.4778V15.1721Z"
          fill="#F8F8F8"
        />
        <path
          d="M8.84028 5.04454C7.44653 5.04454 6.32007 3.91716 6.32007 2.52227C6.32007 1.12738 7.44653 0 8.84028 0C10.234 0 11.3605 1.12738 11.3605 2.52227V5.04454H8.84028Z"
          fill="#F8F8F8"
        />
        <path
          d="M8.83984 6.32492C10.2336 6.32492 11.36 7.4523 11.36 8.84719C11.36 10.2421 10.2336 11.3695 8.83984 11.3695H2.52021C1.12646 11.3695 0 10.2421 0 8.84719C0 7.4523 1.12646 6.32492 2.52021 6.32492H8.83984Z"
          fill="#F8F8F8"
        />
        <path
          d="M18.9397 8.84719C18.9397 7.4523 20.0662 6.32492 21.4599 6.32492C22.8537 6.32492 23.9801 7.4523 23.9801 8.84719C23.9801 10.2421 22.8537 11.3695 21.4599 11.3695H18.9397V8.84719Z"
          fill="#F8F8F8"
        />
        <path
          d="M17.6803 8.84705C17.6803 10.2419 16.5539 11.3693 15.1601 11.3693C13.7664 11.3693 12.6399 10.2419 12.6399 8.84705V2.52227C12.6399 1.12738 13.7664 0 15.1601 0C16.5539 0 17.6803 1.12738 17.6803 2.52227V8.84705Z"
          fill="#F8F8F8"
        />
        <path
          d="M15.1601 18.9551C16.5539 18.9551 17.6803 20.0825 17.6803 21.4774C17.6803 22.8723 16.5539 23.9997 15.1601 23.9997C13.7664 23.9997 12.6399 22.8723 12.6399 21.4774V18.9551H15.1601Z"
          fill="#F8F8F8"
        />
        <path
          d="M15.1601 17.6944C13.7664 17.6944 12.6399 16.567 12.6399 15.1721C12.6399 13.7772 13.7664 12.6498 15.1601 12.6498H21.4797C22.8735 12.6498 23.9999 13.7772 23.9999 15.1721C23.9999 16.567 22.8735 17.6944 21.4797 17.6944H15.1601Z"
          fill="#F8F8F8"
        />
      </svg>
    );
  },
);
