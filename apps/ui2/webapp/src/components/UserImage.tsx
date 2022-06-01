import { css } from "@emotion/css"
import React from "react"
import { mannMedFjes } from "./Images"

const imageSrc = (navIdent: string) => `https://nom-ui2.intern.nav.no/teamcat/resource/${navIdent}/photo`

type UserImageProps = {
    navIdent?: string,
    size?: string
}

const UserImage = (props: UserImageProps) => {
    const { navIdent, size } = props
    const [image, setImage] = React.useState(navIdent ? imageSrc(navIdent) : mannMedFjes)

    return <img 
              className={css({ height: size || '65px', width: size || '65px', borderRadius: '100%'})}
              src={navIdent ? imageSrc(navIdent) : mannMedFjes}
           />
}

export default UserImage