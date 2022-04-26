import { Header } from "@navikt/ds-react-internal"

export const headerHeight = '3rem'

const NavHeader = () => {

    return (
        <Header className="w-full" style={{ height: headerHeight }}>
            <Header.Title as="h1">NOM</Header.Title>
            <Header.User
                name="Navn her"
                style={{ marginLeft: "auto" }}
            />
      </Header>
    )
}

export default NavHeader