import { NavLink } from 'react-router-dom'
import ContentWrapper from '../components/ContentWrapper'
import * as Gq from '../api/generated/graphql_generated'
import React from 'react'

export function NotfoundPage() {
  return (
    <ContentWrapper>
      <>
        404. Undersiden fantes ikke. Gå <NavLink to='/'>hjem</NavLink>
      </>
    </ContentWrapper>
  )
}
