package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimboloApi(
        String nome,
        String categoria,
        String detalhe,
        int linha,
        int coluna,
        String container
) {}
