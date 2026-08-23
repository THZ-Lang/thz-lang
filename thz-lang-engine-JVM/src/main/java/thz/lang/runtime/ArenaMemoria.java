package thz.lang.runtime;

import java.nio.ByteBuffer;

/**
 * <h3>Gerenciamento de Memória por Bloco Efêmero (Arena de Memória)</h3>
 *
 * <p>A <strong>ArenaMemoria</strong> é um alocador linear de memória de altíssima performance.
 * Em vez de criar e desalocar dezenas de pequenos objetos individualmente no heap (o que
 * gera sobrecarga e pausas no Garbage Collector), uma <em>Arena</em> reserva um bloco de memória
 * contíguo de tamanho fixo e atende todas as alocações avançando um simples ponteiro sequencial.</p>
 *
 * <h4>Principais Vantagens:</h4>
 * <ul>
 *   <li><strong>Alocação Ultrarrápida:</strong> A alocação consiste apenas em incrementar um contador de deslocamento (<i>offset</i>).</li>
 *   <li><strong>Descarte Instantâneo em tempo O(1):</strong> Ao término de um lote ou escopo (ex: {@code USAR_BLOCO_MEMORIA}),
 *       toda a memória utilizada é liberada de uma única vez resetando o apontador a zero, sem nenhum custo de varredura.</li>
 *   <li><strong>Localidade Espacial de Cache:</strong> Como os dados ficam contíguos no mesmo bloco, o uso de cache L1/L2/L3 é maximizado.</li>
 * </ul>
 */
public final class ArenaMemoria {

    private final ByteBuffer buffer;
    private int offset = 0;
    private final int tamanhoMb;
    private final int capacidadeBytes;

    /**
     * Cria uma nova arena reservando um bloco contíguo com o tamanho especificado em Megabytes (MB).
     *
     * @param tamanhoMb Capacidade total do bloco em Megabytes (ex: 1 para 1 MB, 64 para 64 MB).
     * @throws IllegalArgumentException se o tamanho for negativo.
     */
    public ArenaMemoria(int tamanhoMb) {
        if (tamanhoMb < 0) {
            throw new IllegalArgumentException("O tamanho em Megabytes (MB) da arena deve ser maior ou igual a zero.");
        }
        this.tamanhoMb = tamanhoMb;
        this.capacidadeBytes = tamanhoMb * 1024 * 1024;
        this.buffer = ByteBuffer.allocate(capacidadeBytes);
    }

    /**
     * Aloca um espaço contíguo de bytes dentro da arena e retorna a posição (offset) inicial do bloco.
     *
     * @param bytes Quantidade de bytes contíguos a serem alocados.
     * @return O endereço/offset inicial onde o bloco foi reservado.
     * @throws RuntimeException se a quantidade solicitada exceder a capacidade total disponível na arena.
     */
    public int alocar(int bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("A quantidade de bytes a alocar deve ser não negativa.");
        }
        int enderecoInicial = this.offset;
        int novoOffset = this.offset + bytes;
        if (novoOffset > this.capacidadeBytes) {
            throw new RuntimeException(String.format(
                    "[Runtime THZ] Limite de capacidade da Arena de Memória excedido: solicitado %d bytes, utilizado %d/%d bytes.",
                    bytes, this.offset, this.capacidadeBytes
            ));
        }
        this.offset = novoOffset;
        return enderecoInicial;
    }

    /**
     * Libera instantaneamente toda a memória alocada na arena em tempo constante O(1).
     * Todos os dados alocados neste bloco são descartados de uma única vez.
     */
    public void liberarTudo() {
        this.offset = 0;
    }

    /**
     * Retorna a capacidade total do bloco de memória em bytes.
     */
    public int getCapacidadeBytes() {
        return capacidadeBytes;
    }

    /**
     * Retorna a quantidade de bytes atualmente ocupados na arena.
     */
    public int getUtilizacaoBytes() {
        return offset;
    }

    /**
     * Retorna o espaço restante ainda livre para novas alocações em bytes.
     */
    public int getEspacoLivreBytes() {
        return capacidadeBytes - offset;
    }

    /**
     * Retorna a porcentagem de memória utilizada no bloco (de 0.0% a 100.0%).
     */
    public double getPorcentagemUso() {
        if (capacidadeBytes == 0) return 0.0;
        return ((double) offset / capacidadeBytes) * 100.0;
    }

    /**
     * Indica se a arena está completamente vazia (sem bytes alocados).
     */
    public boolean estaVazia() {
        return offset == 0;
    }

    /**
     * Retorna o tamanho configurado em Megabytes.
     */
    public int getTamanhoMb() {
        return tamanhoMb;
    }

    // --- Métodos de compatibilidade (idiomáticos) ---
    public int capacidadeBytes() { return getCapacidadeBytes(); }
    public int utilizacaoBytes() { return getUtilizacaoBytes(); }
    public int espacoLivreBytes() { return getEspacoLivreBytes(); }
    public double porcentagemUso() { return getPorcentagemUso(); }

    @Override
    public String toString() {
        return String.format("ArenaMemoria[Utilizado: %d B / %d B (%.1f%%) — Bloco de %d MB]",
                offset, capacidadeBytes, getPorcentagemUso(), tamanhoMb);
    }
}
