package thz.lang.lexico;

import java.util.ArrayList;
import java.util.List;

public class ThzLexer {
    private final String input;
    private int pos = 0;
    private int line = 1;
    private int col = 1;

    public ThzLexer(String input) {
        this.input = input;
    }

    public List<Token> tokenize() {
        List<Token> tokens = new ArrayList<>();
        // Tolerância Dual-OS: descarta BOM UTF-8 (U+FEFF) no início do arquivo.
        if (pos < input.length() && input.charAt(pos) == '\uFEFF') advance();
        while (pos < input.length()) {
            char c = input.charAt(pos);
            if (c == ' ' || c == '\t' || c == '\r') { advance(); continue; }
            if (c == '\n') { line++; col = 1; pos++; continue; }
            if (c == '#') {
                while (pos < input.length() && input.charAt(pos) != '\n') advance();
                continue;
            }
            if (c == '"') { tokens.add(readString()); continue; }
            if (Character.isDigit(c)) { tokens.add(readNumber()); continue; }
            if (c == ':') { tokens.add(make(TokenType.DOIS_PONTOS, ":")); advance(); continue; }
            if (c == '=') { tokens.add(make(TokenType.OPERADOR_RELACIONAL, "=")); advance(); continue; }
            if (c == '.') { tokens.add(make(TokenType.PONTO, ".")); advance(); continue; }
            if (c == ',') { tokens.add(make(TokenType.VIRGULA, ",")); advance(); continue; }
            if (c == '(') { tokens.add(make(TokenType.ABRE_PARENTESE, "(")); advance(); continue; }
            if (c == ')') { tokens.add(make(TokenType.FECHA_PARENTESE, ")")); advance(); continue; }
            if (c == '[') { tokens.add(make(TokenType.ABRE_COLCHETE, "[")); advance(); continue; }
            if (c == ']') { tokens.add(make(TokenType.FECHA_COLCHETE, "]")); advance(); continue; }
            if (c == '%') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "%")); advance(); continue; }
            if (c == '+') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "+")); advance(); continue; }
            if (c == '-') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '>') {
                    tokens.add(make(TokenType.SETA_CASO, "->"));
                    advance(); advance(); continue;
                }
                tokens.add(make(TokenType.OPERADOR_ARITMETICO, "-")); advance(); continue;
            }
            if (c == '*') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "*")); advance(); continue; }
            if (c == '/') { tokens.add(make(TokenType.OPERADOR_ARITMETICO, "/")); advance(); continue; }
            if (c == '<') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '-') { tokens.add(make(TokenType.SETA_ATRIBUICAO, "<-")); advance(); advance(); continue; }
                if (nxt == '=') { tokens.add(make(TokenType.OPERADOR_RELACIONAL, "<=")); advance(); advance(); continue; }
                if (nxt == '>') { tokens.add(make(TokenType.OPERADOR_RELACIONAL, "<>")); advance(); advance(); continue; }
                tokens.add(make(TokenType.OPERADOR_RELACIONAL, "<")); advance(); continue;
            }
            if (c == '>') {
                char nxt = pos + 1 < input.length() ? input.charAt(pos + 1) : 0;
                if (nxt == '=') { tokens.add(make(TokenType.OPERADOR_RELACIONAL, ">=")); advance(); advance(); continue; }
                tokens.add(make(TokenType.OPERADOR_RELACIONAL, ">")); advance(); continue;
            }
            if (Character.isLetter(c) || c == '_') { tokens.add(readIdentifier()); continue; }
            throw new ErroLexico(line, col, "Caractere não reconhecido pela gramática THZ: '" + c + "'.");
        }
        tokens.add(new Token(TokenType.EOF, "", line, col));
        return tokens;
    }

    private void advance() { pos++; col++; }

    private Token make(TokenType t, String v) { return new Token(t, v, line, col); }

    private Token readString() {
        int startLine = line, startCol = col;
        advance();
        StringBuilder sb = new StringBuilder();
        while (pos < input.length() && input.charAt(pos) != '"' && input.charAt(pos) != '\n') {
            if (input.charAt(pos) == '\\' && pos + 1 < input.length() && input.charAt(pos + 1) == 'n') {
                sb.append('\n'); advance(); advance();
            } else {
                sb.append(input.charAt(pos)); advance();
            }
        }
        if (pos >= input.length() || input.charAt(pos) != '"') {
            throw new ErroLexico(startLine, startCol, "Literal de texto não terminado (esperada aspa dupla de fechamento).");
        }
        advance();
        return new Token(TokenType.STRING_LITERAL, sb.toString(), startLine, startCol);
    }

    private Token readNumber() {
        int startCol = col;
        StringBuilder num = new StringBuilder();
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (ch == '.' && (pos + 1 >= input.length() || !Character.isDigit(input.charAt(pos + 1)))) break;
            if (ch == '_' || ch == '.' || Character.isDigit(ch)) {
                if (ch != '_') num.append(ch);
                advance();
            } else break;
            // also allow letters? no
            // handle the . already
            // stop checking for other chars
            // need to replicate TS: while /[\\d._]/
            // we already did digit, ., _
        }
        String s = num.toString();
        if (s.endsWith(".")) s = s.substring(0, s.length() - 1);
        return new Token(TokenType.NUMERO_LITERAL, s, line, startCol);
    }

    private Token readIdentifier() {
        int startCol = col;
        StringBuilder sb = new StringBuilder();
        while (pos < input.length()) {
            char ch = input.charAt(pos);
            if (Character.isLetterOrDigit(ch) || ch == '_') { sb.append(ch); advance(); }
            else break;
        }
        String ident = sb.toString();
        TokenType t = PalavrasReservadas.tokenDe(ident);
        if (t == null) t = TokenType.IDENTIFICADOR;
        return new Token(t, ident, line, startCol);
    }
}
