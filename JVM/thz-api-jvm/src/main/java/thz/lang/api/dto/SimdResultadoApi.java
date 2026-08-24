package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimdResultadoApi(
        String loopIdentificador,
        String variavel,
        int passoSimd,
        boolean vetorizavel,
        List<String> regrasAtendidas,
        List<String> violacoes,
        List<String> avisos
) {}
