package thz.lang.api.dto;

import jakarta.validation.constraints.NotBlank;

public record AnaliseRequest(
        @NotBlank(message = "Campo 'fonte' é obrigatório")
        String fonte,
        Boolean estrito
) {
    public AnaliseRequest {
        if (estrito == null) estrito = false;
    }
}
