package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record DiagnosticoApi(
        int linha,
        int coluna,
        String mensagem,
        String origem,
        String severidade
) {}
