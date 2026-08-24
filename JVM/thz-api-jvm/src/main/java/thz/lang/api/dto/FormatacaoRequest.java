package thz.lang.api.dto;

import jakarta.validation.constraints.NotBlank;

public record FormatacaoRequest(
        @NotBlank(message = "Campo 'fonte' é obrigatório")
        String fonte
) {}
