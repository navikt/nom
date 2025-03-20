package no.nav.nom.apps.admin.sanitycheck.remedycompare;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RemedyDataFileRepository {
    private final List<ImportedRemedyUser> remedyUsers = new ArrayList<>();
    private final Map<String, ImportedRemedyUser> remedyUsersByNavident = new HashMap<>();

    private final ImportRemedyDataFile importRemedyDataFile;

    public RemedyDataFileRepository(ImportRemedyDataFile importRemedyDataFile) {
        this.importRemedyDataFile = importRemedyDataFile;
    }

    public ImportedRemedyUser getRemedyUser(String navident) {
        loadIfEmpty();
        return remedyUsersByNavident.get(navident);
    }

    public Map<String, ImportedRemedyUser> getRemedyUsers() {
        loadIfEmpty();
        return remedyUsersByNavident;
    }

    private void loadIfEmpty() {
        if (remedyUsers.isEmpty()) {
            var imported = importRemedyDataFile.importfromBucket();
            remedyUsers.addAll(imported);
            imported.stream()
                .filter(remedyUser -> remedyUser.navident() != null)
                .sorted(Comparator.comparing(ImportedRemedyUser::regDato))
                .forEach(remedyUser -> remedyUsersByNavident.put(remedyUser.navident(), remedyUser));
        }
    }

    public void reload() {
        remedyUsers.clear();
        remedyUsersByNavident.clear();
        loadIfEmpty();
    }
}
