import React, {useState} from 'react';

interface BestillingContextType {
  bestillingSetId?: string
}

export const defaultBestillingContext: BestillingContextType = {
  bestillingSetId: undefined
};

const defaultValue = {
// eslint-disable-next-line @typescript-eslint/no-empty-function
  bestillingContext: defaultBestillingContext, update: (newContext: Partial<BestillingContextType>): void => {
  }
};
export const BestillingContext = React.createContext(defaultValue);

type BestillingContextHookType = {
  bestillingContext: BestillingContextType,
  update: (newContext: Partial<BestillingContextType>) => void
};

export const useBestillingContext = (): BestillingContextHookType => {
  const [bestillingContext, setBestillingContext] = useState(defaultBestillingContext);
  return {
    bestillingContext,
    update: (newContext: Partial<BestillingContextType>) => setBestillingContext({...bestillingContext, ...newContext})
  };
};
