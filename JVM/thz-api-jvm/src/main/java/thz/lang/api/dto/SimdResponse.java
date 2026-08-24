package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimdResponse(
        List<SimdResultadoApi> resultados
) {}
