package thz.lang.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record HoverResponse(
        String conteudo,
        HoverRange range
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record HoverRange(int linha, int coluna, int comprimento) {}
}
