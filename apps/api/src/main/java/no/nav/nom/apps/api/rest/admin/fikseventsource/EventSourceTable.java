package no.nav.nom.apps.api.rest.admin.fikseventsource;

public enum EventSourceTable {
    RESSURS("T_RESSURS_EVENTSOURCE", "${app.kafka.ressurs.eventsourceTopic}"),
    ORGENHET("T_ORGENHET_EVENTSOURCE", "${app.kafka.orgEnhet.eventsourceTopic}"),
    ORGENHET_LEDER("T_ORGENHET_LEDER_EVENTSOURCE", "${app.kafka.orgEnhetLeder.eventsource-topic}"),
    ORGTILKNYTNING("T_ORGTILKNYTNING_EVENTSOURCE", "${app.kafka.orgtilknytning.eventsource-topic}"),
    ORGANISERING("T_ORGANISERING_EVENTSOURCE", "${app.kafka.organisering.eventsourceTopic}"),
    AUTHORIZE("T_AUTHORIZE_EVENTSOURCE", "${app.kafka.authorize.eventsource-topic}");

    private final String tableName;
    private final String topicPropertyKey;

    EventSourceTable(String tableName, String topicPropertyKey) {
        this.tableName = tableName;
        this.topicPropertyKey = topicPropertyKey;
    }

    public String getTableName() {
        return tableName;
    }

    public String getTopicPropertyKey() {
        return topicPropertyKey;
    }
}

