package thz.lang.api.dto;

import jakarta.validation.constraints.NotBlank;

public record HoverRequest(
        @NotBlank(message = "Campo 'fonte' é obrigatório")
        String fonte,
        int linha,
        int coluna
) {}
