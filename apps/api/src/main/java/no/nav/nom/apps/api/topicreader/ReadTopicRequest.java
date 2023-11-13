package no.nav.nom.apps.api.topicreader;

import io.swagger.v3.oas.annotations.media.Schema;

public record ReadTopicRequest(
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) int maxRecords,
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long fromOffset,
    @Schema(requiredMode = Schema.RequiredMode.NOT_REQUIRED) int partition
) { }
