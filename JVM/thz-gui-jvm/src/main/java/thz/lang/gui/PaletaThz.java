package thz.lang.gui;

import thz.lang.lexico.CategoriaPalavra;
import thz.lang.lexico.PalavrasReservadas;
import thz.lang.lexico.TokenType;

import javax.swing.text.AttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.util.Set;

/**
 * Paletas escuro/claro THZ — editor + chrome (toolbar/status/cards).
 * Monarch -> PalavrasReservadas como fonte da verdade.
 */
public final class PaletaThz {

    private static final Set<String> TIPOS_PRIMITIVOS = Set.of(
            "TEXTO","LOGICO","UUID","DATA","DATA_HORA",
            "NATURAL8","NATURAL16","NATURAL32","NATURAL64",
            "INTEIRO","INTEIRO8","INTEIRO16","INTEIRO32","INTEIRO64",
            "DECIMAL","MONETARIO","FATIA","RESULTADO","LISTA"
    );

    public final String nome;
    // editor
    public final Color fundoEditor;
    public final Color frenteEditor;
    public final Color fundoGutter;
    public final Color frenteGutter;
    public final Color frenteGutterAtiva;
    public final Color fundoLinhaAtual;
    public final Color corSelecao;
    public final Color corCaret;
    public final Color fundoErro;
    // chrome
    public final Color fundoJanela;
    public final Color fundoPainel;
    public final Color fundoToolbar;
    public final Color fundoStatus;
    public final Color corBorda;
    public final Color corBordaSuave;
    public final Color corAcento;
    public final Color corAcentoFg;
    public final Color corAcentoHover;
    public final Color corTextoSecundario;
    public final Color corTextoTitulo;

    public final AttributeSet attrPadrao;
    public final AttributeSet attrComentario;
    public final AttributeSet attrString;
    public final AttributeSet attrNumero;
    public final AttributeSet attrIdentificador;
    public final AttributeSet attrTipo;
    public final AttributeSet attrLiteralBooleano;
    public final AttributeSet attrOperador;
    public final AttributeSet attrDelimitador;
    public final AttributeSet attrErro;

    private final AttributeSet attrDeclaracao;
    private final AttributeSet attrFimBloco;
    private final AttributeSet attrContrato;
    private final AttributeSet attrControle;
    private final AttributeSet attrMemoria;
    private final AttributeSet attrModificador;
    private final AttributeSet attrConectivo;

    public static final PaletaThz ESCURO = criarEscuro();
    public static final PaletaThz CLARO = criarClaro();

    private PaletaThz(String nome,
                      Color fundoEditor, Color frenteEditor, Color fundoGutter, Color frenteGutter, Color frenteGutterAtiva, Color fundoLinhaAtual, Color corSelecao, Color corCaret, Color fundoErro,
                      Color fundoJanela, Color fundoPainel, Color fundoToolbar, Color fundoStatus, Color corBorda, Color corBordaSuave, Color corAcento, Color corAcentoFg, Color corAcentoHover, Color corTextoSecundario, Color corTextoTitulo,
                      AttributeSet attrPadrao, AttributeSet attrComentario, AttributeSet attrString, AttributeSet attrNumero,
                      AttributeSet attrIdentificador, AttributeSet attrTipo, AttributeSet attrLiteralBooleano,
                      AttributeSet attrOperador, AttributeSet attrDelimitador, AttributeSet attrErro,
                      AttributeSet attrDeclaracao, AttributeSet attrFimBloco, AttributeSet attrContrato,
                      AttributeSet attrControle, AttributeSet attrMemoria, AttributeSet attrModificador, AttributeSet attrConectivo) {
        this.nome = nome;
        this.fundoEditor = fundoEditor; this.frenteEditor = frenteEditor;
        this.fundoGutter = fundoGutter; this.frenteGutter = frenteGutter; this.frenteGutterAtiva = frenteGutterAtiva; this.fundoLinhaAtual = fundoLinhaAtual;
        this.corSelecao = corSelecao; this.corCaret = corCaret; this.fundoErro = fundoErro;
        this.fundoJanela = fundoJanela; this.fundoPainel = fundoPainel; this.fundoToolbar = fundoToolbar; this.fundoStatus = fundoStatus;
        this.corBorda = corBorda; this.corBordaSuave = corBordaSuave; this.corAcento = corAcento; this.corAcentoFg = corAcentoFg; this.corAcentoHover = corAcentoHover;
        this.corTextoSecundario = corTextoSecundario; this.corTextoTitulo = corTextoTitulo;
        this.attrPadrao = attrPadrao; this.attrComentario = attrComentario; this.attrString = attrString; this.attrNumero = attrNumero;
        this.attrIdentificador = attrIdentificador; this.attrTipo = attrTipo; this.attrLiteralBooleano = attrLiteralBooleano;
        this.attrOperador = attrOperador; this.attrDelimitador = attrDelimitador; this.attrErro = attrErro;
        this.attrDeclaracao = attrDeclaracao; this.attrFimBloco = attrFimBloco; this.attrContrato = attrContrato;
        this.attrControle = attrControle; this.attrMemoria = attrMemoria; this.attrModificador = attrModificador; this.attrConectivo = attrConectivo;
    }

