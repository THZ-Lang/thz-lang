package thz.lang.api.dto;

import jakarta.validation.constraints.NotBlank;

public record ExecucaoRequest(
        @NotBlank(message = "Campo 'fonte' é obrigatório")
        String fonte,
        String operacao
) {}
