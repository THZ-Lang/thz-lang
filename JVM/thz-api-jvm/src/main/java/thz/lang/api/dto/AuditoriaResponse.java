package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AuditoriaResponse(
        String relatorioJson,
        String markdown
) {}
