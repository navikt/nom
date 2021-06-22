import Nompagewrap from '../../../components/nompagewrap/Nompagewrap';
import React from 'react';
import Contentbox from '../../../components/contentbox/Contentbox';
import {gql, useQuery} from '@apollo/client';

const bestillingQuery = gql`
    query bestillinger {
        bestillingSets {
            bestillingSetId
            activateTime
            beskrivelse
            bestillinger {
                bestillingId
                metadata {
                    aktivering
                    beskrivelse
                }
                type
                data
            }
            sistEndret
        }
    }
`;

export enum Bestillingsaktiving {
  IMMEDIATE = 'IMMEDIATE',
  FOR_APPROVAL = 'FOR_APPROVAL'
}

export interface BestillingMetadata {
  bestillingSetId: string
  aktivering: Bestillingsaktiving
  beskrivelse: string
}

export interface Bestilling {
  bestillingId: string
  bestillingSetId: string
  metadata: BestillingMetadata
  type: string
  data: any
}

export interface BestillingSet {
  bestillingSetId: string
  activateTime: string
  beskrivelse: string
  bestillinger: Bestilling[]
  sistEndret: string
}


export const BestillingList = (): JSX.Element => {
  const {data} = useQuery<Bestilling[]>(bestillingQuery);


  return (
    <Nompagewrap pagetitle="Bestillinger">
      <Contentbox label={'Totalt antall'}>
        <div>{data?.length}</div>
      </Contentbox>
    </Nompagewrap>
  );
};