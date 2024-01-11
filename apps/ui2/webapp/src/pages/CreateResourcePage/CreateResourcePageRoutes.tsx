import { css } from "@emotion/react";
import { Stepper } from "@navikt/ds-react";
import { useParams } from "@tanstack/react-router";
import React from "react";
import * as yup from "yup";

import { PageBanner } from "~/components/PageBanner";
import { ConfirmStep } from "~/pages/CreateResourcePage/ConfirmStep";
import { OrganisasjonstilknytningStep } from "~/pages/CreateResourcePage/OrganisasjonstilknytningStep";
import { PersonopplysningerStep } from "~/pages/CreateResourcePage/PersonopplysningerStep";
import { SektorStep } from "~/pages/CreateResourcePage/SektorStep";
import { SummaryStep } from "~/pages/CreateResourcePage/SummaryStep";
import { createRessursRoute } from "~/reactRouterRoutes";
import { AnsettelsesType, Sektor } from "~/types/api-models";

import ContentWrapper from "../../components/ContentWrapper";

export function CreateRessursPage() {
  const { step } = useParams({ from: createRessursRoute.fullPath });

  const stepIdAsNumber = Number(step);

  const steps = [
    "Sektor",
    "Personopplysninger",
    "Organisasjonstilknytning",
    "Oppsummering og veien videre",
  ];

  let StepComponent;
  if (stepIdAsNumber === 1) {
    StepComponent = <SektorStep />;
  }
  if (stepIdAsNumber === 2) {
    StepComponent = <PersonopplysningerStep />;
  }
  if (stepIdAsNumber === 3) {
    StepComponent = <OrganisasjonstilknytningStep />;
  }
  // TODO: solve this in a better way
  if (stepIdAsNumber === 9) {
    StepComponent = <ConfirmStep />;
  }
  if (stepIdAsNumber === 4) {
    StepComponent = <SummaryStep />;
  }

  return (
    <>
      <PageBanner text="Opprett medarbeider" />
      <ContentWrapper>
        <Stepper
          activeStep={stepIdAsNumber}
          css={css`
            margin-bottom: var(--a-spacing-8);
          `}
          interactive={false}
          orientation="horizontal"
        >
          {steps.map((step, index) => (
            <Stepper.Step completed={stepIdAsNumber > index + 1} key={step}>
              {step}
            </Stepper.Step>
          ))}
        </Stepper>
        {StepComponent}
      </ContentWrapper>
    </>
  );
}

const commonValidationSchema = yup.object({
  personident: yup.string().required("Obligatorisk"),
  visningsfornavn: yup.string().required("Obligatorisk"),
  visningsmellomnavn: yup.string().optional(),
  visningsetternavn: yup.string().required("Obligatorisk"),
  sektor: yup
    .mixed<Sektor>()
    .oneOf(Object.values(Sektor))
    .required("Obligatorisk"),
  orgEnhetId: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
  ansettelsesType: yup
    .mixed<AnsettelsesType>()
    .oneOf(Object.values(AnsettelsesType))
    .required("Obligatorisk"),
  fysiskBrukerstedNr: yup.string().required("Obligatorisk"),
});

// eslint-disable-next-line unicorn/prefer-spread
const statligValidationSchema = commonValidationSchema.concat(
  yup.object({
    gyldigTom: yup.string().optional(),
  }),
);

export function validateCreateRessursSearch(
  search: Record<string, unknown>,
): Partial<StatligFormValues> & { navident?: string } {
  // validate and parse the search params into a typed state
  return {
    personident: String(search.personident ?? ""),
    navident: String(search.navident ?? ""),
    visningsfornavn: String(search.visningsfornavn ?? ""),
    visningsmellomnavn: String(search.visningsmellomnavn ?? ""),
    visningsetternavn: String(search.visningsetternavn ?? ""),
    sektor: (search.sektor as Sektor) ?? undefined,
    orgEnhetId: String(search.orgEnhetId ?? ""),
    gyldigFom: String(search.gyldigFom ?? ""),
    gyldigTom: String(search.gyldigTom ?? ""),
    ansettelsesType: (search.ansettelsesType as AnsettelsesType) ?? undefined,
    fysiskBrukerstedNr: String(search.fysiskBrukerstedNr ?? ""),
  };
}

export type StatligFormValues = yup.InferType<typeof statligValidationSchema>;

export const AnsettelsesTypeNameMapping = {
  [AnsettelsesType.FAST]: "Fast",
  [AnsettelsesType.MIDLERTIDIG]: "Midlertidig",
};