    private static AttributeSet estilo(Color fg, boolean bold, boolean italic) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setForeground(s, fg);
        StyleConstants.setBold(s, bold);
        StyleConstants.setItalic(s, italic);
        StyleConstants.setFontFamily(s, "Monospaced");
        StyleConstants.setFontSize(s, 14);
        return s;
    }
    private static AttributeSet estiloErro(Color fg, Color bg) {
        SimpleAttributeSet s = new SimpleAttributeSet();
        StyleConstants.setForeground(s, fg);
        StyleConstants.setBackground(s, bg);
        StyleConstants.setUnderline(s, true);
        StyleConstants.setBold(s, true);
        return s;
    }

    private static PaletaThz criarEscuro() {
        Color bg = new Color(0x1E,0x1E,0x1E);
        Color fg = new Color(0xD4,0xD4,0xD4);
        return new PaletaThz(
                "Escuro",
                bg, fg,
                new Color(0x25,0x25,0x26), new Color(0x6E,0x6E,0x6E), new Color(0xD4,0xD4,0xD4), new Color(0x2A,0x2D,0x2E),
                new Color(0x3A,0x3D,0x41), fg,
                new Color(0x5A,0x1D,0x1D),
                new Color(0x12,0x12,0x12), new Color(0x1E,0x1E,0x1E), new Color(0x25,0x25,0x26), new Color(0x1A,0x1A,0x1A),
                new Color(0x3A,0x3A,0x3A), new Color(0x2E,0x2E,0x2E),
                new Color(0x0E,0x63,0x9C), Color.WHITE, new Color(0x11,0x77,0xC0),
                new Color(0x9A,0x9A,0x9A), new Color(0xE8,0xE8,0xE8),
                estilo(fg,false,false),
                estilo(new Color(0x6A,0x99,0x55),false,true),
                estilo(new Color(0xCE,0x91,0x78),false,false),
                estilo(new Color(0xB5,0xCE,0xA8),false,false),
                estilo(new Color(0x9C,0xDC,0xFE),false,false),
                estilo(new Color(0x4E,0xC9,0xB0),true,false),
                estilo(new Color(0x56,0x9C,0xD6),false,false),
                estilo(new Color(0xD4,0xD4,0xD4),false,false),
                estilo(new Color(0xDA,0xDA,0xAA),false,false),
                estiloErro(new Color(0xF1,0x47,0x47), new Color(0x5A,0x1D,0x1D)),
                estilo(new Color(0x56,0x9C,0xD6),true,false),
                estilo(new Color(0x56,0x9C,0xD6),true,false),
                estilo(new Color(0xC5,0x86,0xC0),true,false),
                estilo(new Color(0xD7,0xBA,0x7D),true,false),
                estilo(new Color(0x4E,0xC9,0xB0),true,false),
                estilo(new Color(0x9C,0xDC,0xFE),false,true),
                estilo(new Color(0xC5,0x86,0xC0),true,false)
        );
    }
    private static PaletaThz criarClaro() {
        Color bg = Color.WHITE;
        Color fg = new Color(0x1F,0x1F,0x1F);
        return new PaletaThz(
                "Claro",
                bg, fg,
                new Color(0xF3,0xF3,0xF3), new Color(0x8A,0x8A,0x8A), new Color(0x00,0x78,0xD4), new Color(0xF0,0xF6,0xFF),
                new Color(0xAD,0xD8,0xE6), fg,
                new Color(0xFF,0xCC,0xCC),
                new Color(0xF5,0xF5,0xF5), Color.WHITE, new Color(0xF3,0xF3,0xF3), new Color(0xEB,0xEB,0xEB),
                new Color(0xE0,0xE0,0xE0), new Color(0xEE,0xEE,0xEE),
                new Color(0x00,0x78,0xD4), Color.WHITE, new Color(0x10,0x6E,0xBE),
                new Color(0x6E,0x6E,0x6E), new Color(0x1A,0x1A,0x1A),
                estilo(fg,false,false),
                estilo(new Color(0x00,0x80,0x00),false,true),
                estilo(new Color(0xA3,0x15,0x15),false,false),
                estilo(new Color(0x09,0x86,0x58),false,false),
                estilo(new Color(0x00,0x10,0x80),false,false),
                estilo(new Color(0x26,0x7F,0x99),true,false),
                estilo(new Color(0x00,0x00,0xFF),true,false),
                estilo(new Color(0x1A,0x1A,0x1A),false,false),
                estilo(new Color(0x3B,0x3B,0x3B),false,false),
                estiloErro(new Color(0xCD,0x00,0x00), new Color(0xFF,0xE0,0xE0)),
                estilo(new Color(0x00,0x00,0xFF),true,false),
                estilo(new Color(0x00,0x00,0x8B),true,false),
                estilo(new Color(0xAF,0x00,0xDB),true,false),
                estilo(new Color(0x00,0x00,0xFF),true,false),
                estilo(new Color(0x00,0x7A,0x7A),true,false),
                estilo(new Color(0x00,0x10,0x80),false,true),
                estilo(new Color(0xAF,0x00,0xDB),true,false)
        );
    }

    public AttributeSet atributoPara(thz.lang.lexico.Token token) {
        if (token.type() == TokenType.IDENTIFICADOR) {
            if (TIPOS_PRIMITIVOS.contains(token.value())) return attrTipo;
            return attrIdentificador;
        }
        switch (token.type()) {
            case STRING_LITERAL: return attrString;
            case NUMERO_LITERAL: return attrNumero;
            case VERDADEIRO, FALSO, NULO: return attrLiteralBooleano;
            case OPERADOR_LOGICO: return attrConectivo;
            case SETA_ATRIBUICAO, OPERADOR_RELACIONAL, OPERADOR_ARITMETICO: return attrOperador;
            case DOIS_PONTOS, PONTO, VIRGULA, ABRE_PARENTESE, FECHA_PARENTESE, ABRE_COLCHETE, FECHA_COLCHETE: return attrDelimitador;
            default:
                CategoriaPalavra cat = PalavrasReservadas.categoriaDe(token.value());
                if (cat == null) return attrPadrao;
                return switch (cat) {
                    case DECLARACAO -> attrDeclaracao;
                    case FIM_BLOCO -> attrFimBloco;
                    case CONTRATO -> attrContrato;
                    case CONTROLE -> attrControle;
                    case MEMORIA -> attrMemoria;
                    case MODIFICADOR -> attrModificador;
                    case LITERAL -> attrLiteralBooleano;
                    case CONECTIVO_LOGICO -> attrConectivo;
                };
        }
    }
    public static boolean ehTipoPrimitivo(String nome) { return TIPOS_PRIMITIVOS.contains(nome); }
}
