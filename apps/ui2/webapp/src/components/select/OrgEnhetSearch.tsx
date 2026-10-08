import {
  AsyncSearch,
  SelectLayoutWrapper,
} from "~/components/select/CustomSelectComponents";
import axios from "axios";
import { useController } from "react-hook-form";

export function OrgEnhetSearch({
  name,
  label,
}: {
  name: string;
  label: string;
}) {
  const { field, fieldState } = useController({
    name,
  });

  return (
    <SelectLayoutWrapper error={fieldState.error} htmlFor="Leder" label={label}>
      <AsyncSearch
        loadOptions={(searchTerm) => searchIfEnoughInput(searchTerm)}
        noOptionsMessage={({ inputValue }: { inputValue: string }) =>
          inputValue.toLowerCase().replace("nav ", "").trim().length < 3
            ? "Du må skrive lenger for å søke"
            : `Fant ingen resultater for "${inputValue}"`
        }
        onChange={(selectedOption: { label: string; value: string } | null) => {
          field.onChange(selectedOption?.value ?? "");
        }}
        placeholder="Søk etter organisasjonsenhet"
      />
    </SelectLayoutWrapper>
  );
}

async function searchIfEnoughInput(searchTerm: string) {
  const newSearchTerm = searchTerm.toLowerCase().replace("nav ", "").trim();

  if (newSearchTerm.length < 3) {
    return [];
  }

  const searchQuery = `
    query orgEnhet($searchTerm: String!) {
      searchOrgEnhet(term: $searchTerm filter: {gyldigTomSelection: IKKE_PASSERT}) 
      {
        id
        navn
      }
    }
  `;

  const resultRaw = await axios.post("/frackend/nom-api/graphql", {
    operationName: "orgEnhet",
    query: searchQuery,
    variables: {
      searchTerm: newSearchTerm,
    },
  });

  const resultOrgEnheter = resultRaw.data.data.searchOrgEnhet;

  return resultOrgEnheter.map((orgEnhet: any) => ({
    label: `${orgEnhet.navn} (${orgEnhet.id})`,
    value: orgEnhet.id,
  }));
}
