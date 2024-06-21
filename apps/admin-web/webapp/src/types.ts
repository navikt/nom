export type Visningsnavn = {
  etternavn: string;
  fornavn: string;
  mellomnavn?: string;
};

// eslint-disable-next-line @typescript-eslint/no-explicit-any
export type KafkaEvent<DATA = any, ADDITIONALDATA = any> = {
  metadata: MetaData;
  type: string;
  fid: string;
  data: DATA;
  additionalData: ADDITIONALDATA;
};

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
