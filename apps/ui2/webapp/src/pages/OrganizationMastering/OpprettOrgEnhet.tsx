import { css } from "@emotion/react";
import { yupResolver } from "@hookform/resolvers/yup";
import { FloppydiskIcon, PlusCircleIcon } from "@navikt/aksel-icons";
import {
  BodyShort,
  Button,
  DatePicker,
  Heading,
  HStack,
  Loader,
  Select,
  TextField,
  useDatepicker,
  VStack,
} from "@navikt/ds-react";
import { useMutation, useQuery } from "@tanstack/react-query";
import { useNavigate } from "@tanstack/react-router";
import type { AxiosError } from "axios";
import axios from "axios";
import React, { useState } from "react";
import { FormProvider, useController, useForm } from "react-hook-form";
import * as yup from "yup";

import {
  getOrgEnhet,
  getOrgEnhetList,
  opprettOrgEnhet,
} from "~/api/react-query-endpoints";
import { useHasAdminAccess } from "~/api/useGetSession";
import ContentWrapper from "~/components/ContentWrapper";
import { SmartLink } from "~/components/navigation/SmartLink";
import { PageBanner } from "~/components/PageBanner";
import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import { formatDateWithoutTimezone } from "~/dateUtils/dateUtils";
import { showToast } from "~/hooks/showToast";
import { ContainerWithHighlightedTop } from "~/pages/CreateResourcePage/components/ContainerWithHighlightedTop";
import { OrgEnhetsTypeNameMapping } from "~/pages/OrganisasjonsHierarkiPage/components/ValgtOrgEnhet";
import { OrgEnhetInfo } from "~/pages/ResourcePage/Masseflytting/Masseflytting";
import type { OpprettOrgEnhetRequest, OrgEnhet } from "~/types/api-models";
import { OrgEnhetsType } from "~/types/api-models";
import { isInDevelopment } from "~/utils";

export const OpprettOrgEnhet = () => {
  const hasAdminAccess = useHasAdminAccess();

  if (!hasAdminAccess) {
    return;
  }

  const [selectedOrgEnhetId, setSelectedOrgEnhet] = useState<string | null>(
    null,
  );

  const handleOpprettHerClick = (id: string) => setSelectedOrgEnhet(id);

  return (
    <>
      <PageBanner text="Opprett ny organisasjonsenhet" />

      <ContentWrapper>
        <ContainerWithHighlightedTop padding={6}>
          <VelgOrgEnhetPlassering
            handleOpprettHerClick={handleOpprettHerClick}
            selectedOrgEnhetId={selectedOrgEnhetId}
          />

          <OrgEnhetInfoForm orgEnhetId={selectedOrgEnhetId} />

          {!selectedOrgEnhetId && (
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
            </div>
          )}
        </ContainerWithHighlightedTop>
      </ContentWrapper>
    </>
  );
};

function VelgOrgEnhetPlassering({
  handleOpprettHerClick,
  selectedOrgEnhetId,
}: {
  handleOpprettHerClick: (id: string) => void;
  selectedOrgEnhetId: string | null;
}) {
  return (
    <>
      <Heading level="2" size="medium">
        Plassering
      </Heading>

      <OrgHierarki handleOpprettHerClick={handleOpprettHerClick} />

      {!!selectedOrgEnhetId && (
        <OrgEnhetInfoWrapper
          heading="Enheten blir opprettet under"
          orgEnhetId={selectedOrgEnhetId}
        />
      )}
    </>
  );
}

