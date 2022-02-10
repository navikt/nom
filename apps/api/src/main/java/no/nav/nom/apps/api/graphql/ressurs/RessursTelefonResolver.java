package no.nav.nom.apps.api.graphql.ressurs;

import graphql.execution.DataFetcherResult;
import graphql.kickstart.tools.GraphQLResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.nav.nom.apps.api.dvh.DvhRepository;
import no.nav.nom.apps.api.dvh.dto.DvhTelefon;
import no.nav.nom.contract.nom.api.graphql.Ressurs;
import no.nav.nom.contract.nom.api.graphql.Telefon;
import no.nav.nom.contract.nom.api.graphql.TelefonType;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;


@Component
@RequiredArgsConstructor
@Slf4j
public class RessursTelefonResolver implements GraphQLResolver<Ressurs> {

    private final DvhRepository dvhRepository;

    public DataFetcherResult<List<Telefon>> telefon(Ressurs ressurs) {

        List<DvhTelefon> tlfList = dvhRepository.getTelefonNrListByNavIdent(ressurs.getNavIdent());

        return DataFetcherResult.<List<Telefon>>newResult()
                .data(tlfList.stream()
                        .map(tlf -> telefonFromDvhTelefon(tlf, ressurs))
                        .filter(Objects::nonNull)
                        .toList())
                .build();
    }

    private Telefon telefonFromDvhTelefon(DvhTelefon it, Ressurs resKontekst){
        if(it == null){
            log.warn("DvhTelefon var <null> for ident " + resKontekst.getNavIdent());
            return null;
        }
            TelefonType telefonType = null;
            switch(it.getType()){
                case TJENESTETELEFON -> {
                    telefonType = TelefonType.NAV_TJENESTE_TELEFON;
                }
                case KONTORTELEFON -> {
                    telefonType = TelefonType.NAV_KONTOR_TELEFON;
                }
                default -> {
                    log.warn("Setter nr til privattelefon for " + it.getNavIdent());
                    telefonType = TelefonType.PRIVAT_TELEFON;
                }
            }
            return new Telefon(it.getNummer(),telefonType,"");
    }
}
