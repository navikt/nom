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
                type="text"
                label=""
                hideLabel
                className={css({width: "500px"})}
                {...register("searchValue")}
            />
            <Button variant="tertiary"><img src={searchIcon} /></Button>
        </div>
    )
}

export default SearchBar