function OrgEnhetInfoForm({ orgEnhetId }: { orgEnhetId: string | null }) {
  if (!orgEnhetId) {
    return;
  }

  const navigate = useNavigate();

  const methods = useForm({
    resolver: yupResolver(validationSchema),
  });

  const opprettOrgEnhetMutation = useMutation<
    unknown,
    AxiosError<Error>,
    OpprettOrgEnhetRequest
  >({
    mutationFn: async (request) => {
      return opprettOrgEnhet(request);
    },
    onSuccess: async () => {
      showToast({ text: "Opprettet" });

      navigate({
        to: "/organisasjonsmastring",
      }).then();
    },
  });

  const onValid = async (values: yup.InferType<typeof validationSchema>) => {
    const request: OpprettOrgEnhetRequest = {
      ...values,
      overordnetOrgEnhetId: orgEnhetId,
    };

    opprettOrgEnhetMutation.mutate(request);
  };

  return (
    <FormProvider {...methods}>
      <form onSubmit={methods.handleSubmit(onValid)}>
        <VStack gap="4">
          <Heading level="2" size="medium">
            Om enheten
          </Heading>

          <TextField
            {...methods.register("navn")}
            description="Enheten kan ikke ha likt navn som en annen enhet"
            error={methods.formState.errors.navn?.message}
            label="Navn på den nye organisasjonsenhet"
          />

          <FormRessursSearchWrapper name="lederRessursNavident" />

          <FormDateWrapper name="gyldigFom" />

          <Select
            css={css`
              width: fit-content;
            `}
            {...methods.register("orgEnhetsType")}
            error={methods.formState.errors.orgEnhetsType?.message}
            label="Organisasjonsenhetstype"
          >
            <option value="" />
            {Object.values(OrgEnhetsType)
              .filter((orgEnhetsType) => orgEnhetsType !== OrgEnhetsType.DIR)
              .map((orgEnhetsType) => (
                <option key={orgEnhetsType} value={orgEnhetsType}>
                  {OrgEnhetsTypeNameMapping[orgEnhetsType]}
                </option>
              ))}
          </Select>

          <TextField
            css={css`
              width: 20%;
            `}
            {...methods.register("firesiffer")}
            error={methods.formState.errors.firesiffer?.message}
            label="Firesiffer"
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

function FormRessursSearchWrapper({ name }: { name: string }) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <SelectLayoutWrapper
      error={fieldState.error}
      htmlFor="Leder"
      label="Leder for ny organisasjonsenhet"
    >
      <AsyncSearch
        loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
        noOptionsMessage={({ inputValue }: { inputValue: string }) =>
          inputValue.length < 3
            ? "Du må skrive minst 3 tegn for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(selectedOption: { label: string; value: string } | null) =>
          field.onChange(selectedOption?.value ?? "")
        }
        placeholder="Søk etter medarbeider"
      />
    </SelectLayoutWrapper>
  );
}

async function searchIfEnoughInput(searchTerm: string) {
  if (searchTerm.length < 3) {
    return [];
  }

  const searchQuery = `query ressurs($searchTerm: String!, $filter: RessursSearchFilter) {
        searchRessurs(term: $searchTerm, filter: $filter) {
          navident
          visningsnavn
        }
      }`;

  const filter = { statusSelection: "AKTIV" };

  const resultRaw = await axios.post("/frackend/nom-api/graphql", {
    operationName: "ressurs",
    query: searchQuery,
    variables: {
      searchTerm: searchTerm,
      filter: filter,
    },
  });

  const resultRessurser = resultRaw.data.data.searchRessurs;

  return resultRessurser.map((ressurs: any) => ({
    label: `${ressurs.visningsnavn} (${ressurs.navident})`,
    value: ressurs.navident,
  }));
}

function FormDateWrapper({ name }: { name: string }) {
  const { field, fieldState } = useController({
    name,
  });

  const datePickerProperties = useDatepicker({
    onDateChange: async (date) => {
      field.onChange(date ? formatDateWithoutTimezone(date) : undefined);
    },
  });

  return (
    <DatePicker {...datePickerProperties.datepickerProps}>
      <DatePicker.Input
        error={fieldState.error?.message}
        ref={field.ref}
        {...datePickerProperties.inputProps}
        label="Dato"
      />
    </DatePicker>
  );
}

function OrgHierarki({
  handleOpprettHerClick,
}: {
  handleOpprettHerClick?: any;
}) {
  const inDevelopment = isInDevelopment();

  const developmentLenker = ["fa965e", "me177r", "sy471r", "hu797w", "tu734a"];
  const productionLenker = ["bu431e", "ry630r", "te464t", "se232x", "ne994m"];

  const topOrgEnhetIder = inDevelopment ? developmentLenker : productionLenker;

  const topOrgEnhetQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(topOrgEnhetIder),
    queryFn: () => getOrgEnhetList.queryFn(topOrgEnhetIder),
    enabled: topOrgEnhetIder.length > 0,
  });

  const topOrgEnheter = (topOrgEnhetQuery.data ?? []).sort(
    (a, b) => topOrgEnhetIder.indexOf(a.id) - topOrgEnhetIder.indexOf(b.id),
  );

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const handleClick = (id: string) => setSelectedId(id);

  return (
    <div
      css={css`
        width: 100%;
      `}
    >
      <div
        css={css`
          width: 20%;
          margin: auto auto var(--a-spacing-8);
          padding: 0 var(--a-spacing-2) 0;
        `}
      >
        <OrgHierarkiCard name="Nav" onClick={handleClick} selected />
      </div>

      <HStack gap="4" wrap={false}>
        {topOrgEnheter.map((orgEnhet) => (
          <OrgHierarkiStack
            handleOpprettHerClick={handleOpprettHerClick}
            key={orgEnhet.id}
            onClick={handleClick}
            orgEnhet={orgEnhet}
            selected={orgEnhet.id === selectedId}
          />
        ))}
      </HStack>
    </div>
  );
}

