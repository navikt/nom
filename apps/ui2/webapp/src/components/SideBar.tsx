import { css } from "@emotion/css"
import { NavLink } from "react-router-dom"

const Link = (props: {linkTo: string, text: string}) => {
    return (
        <NavLink to={props.linkTo}>{props.text}</NavLink>
    )
}

const SideBar = () => {
    return (
        <div className={css({width: '300px', marginTop: '2rem', paddingLeft: '1rem'})}>
            <div className={css({width: '100%'})}>
                <Link linkTo="/" text="Lenke" />
                <p>Organisasjonsenheter</p>
                <p>Medarbeidere</p>
                <p>Option 3</p>
            </div>
            
        </div>
    )
}

export default SideBar