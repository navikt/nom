import { css } from '@emotion/css'
import { useForm } from 'react-hook-form'
import * as Gq from '../api/generated/graphql_generated'
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router'
import ContentWrapper from '../components/ContentWrapper'
import ResourceFormPanel from '../components/Resource/ResourceFormPanel'
import { opprettRessursIkon } from '../components/Images'
import { BodyShort, Heading, Search } from '@navikt/ds-react'
import ResourceMetadata from '../components/Resource/ResourceMetadata'
import ResourceForm from '../components/Resource/ResourceForm'

const CreateResourceView = () => {
  const { register, handleSubmit, formState: { errors } } = useForm()
  const navigate = useNavigate()
  const [searchStr, setSearchStr] = useState<string | undefined>()

  const query = Gq.useHentPersonQuery({ variables: { personIdent: searchStr || ''}, errorPolicy: 'all'})

  const [createResource, createResourceStatus] = Gq.useOpprettRessurs2Mutation()

  //Ikke ferdig - Mangler feilhåndtering
  const handleCreateResource = (values: Gq.OpprettRessursRequest2) => {
    console.log(values, 'Values')
    if (!searchStr) return
    
    createResource({
      variables: { request: { personident: searchStr, fornavn: values.fornavn, etternavn: values.etternavn}},
      onCompleted: (data) => {
          console.log('Skal redirectes til kvitteringsside...', data)
          setSearchStr(undefined)

          navigate('/ressurs/' + data.opprettRessurs2?.navident)
      },
      onError: (error) => console.log('ERROR', error),
    })
  }

  useEffect(() => {
    console.log(errors, "ERROR")
    if (query.data?.person?.navIdent)
      navigate('/ressurs/' + query.data?.person?.navIdent, {
        state: { alreadyExists: true },
      })
  }, [query])


  return (
    <ContentWrapper>
      <ResourceFormPanel>
        <div className={css({width: '100%', textAlign: 'center', marginBottom: '2rem'})}>
          <div><img src={opprettRessursIkon} alt="Opprett ressurs ikon" /></div>
          <Heading size="large">Opprett ressurs</Heading>
        </div>

        {/* Søkefelt */}
        <form onSubmit={handleSubmit((form: any) => { setSearchStr(form.searchValue)})}>
          <div>
            <BodyShort size="medium" className={css({marginBottom: '0.5rem'})}>Fyll inn personnummer/D-nummer så finner vi personen i folkeregisteret</BodyShort>
            <Search label="Søk etter person" variant='secondary' {...register('searchValue', { required: true, minLength: 11, maxLength: 11})} />
            
            {errors.searchValue?.type === ('minLength' || 'maxLength') && 
                <BodyShort className={css({color: 'red', marginTop: '1rem'})}>Personnummer/D-nummer må være 11 siffer.</BodyShort>
            }

            {/* Viser feilmelding ved søk */}
            {!errors.searchValue && !query.loading && query.error && searchStr?.length === 11 && <BodyShort className={css({color: 'red', marginTop: '1rem'})}>{query.error.message}</BodyShort>}  
          </div>
        </form>
        
        
        {!query.loading && query.data && query.data.person?.person && !query.data.person?.navIdent && 
          <>
            {/* Viser opplysninger fra folkeregisteret */}
            <ResourceMetadata resource={query.data.person.person as Gq.Person}/>

            {/* Form - Fylles ut for oppretting av ressurs */}
            <ResourceForm 
                onSubmit={(values: any) => handleCreateResource(values)}
                initialValues={{
                  fornavn: query.data.person.person.navn.fornavn,
                  mellomnavn: query.data.person.person.navn.mellomnavn || '',
                  etternavn: query.data.person.person.navn.etternavn
                }}
            />
          </>
        }
        
      </ResourceFormPanel>
    </ContentWrapper>
  )
}

export default CreateResourceView