function OrgHierarkiStack({
  orgEnhet,
  onClick,
  selected = false,
  handleOpprettHerClick,
}: {
  orgEnhet?: OrgEnhet;
  onClick?: any;
  selected?: boolean;
  handleOpprettHerClick?: any;
}) {
  const underliggendeOrgEnhetIder = orgEnhet?.underordnedeOrgEnhetId;

  const underliggendeOrgEnheterQuery = useQuery({
    queryKey: getOrgEnhetList.queryKey(underliggendeOrgEnhetIder ?? []),
    queryFn: () => getOrgEnhetList.queryFn(underliggendeOrgEnhetIder ?? []),
    enabled:
      !!underliggendeOrgEnhetIder && underliggendeOrgEnhetIder.length > 0,
  });

  const underliggendeOrgEnheter = underliggendeOrgEnheterQuery.data ?? [];

  const [selectedId, setSelectedId] = useState<string | null>(null);

  const handleClick = (id: string) => setSelectedId(id);

  return (
    <VStack
      css={css`
        width: 100%;
      `}
      gap="0"
    >
      <OrgHierarkiCard
        name={orgEnhet?.navn ?? ""}
        onClick={() => {
          onClick?.(orgEnhet?.id);
          if (!!selectedId) {
            setSelectedId(null);
          }
        }}
        selected={selected}
      />

      {selected && !selectedId && (
        <VStack
          css={css`
            width: 100%;
            margin-top: var(--a-spacing-4);
          `}
          gap="4"
        >
          {underliggendeOrgEnheter.map((underliggendeOrgEnhet) => (
            <OrgHierarkiStack
              key={underliggendeOrgEnhet.id}
              onClick={handleClick}
              orgEnhet={underliggendeOrgEnhet}
            />
          ))}

          <OpprettEnhetHerButton
            onClick={handleOpprettHerClick}
            plassering={orgEnhet?.id ?? ""}
          />
        </VStack>
      )}

      {selected && !!selectedId && (
        <>
          <OrgHierarkiLine />

          <OrgHierarkiStack
            handleOpprettHerClick={handleOpprettHerClick}
            onClick={handleClick}
            orgEnhet={underliggendeOrgEnheter.find(
              (underliggendeOrgEnhet) =>
                underliggendeOrgEnhet.id === selectedId,
            )}
            selected
          />
        </>
      )}
    </VStack>
  );
}

function OrgHierarkiCard({
  name,
  onClick,
  selected = false,
}: {
  name: string;
  onClick?: any;
  selected?: boolean;
}) {
  return (
    <Button
      css={css`
        display: flex;
        justify-content: center;
        width: 100%;
        padding: ${selected ? "20px 10px 20px" : "10px"};
        background: #cce2f0;
        border-radius: 5px;
        border-top: ${selected ? "7px solid #236b7d" : "none"};
        font-size: var(--a-font-size-medium) !important;
      `}
      onClick={onClick}
      size="small"
      variant="tertiary"
    >
      {name}
    </Button>
  );
}

function OpprettEnhetHerButton({
  plassering,
  onClick,
}: {
  plassering: string;
  onClick?: any;
}) {
  return (
    <Button
      css={css`
        justify-content: space-between;
      `}
      icon={<PlusCircleIcon fontSize="36" title="Opprett ny enhet her" />}
      iconPosition="right"
      onClick={() => onClick?.(plassering)}
      variant="secondary"
    >
      Opprett ny
      <br />
      enhet her
    </Button>
  );
}

function OrgHierarkiLine() {
  return (
    <div
      css={css`
        margin: auto;
        width: 4px;
        height: var(--a-spacing-4);
        background: #236b7d;
      `}
    />
  );
}

export function OrgEnhetInfoWrapper({
  orgEnhetId,
  heading,
}: {
  orgEnhetId?: string;
  heading: string;
}) {
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

  return (
    !!orgEnhet && (
      <VStack gap="2">
        {!!heading && (
          <Heading level="2" size="small">
            {heading}
          </Heading>
        )}

        <div
          css={css`
            background-color: #deecef;
            padding: var(--a-spacing-4);
            border-radius: var(--a-border-radius-xlarge);
            display: grid;
          `}
        >
          <OrgEnhetInfo orgEnhet={orgEnhet} />
        </div>
      </VStack>
    )
  );
}

const validationSchema = yup.object({
  navn: yup.string().required("Obligatorisk"),
  gyldigFom: yup.string().required("Obligatorisk"),
  lederRessursNavident: yup.string().required("Obligatorisk"),
  firesiffer: yup.string().required("Obligatorisk"),
  orgEnhetsType: yup
    .mixed<OrgEnhetsType>()
    .oneOf(Object.values(OrgEnhetsType), "Obligatorisk"),
});
