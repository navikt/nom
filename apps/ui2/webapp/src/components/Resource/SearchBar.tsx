import {Button, TextField} from "@navikt/ds-react";
import {css} from "@emotion/css";
import {searchIcon} from "../Images";

const searchStyles = css({
    marginTop: "1em",
    width: "100%",
    display: "flex",
});

const SearchBar = (props: { register: any }) => {
    const register = props.register
    return (
        <div className={searchStyles}>
            <TextField
                disabled={true}
                type="text"
                label=""
                hideLabel
                placeholder={"Søkefunksjonalitet kommer"}
                className={css({width: "100%"})}
                {...register("searchValue")}
            />
            <Button variant="primary"><img src={searchIcon} /></Button>
        </div>
    )
}

export default SearchBar