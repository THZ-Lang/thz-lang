package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DocumentacaoResponse(
        String markdown
) {}
