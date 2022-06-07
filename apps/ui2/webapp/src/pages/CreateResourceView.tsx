import { css } from '@emotion/css'
import { useForm } from 'react-hook-form'
import PageTitle from '../components/PageTitle'
import { defaultTextStyles } from '../components/Styles'
import * as Gq from '../api/generated/graphql_generated'
import { useEffect, useState } from 'react'
import PersonDetailCard from '../components/Resource/PersonDetailCard'
import ResourceFormCard from '../components/Resource/ResourceFormCard'
import { useNavigate } from 'react-router'
import ContentWrapper from '../components/ContentWrapper'
import SearchBar from '../components/Resource/SearchBar'

/*const bannerStyles = css({
  background: '#e5e5e5',
  width: '200%',
  display: 'flex',
  marginTop: '-32px',
  marginLeft: '-50%',
  height: '100px',
  overflow: 'hidden',
})

const headerStyle = css({
  marginLeft: '50%',
})*/

const CreateResourceView = () => {
  const { register, handleSubmit } = useForm()
  const navigate = useNavigate()
  const [searchStr, setSearchStr] = useState<string | undefined>()
  const [requestBody, setRequestBody] = useState<
    Gq.OpprettRessursRequest2 | undefined
  >()
  console.log({ searchStr })
  const query = Gq.useHentPersonQuery({
    variables: { personIdent: searchStr || '' },
    // SKIP er/var bugged. Hvis den starter som true kan den ike blie false
    // skip: 11 !== searchStr?.length,
  })
  const [createResource, createResourceStatus] = Gq.useOpprettRessurs2Mutation()

  //Ikke ferdig - Mangler feilhåndtering og onCompleted funksjon
  const handleCreateResource = (values: Gq.OpprettRessursRequest2) => {
    console.log(values, 'Values')
    if (!searchStr) return
    setRequestBody({
      personident: searchStr,
      fornavn: values.fornavn,
      etternavn: values.etternavn,
    })
    createResource({
      variables: {
        request: {
          personident: searchStr,
          fornavn: values.fornavn,
          etternavn: values.etternavn,
        },
      },
      onCompleted: (data) =>
        console.log('Skal redirectes til kvitteringsside...', data),
      onError: (error) => console.log('ERROR', error),
    })
  }

  useEffect(() => {
    if (query.data?.person?.navIdent)
      navigate('/ressurs/' + query.data?.person?.navIdent, {
        state: { alreadyExists: true },
      })
  }, [query])

  return (
    <ContentWrapper>
      <>
        {/*<div className={bannerStyles}/>*/}

        <PageTitle title='Opprett ressurs' />

        <div className={css({ marginTop: '5em', width: '100%' })}>
          <span className={defaultTextStyles}>
            Fyll inn personnummer/D-nummer så finner vi personen i
            folkeregisteret
          </span>
          <form
            onSubmit={handleSubmit((form: any) => {
              setSearchStr(form.searchValue)
              console.log('dette skjer')
            })}
          >
            <SearchBar register={register} />
          </form>

          {query.loading && <p>Loading...</p>}
          {!query.loading && !query.data && query.error && (
            <p>{query.error.message}</p>
          )}

          {/* Er ansatt i NAV - redirect til ressursvisning*/}
          {!query.loading &&
            query.data &&
            query.data.person?.navIdent &&
            history.pushState(
              { alreadyExists: true },
              '',
              '/ressurs/' + query.data.person?.navIdent
            )}

          {/* Finnes ikke i NAV fra før */}
          {!query.loading &&
            query.data &&
            !query.data.person?.navIdent &&
            query.data.person?.person && (
              <>
                <PersonDetailCard
                  cardTitle='Opplysninger fra folkeregisteret'
                  person={query.data.person}
                />
                <ResourceFormCard
                  cardTitle='NOM opplysninger/Andre opplysninger/Opplysninger til NOM '
                  handleCreate={(values: any) => handleCreateResource(values)}
                />
              </>
            )}
        </div>
      </>
    </ContentWrapper>
  )
}

export default CreateResourceView
