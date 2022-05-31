import { mannMedFjes } from "./Images"

const imageSrc = (navIdent: string) => `https://nom-ui2.intern.nav.no/teamcat/resource/${navIdent}/photo`

type UserImageProps = {
    navIdent?: string
}

const UserImage = (props: UserImageProps) => {
    const { navIdent } = props



    return <img 
              src={navIdent ? imageSrc(navIdent) : mannMedFjes}
              sizes="65px"
           />
}

export default UserImage