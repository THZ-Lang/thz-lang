package thz.lang.api.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record SimdResultadoApi(
                String loopIdentificador,
                String variavel,
                int passoSimd,
                boolean vetorizavel,
                List<String> regrasAtendidas,
                List<String> violacoes,
                List<String> avisos) {
}
