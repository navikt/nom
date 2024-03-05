package no.nav.nom.apps.nomremedyintegrasjon.fysiskbrukersted.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

import java.util.List;

@org.springframework.stereotype.Repository
public interface FysiskBrukerstedReadOnlyRepository extends Repository<FysiskBrukersted, String> {

    @Query(
        nativeQuery = true,
        value = """
        select *
        from ARADMIN.FYSISK_BRUKERSTED
        where STATUS = 0 and GYLDIG_SOM_FYSISK_PLASSERING_A = 0
        """
    )
    List<FysiskBrukersted> findAll();

    @Query(
        nativeQuery = true,
        value = """
        select *
        from ARADMIN.FYSISK_BRUKERSTED
        where STATUS = 0 and GYLDIG_SOM_FYSISK_PLASSERING_A = 0
        and BRUKERSTED_NR in :brukerstedNrs
        """
    )
    List<FysiskBrukersted> findByBrukerstedNrIn(List<String> brukerstedNrs);
}
