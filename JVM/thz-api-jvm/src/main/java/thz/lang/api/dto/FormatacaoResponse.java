package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record FormatacaoResponse(
        String resultado,
        boolean alterou
) {}
