import Nompagewrap from '../../../components/nompagewrap/Nompagewrap';
import React, {useContext} from 'react';
import Contentbox from '../../../components/contentbox/Contentbox';
import {gql, useQuery} from '@apollo/client';
import TableSorter from "tabell";
import moment from 'moment';
import {BestillingContext} from '../BestillingContext';
import {Knapp} from 'nav-frontend-knapper';
import AlertStripe from 'nav-frontend-alertstriper';

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
    const {data} = useQuery<{bestillingSets: BestillingSet[]}>(bestillingQuery);
    const bestillingContext = useContext(BestillingContext);

    return (
      <Nompagewrap pagetitle="Bestillinger">
        <div style={{margin: '1em'}}>
          <Contentbox label={'Totalt antall'}>
            <div>{data?.bestillingSets.length}</div>
          </Contentbox>

          {data?.bestillingSets.map(set => (
            <Contentbox key={set.bestillingSetId} label={`Bestilling Set ${set.bestillingSetId}`}>
              <div style={{margin: '1em', display: 'flex', justifyContent: 'space-between', alignItems: 'flex-end'}}>
                <div>
                  <div>Bestillinger: {set.bestillinger.length}</div>
                  <div>Beskrivelse: {set.beskrivelse}</div>
                  <div>Sist endrett: {moment(set.sistEndret).format('lll')}</div>
                  <div>Aktivering tid: {set.activateTime ? moment(set.activateTime).format('lll') : ''}</div>
                </div>
                {bestillingContext.bestillingContext.bestillingSetId === set.bestillingSetId ? <AlertStripe type={'info'}>Aktiv bestilling set</AlertStripe> :
                  <Knapp onClick={() => bestillingContext.update({bestillingSetId: set.bestillingSetId})}>Velg</Knapp>}
              </div>
              <TableSorter items={set.bestillinger.map(b => ({...b, key: b.bestillingId}))}
                           columns={[
                             {
                               id: 'key',
                               label: 'ID',
                               type: 'string'
                             },
                             {
                               id: 'type',
                               label: 'Type',
                               type: 'string'
                             },
                             {
                               id: 'beskrivelse',
                               label: 'Beskrivelse',
                               type: 'string',
                               renderCell: (item: Bestilling) => <span>{item.metadata.beskrivelse}</span>
                             },
                             {
                               id: 'aktivering',
                               label: 'Aktivering',
                               type: 'string',
                               renderCell: (item: Bestilling) => <span>{item.metadata.aktivering}</span>
                             },
                             {
                               id: 'data',
                               label: 'Data',
                               type: 'string',
                               renderCell: (_, value) => <pre>{JSON.stringify(value, null, 1)}</pre>
                             },
                           ]}
                           itemsPerPage={10}
                           compact
                           searchable={false}
              />
            </Contentbox>
          ))}

        </div>
      </Nompagewrap>
    );
  }
;