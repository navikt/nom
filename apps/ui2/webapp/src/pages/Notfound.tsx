import { NavLink } from "react-router-dom";
import { Pagewrap } from "../components/Pagewrap";

export function Notfound(){
    return <Pagewrap>
        404. Undersiden fantes ikke.
        Gå <NavLink to="/">hjem</NavLink>
    </Pagewrap>
}