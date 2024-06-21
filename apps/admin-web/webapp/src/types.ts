export type Visningsnavn = {
  etternavn: string;
  fornavn: string;
  mellomnavn?: string;
};

export type KafkaEvent = { metadata: MetaData; [key: string]: unknown };

export type MetaData = {
  inputMetadata: InputMetadata;
  createdTimestamp: string;
};

export type InputMetadata = {
  inputInterfaceName: string;
  inputInterfaceMethodName: string;
  inputInterfaceMethodArguments: { [key: string]: unknown };
  origination: Origination;
};

export type Origination = {
  initiatedBy: string;
  initiatedFrom: string;
};
