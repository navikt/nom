import { css } from "@emotion/css"
import { NavLink } from "react-router-dom"

const linkStyle = css({
    width: '100%'
}) 

const Link = (props: {linkTo: string, text: string}) => {
    return (
        <NavLink to={props.linkTo}>{props.text}</NavLink>
    )
}

const SideBar = () => {

    return (
        <div className={css({width: '330px'})}>
            <div className={css({width: '100%'})}>
                <Link linkTo="/" text="Lenke" />
                <p>Organisasjonsenheter</p>
                <p>Medarbeidere</p>
                <p>Option 3</p>
                <p>Option 4</p>
                <p>Option 5</p>
            </div>
            
        </div>
    )
}

export default SideBar