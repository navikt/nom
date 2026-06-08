import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon } from "@navikt/aksel-icons";
import {
  BodyLong,
  BodyShort,
  Button,
  Checkbox,
  DatePicker,
  HStack,
  Loader,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { useNavigate, useSearch } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import React from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  getOrgEnhet,
  opprettOrgEnhetLeder,
  orgEnheterKeys,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import {
  FormRessursSearchWrapper,
  IkkeTilgangRedigering,
  OrgEnhetIkkeFunnet,
} from "~/pages/OrganizationMastering/Common/CommonComponents";
import { OrgEnhetInfoWrapper } from "~/pages/OrganizationMastering/OpprettOrgEnhet";
import type { OpprettOrgEnhetLederRequest, OrgEnhet } from "~/types/api-models";

export const KorrigerOrgEnhetLeder = () => {
  const hasAdminAccess = useHasAdminAccess();

  const { id } = useSearch({ from: "/korriger-leder-organisasjonsenhet" });

  return (
    <>
      <PageBanner text="Endre leder på organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          {hasAdminAccess ? (
            <KorrigeringOrgEnhetLeder orgEnhetId={id} />
          ) : (
            <IkkeTilgangRedigering />
          )}
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function KorrigeringOrgEnhetLeder({ orgEnhetId }: { orgEnhetId?: string }) {
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

      <KorrigeringOrgEnhetLederForm orgEnhet={orgEnhet} />
    </>
  );
}

function KorrigeringOrgEnhetLederForm({ orgEnhet }: { orgEnhet: OrgEnhet }) {
  const navigate = useNavigate();

  const queryClient = useQueryClient();

  const methods = useForm({
    resolver: yupResolver(validationSchema),
    defaultValues: {
      fungerendeLeder: false,
    },
    shouldUnregister: true,
  });

  const opprettOrgEnhetLederMutation = useMutation<
    unknown,
    AxiosError<Error>,
    OpprettOrgEnhetLederRequest
  >({
    mutationFn: async (request) => {
      return opprettOrgEnhetLeder(request);
    },
    onSuccess: async () => {
      showToast({ text: "Oppdatert" });

      await queryClient.invalidateQueries({
        queryKey: orgEnheterKeys.id(orgEnhet.id),
      });

      navigate({
        to: "/organisasjonsmastring",
      }).then();
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: OpprettOrgEnhetLederRequest = {
      orgEnhetId: orgEnhet.id,
      navident: values.lederRessursNavident,
      gyldigFom: values.gyldigFom,
      gyldigTom: values.fungerendeLeder ? values.gyldigTom : undefined,
    };

    opprettOrgEnhetLederMutation.mutate(request);
  };

  const fungerendeLeder = methods.watch("fungerendeLeder");

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="8">
          <VStack gap="6">
            <FormRessursSearchWrapper
              label="Ny leder på organisasjonsenhet"
              name="lederRessursNavident"
            />

            <div
              css={css`
                display: flex;
                justify-content: space-between;
              `}
            >
              <VStack
                css={css`
                  min-width: 450px;
                `}
                gap="2"
              >
                <Checkbox {...methods.register("fungerendeLeder")}>
                  Midlertidig fungerende leder
                </Checkbox>

                <HStack gap="6">
                  <FormDateWrapper label="Leder fra" name="gyldigFom" />

                  {fungerendeLeder && (
                    <FormDateWrapper label="Leder til" name="gyldigTom" />
                  )}
                </HStack>
              </VStack>

              <BodyLong
                css={css`
                  width: fit-content;
                  background-color: var(--nom-secondary-hover);
                  border-radius: 8px;
                  padding: var(--a-spacing-4);
                  height: fit-content;
                `}
              >
                <VStack gap="2">
                  <span>
                    <b>Lederplan:</b>
                  </span>
                  <span>
                    <b>Hans Petter Sørensen Grønsveen Lang</b>:<br /> 01.01.2026
                    - 30.06.2026
                  </span>
                  <span>
                    <b>Hans Petter Sørensen Grønsveen Lang</b>:<br /> 01.07.2026
                    - ubestemt tid
                  </span>
                </VStack>
              </BodyLong>
            </div>
          </VStack>

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

function FormDateWrapper({ name, label }: { name: string; label: string }) {
  const { field, fieldState } = useController({
    name,
  });

  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : undefined);
    },
    defaultSelected: field.value ? new Date(field.value) : undefined,
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={fieldState.error?.message}
        ref={field.ref}
        {...datePickerProperties.inputProps}
        label={label}
        size="medium"
      />
    </DatePicker>
  );
}

const validationSchema = yup.object({
  lederRessursNavident: yup.string().required("Obligatorisk"),
  fungerendeLeder: yup.boolean().default(false),
  gyldigFom: yup.string().required("Obligatorisk"),
  gyldigTom: yup.string().when("fungerendeLeder", {
    is: true,
    then: (schema) => schema.required("Obligatorisk"),
    otherwise: (schema) => schema.notRequired(),
  }),
});
