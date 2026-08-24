package thz.lang.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnaliseResponse(
                List<DiagnosticoApi> diagnosticos,
                List<String> textoDiagnosticos,
                boolean temErros,
                List<SimboloApi> simbolos,
                String astJson) {
}
