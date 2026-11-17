-- Ved rollback gjenopprettes opprinnelig JSON fra backup, ikke ved en generell omvendt navneendring.
LOCK TABLE outbox IN SHARE ROW EXCLUSIVE MODE;

DO
$$
    BEGIN
        IF EXISTS (SELECT 1
                   FROM outbox
                   WHERE jsonb_typeof(orgenhet) = 'object'
                     AND (
                       (
                           orgenhet ? 'erLovnode'
                               AND orgenhet ? 'lovnode'
                               AND (orgenhet -> 'erLovnode') IS DISTINCT FROM (orgenhet -> 'lovnode')
                           )
                           OR (
                           orgenhet ? 'overOrgEnheter'
                               AND orgenhet ? 'overenheter'
                               AND (orgenhet -> 'overOrgEnheter') IS DISTINCT FROM (orgenhet -> 'overenheter')
                           )
                       )) THEN
            RAISE EXCEPTION 'Motstridende feltverdier i outbox.orgenhet. Migreringen er avbrutt.';
        END IF;
    END;
$$;

UPDATE outbox
SET orgenhet = (orgenhet - 'erLovnode' - 'overOrgEnheter')
                   || CASE
                          WHEN orgenhet ? 'erLovnode' THEN jsonb_build_object('lovnode', orgenhet -> 'erLovnode')
                          ELSE '{}'::jsonb
                   END
    || CASE
           WHEN orgenhet ? 'overOrgEnheter' THEN jsonb_build_object('overenheter', orgenhet -> 'overOrgEnheter')
           ELSE '{}'::jsonb
                   END
WHERE jsonb_typeof(orgenhet) = 'object'
  AND (orgenhet ? 'erLovnode' OR orgenhet ? 'overOrgEnheter');
