import { NavLink } from 'react-router-dom'
import ContentWrapper from '../components/ContentWrapper'
import * as Gq from '../api/generated/graphql_generated'
import React from 'react'

export function NotfoundPage() {
  const [toggle, setToggle] = React.useState<boolean>(false)

  const x = Gq.useHentAlleOrganisasjonsenheterQuery({
    skip: toggle,
  })
  console.log({ x })
  return (
    <ContentWrapper>
      <>
        <button onClick={() => setToggle(!toggle)}>
          heihei toggle ( {toggle ? 'true' : 'false'} )
        </button>
        404. Undersiden fantes ikke. Gå <NavLink to='/'>hjem</NavLink>
      </>
    </ContentWrapper>
  )
}
