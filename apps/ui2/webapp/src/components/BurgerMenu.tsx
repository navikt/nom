import { css } from '@emotion/css'
import { Hamburger } from '@navikt/ds-icons'
import { Button, Label } from '@navikt/ds-react'
import React from 'react'
import { Link } from 'react-router-dom'
import { opprettRessursIcon, orgIcon } from './Images'

const BurgerButton = css({
  display: 'flex',
  width: '7rem',
  height: '2.5rem',
  padding: '0.5rem',
  // ':hover': { display: 'block' },
})

const buttonIcon = css({
  marginRight: '0.4rem',
})

const linkStyle = css({
  display: 'flex',
  textDecoration: 'none',
  color: '#262626',
  padding: '1rem',
  borderBottom: 'solid 1px #E5E5E5',
})

const dropdownContent = css({
  // display: 'none',
  position: 'absolute',
  backgroundColor: '#ffffff',
  minWidth: '160px',
  boxShadow: '0px 8px 16px 0px rgba(0,0,0,0.2)',
  zIndex: '1',
})

const dropDown = css({
  position: 'relative',
  display: 'inline-block',
})

const linkImage = css({
  marginRight: '1rem',
})

const ListItem = (props: { label: string; url: string; icon: string }) => (
  <Link to={props.url} className={linkStyle}>
    <img src={props.icon} alt={props.label + '- icon'} className={linkImage} />
    <Label>{props.label}</Label>
  </Link>
)

const BurgerMenu = () => {
  const [showMenu, setShowMenu] = React.useState<boolean>(false)
  return (
    <div className={dropDown}>
      <Button
        size='medium'
        variant='secondary'
        className={BurgerButton}
        onClick={() => (showMenu ? setShowMenu(false) : setShowMenu(true))}
      >
        <Hamburger className={buttonIcon} />
        <b>Menu</b>
      </Button>

      {showMenu && (
        <div className={dropdownContent}>
          <ListItem label='Organisasjonen' url='/org' icon={orgIcon} />
          <ListItem
            label='Opprett ressurs'
            url='/ressurs/create'
            icon={opprettRessursIcon}
          />
        </div>
      )}
    </div>
  )
}

export default BurgerMenu
