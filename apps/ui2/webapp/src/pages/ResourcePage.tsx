import * as Gq from '../api/generated/graphql_generated'
import { useParams } from 'react-router'
import ContentWrapper from '../components/ContentWrapper'
import BannerSmall from '../components/BannerSmall'
import { Table } from '@navikt/ds-react'

const ResourcePage = () => {
  const { id } = useParams<any>()
  const query = Gq.useHentRessursMedNavIdentQuery({
    variables: { navIdent: id },
  })

  return (
    <div>
      <BannerSmall title={'Organisasjonshierarkiet'} />

      <ContentWrapper>
        <>
          <h1>Ressurser</h1>

          <a href={'/ressurs/create'}>Opprett ny ressurs</a>

          {/* {query.data && <h1>{query.data.ressurs?.navIdent}</h1>}
          {query.error && <p>Fant feil</p>} */}

          <div className='flex w-full flex-col gap-4'>
            <Table size='medium' zebraStripes={false}>
              <Table.Header>
                <Table.Row>
                  <Table.HeaderCell scope='col'>Navn</Table.HeaderCell>
                  <Table.HeaderCell scope='col'>Fødseslnr.</Table.HeaderCell>
                  <Table.HeaderCell scope='col'>Start</Table.HeaderCell>
                </Table.Row>
              </Table.Header>
              <Table.Body>
                <Table.Row>
                  <Table.HeaderCell scope='row'>
                    Jakobsen, Markus
                  </Table.HeaderCell>
                  <Table.DataCell>031292 65463</Table.DataCell>
                  <Table.DataCell>28.04.2021</Table.DataCell>
                </Table.Row>
                <Table.Row>
                  <Table.HeaderCell scope='row'>
                    Halvorsen, Mari
                  </Table.HeaderCell>
                  <Table.DataCell>160636 34134</Table.DataCell>
                  <Table.DataCell>29.01.2022</Table.DataCell>
                </Table.Row>
                <Table.Row>
                  <Table.HeaderCell scope='row'>
                    Christiansen, Mathias
                  </Table.HeaderCell>
                  <Table.DataCell>181244 41438</Table.DataCell>
                  <Table.DataCell>04.06.2021</Table.DataCell>
                </Table.Row>
                <Table.Row>
                  <Table.HeaderCell scope='row'>
                    Fredriksen, Leah
                  </Table.HeaderCell>
                  <Table.DataCell>240890 80180</Table.DataCell>
                  <Table.DataCell>31.08.2021</Table.DataCell>
                </Table.Row>
              </Table.Body>
            </Table>
          </div>
        </>
      </ContentWrapper>
    </div>
  )
}

export default ResourcePage
