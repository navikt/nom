package no.nav.nom.apps.api.orgtilknytning.u4diff;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class U4DiffService {
    final U4DiffRepository u4DiffRepository;

    public U4DiffService(U4DiffRepository u4DiffRepository) {
        this.u4DiffRepository = u4DiffRepository;
    }

    public List<U4Diff> getStatligeIkkeIU4() {
        return u4DiffRepository.findByType(U4DiffType.MANGLENDE_STATLIG_I_U4);
    }
}
