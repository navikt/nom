import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import { BodyShort, Button, Loader, TextField, VStack } from "@navikt/ds-react";
import { useQuery } from "@tanstack/react-query";
import { useSearch } from "@tanstack/react-router";
import React from "react";
import { FormProvider, useForm } from "react-hook-form";
import * as yup from "yup";

import { getOrgEnhet } from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import type { OrgEnhet } from "~/types/api-models";

export const KorrigerOrgEnhetNavn = () => {
  const hasAdminAccess = useHasAdminAccess();

  const { id } = useSearch({ from: "/korriger-navn-organisasjonsenhet" });

  return (
    <>
      <PageBanner text="Korrigér organisasjonsenhetsnavn" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          {hasAdminAccess ? (
            <KorrigeringOrgEnhetNavn orgEnhetId={id} />
          ) : (
            <IkkeTilgangRedigering />
          )}
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function KorrigeringOrgEnhetNavn({ orgEnhetId }: { orgEnhetId?: string }) {
  const orgEnhetQuery = useQuery({
    queryKey: getOrgEnhet.queryKey(orgEnhetId ?? ""),
    queryFn: () => getOrgEnhet.queryFn(orgEnhetId ?? ""),
    enabled: !!orgEnhetId,
  });

  const orgEnhet = orgEnhetQuery.data;

  if (orgEnhetQuery.isLoading) {
    return (
      <div
        css={css`
          display: flex;
          flex-direction: column;
          align-items: center;
          gap: var(--a-spacing-2);
        `}
      >
        <Loader size="3xlarge" />
        <BodyShort size="large">Laster...</BodyShort>
      </div>
    );
  }

  if (!orgEnhet) {
    return <OrgEnhetIkkeFunnet />;
  }

  return (
    <>
      <OrgEnhetInfoWrapper
        heading="Valgt organisasjonsenhet"
        orgEnhetId={orgEnhetId}
      />

      <KorrigeringOrgEnhetNavnForm orgEnhet={orgEnhet} />
    </>
  );
}

function KorrigeringOrgEnhetNavnForm({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const methods = useForm({
    resolver: yupResolver(validationSchema),
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {};

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="9">
          <TextField
            {...methods.register("navn")}
            description="Enheten kan ikke ha likt navn som en annen enhet"
            error={methods.formState.errors.navn?.message}
            label="Nytt navn på organisasjonsenhet"
          />

          <div
            css={css`
              display: flex;
              justify-content: end;
              gap: var(--a-spacing-4);
            `}
          >
            <SmartLink href="/organisasjonsmastring">
              <Button size="small" variant="secondary">
                Avbryt
              </Button>
            </SmartLink>

            <Button
              icon={<FloppydiskIcon aria-hidden />}
              size="small"
              type="submit"
            >
              Lagre
            </Button>
          </div>
        </VStack>
      </form>
    </FormProvider>
  );
}

const validationSchema = yup.object({
  navn: yup.string().required("Obligatorisk"),
});
