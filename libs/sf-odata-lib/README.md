# sf-odata-lib

Bibliotek for typesikker tilgang til SAP SuccessFactors OData v2-tjenester via SAP Cloud SDK VDM (Virtual Data Model).

Modulen konfigurerer SAP Cloud SDK sin OData v2 Maven-plugin til å generere Java-klasser fra EDMX-metadata-filer.

## Forutsetninger

- Java 25
- Maven

## Mappestruktur

```
nedlastede-edmx-filer/   ← arkiv: lagre alle nedlastede EDMX-filer her
edmx-source-input/       ← input til generatoren: kopier hit de filene du vil generere kode fra
src/generated/java/      ← generert VDM-kode som sjekkes inn i git
```

## Legge til nye tjenester

1. Last ned EDMX-metadata-filen fra SuccessFactors OData-tjenesten:
   ```
   GET https://<sf-host>/odata/v2/<ServiceName>/$metadata
   ```
2. Lagre filen i `nedlastede-edmx-filer/` som arkiv, f.eks.:
   ```
   nedlastede-edmx-filer/MyService.xml
   ```
3. Kopier filen til `edmx-source-input/` for å inkludere den i kodegenerering:
   ```
   edmx-source-input/MyService.xml
   ```

> **Merk:** Generatoren scanner `edmx-source-input/` rekursivt. Legg kun fullstendige EDMX-filer direkte i denne mappen — ikke legg duplikater i underkataloger.

## Generere VDM-kilder

Genereringen er skrudd av som standard. Pluginen kjører kun når profilen `generate-odata` er aktiv.

For regenerering med rent output (anbefalt etter EDMX-endringer):

```bash
JAVA_TOOL_OPTIONS="" mvn clean install -pl libs/sf-odata-lib -am -Pgenerate-odata -Dmaven.build.cache.skipCache=true
```

Kun generering (uten full build):

```bash
JAVA_TOOL_OPTIONS="" mvn generate-sources -pl libs/sf-odata-lib -am -Pgenerate-odata
```

Vanlig bygg uten regenerering:

```bash
JAVA_TOOL_OPTIONS="" mvn install -pl libs/sf-odata-lib -am
```

Genererte klasser havner i `src/generated/java/` og kompileres i vanlig bygg.

Dette betyr:

- `mvn clean install` fungerer uten å kjøre generatoren.
- Regenerering skjer kun når du eksplisitt aktiverer `-Pgenerate-odata`.

## Konfigurasjon

Plugin-parametere i `pom.xml`:

| Parameter                   | Verdi                                      | Beskrivelse                                        |
|-----------------------------|--------------------------------------------|----------------------------------------------------|
| `inputDirectory`            | `${project.basedir}/edmx-source-input`     | Mappe med EDMX-filer som genereres (kun OData v2)  |
| `outputDirectory`           | `${project.basedir}/src/generated/java` | Mappe for genererte Java-klasser (versjonert) |
| `packageName`               | `no.nav.nom.sf.odata.vdm`                 | Pakkenavn for generert kode                        |
| `defaultBasePath`           | `odata/v2/`                                | Standard basesti for tjenestene                    |
| `deleteOutputDirectory`     | `true`                                     | Sletter output-mappen ved ny generering            |
| `compileScope`              | `COMPILE`                                  | Legger genererte kilder til kompilering            |
| `serviceMethodsPerEntitySet`| `true`                                     | Genererer metoder per entity set                   |
| `nameSource`                | `NAME`                                     | Bruker `Name`-attributtet (ikke `Label`) for navn  |

Se full parameterliste i [SAP Cloud SDK-dokumentasjonen](https://sap.github.io/cloud-sdk/docs/java/features/odata/vdm-generator#available-parameters).

## Bruke generert kode

Legg til modulen som dependency i den aktuelle `pom.xml`:

```xml
<dependency>
    <groupId>no.nav.nom</groupId>
    <artifactId>sf-odata-lib</artifactId>
    <version>${project.version}</version>
</dependency>
```

Eksempel på bruk av generert klient:

```java
MyServiceV4 service = new MyServiceV4()
    .withServicePath("/odata/v4/MyService");

List<MyEntity> entities = service.getAllMyEntity()
    .top(10)
    .executeRequest(destination);
```

> **Merk:** Du må typisk overstyre tjenestestien med `.withServicePath(...)` da generatoren bruker `namespace`-verdien fra EDMX som standard.

## OData v2 vs v4

Denne modulen bruker OData **v2**-generatoren. Dersom SuccessFactors-tjenesten er OData v4, bytt til:

- Plugin: `odata-v4-generator-maven-plugin`
- Dependency: `odata-v4-core`
- `defaultBasePath`: `odata/v4/`
