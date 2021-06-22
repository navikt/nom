import React, {useState} from 'react';

interface BestillingContextType {
  bestillingSetId?: string
}

export const defaultBestillingContext: BestillingContextType = {
  bestillingSetId: undefined
};

// eslint-disable-next-line @typescript-eslint/no-empty-function
const defaultValue = {bestillingContext: defaultBestillingContext, update: (newContext: Partial<BestillingContextType>) => {}};
export const BestillingContext = React.createContext(defaultValue);

export const useBestillingContext = () => {
  const [bestillingContext, setBestillingContext] = useState(defaultBestillingContext);
  return {bestillingContext, update: (newContext: Partial<BestillingContextType>) => setBestillingContext({...bestillingContext, ...newContext})};
};
