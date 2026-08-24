package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AnaliseResponse(
        List<DiagnosticoApi> diagnosticos,
        List<String> textoDiagnosticos,
        boolean temErros,
        List<SimboloApi> simbolos,
        String astJson
) {}
