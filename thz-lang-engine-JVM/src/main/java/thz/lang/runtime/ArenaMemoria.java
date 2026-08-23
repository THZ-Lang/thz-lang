package thz.lang.runtime;

import java.nio.ByteBuffer;

/**
 * ARENA DE MEMÓRIA — alocação contígua, descarte O(1).
 * Port exato de runtime.ts ArenaMemoria.
 */
public final class ArenaMemoria {

    private final ByteBuffer buffer;
    private int offset = 0;
    private final int capacidadeBytes;

    public ArenaMemoria(int tamanhoMb) {
        if (tamanhoMb < 0) throw new IllegalArgumentException("tamanhoMb deve ser não negativo");
        this.capacidadeBytes = tamanhoMb * 1024 * 1024;
        this.buffer = ByteBuffer.allocate(capacidadeBytes);
    }

    /**
     * Aloca bytes contíguos na arena, retornando o endereço (offset) inicial.
     * Lança RuntimeException com mensagem "[Runtime THZ] Estouro de capacidade da Arena de Memória." se exceder.
     */
    public int alocar(int bytes) {
        if (bytes < 0) throw new IllegalArgumentException("bytes deve ser não negativo");
        int endereco = this.offset;
        this.offset += bytes;
        if (this.offset > this.buffer.capacity()) {
            throw new RuntimeException("[Runtime THZ] Estouro de capacidade da Arena de Memória.");
        }
        return endereco;
    }

    public void liberarTudo() {
        this.offset = 0;
    }

    public int getCapacidadeBytes() {
        return buffer.capacity();
    }

    public int getUtilizacaoBytes() {
        return offset;
    }

    // Alias em português alternativo para compatibilidade
    public int capacidadeBytes() { return getCapacidadeBytes(); }
    public int utilizacaoBytes() { return getUtilizacaoBytes(); }
}
