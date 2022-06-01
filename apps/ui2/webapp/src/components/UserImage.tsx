import { css } from "@emotion/css"
import { mannMedFjes } from "./Images"

const imageSrc = (navIdent: string) => `https://nom-ui2.intern.nav.no/teamcat/resource/${navIdent}/photo`

type UserImageProps = {
    navIdent?: string,
    size?: string
}

const UserImage = (props: UserImageProps) => {
    const { navIdent, size } = props

    return <img 
              src={navIdent ? imageSrc(navIdent) : mannMedFjes}
              className={css({ height: size || '65px', width: size || '65px', borderRadius: '100%'})}
           />
}

export default UserImage