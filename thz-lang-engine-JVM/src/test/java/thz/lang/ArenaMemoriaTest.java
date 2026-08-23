package thz.lang;

import org.junit.jupiter.api.Test;
import thz.lang.runtime.ArenaMemoria;

import static org.junit.jupiter.api.Assertions.*;

public class ArenaMemoriaTest {

    @Test
    public void testCriacaoEAlocacaoBasica() {
        ArenaMemoria arena = new ArenaMemoria(1); // 1 MB
        assertEquals(1, arena.getTamanhoMb());
        assertEquals(1024 * 1024, arena.getCapacidadeBytes());
        assertEquals(0, arena.getUtilizacaoBytes());
        assertTrue(arena.estaVazia());
        assertEquals(1024 * 1024, arena.getEspacoLivreBytes());
        assertEquals(0.0, arena.getPorcentagemUso());

        int offset1 = arena.alocar(512);
        assertEquals(0, offset1);
        assertEquals(512, arena.getUtilizacaoBytes());
        assertFalse(arena.estaVazia());
        assertEquals((1024 * 1024) - 512, arena.getEspacoLivreBytes());

        int offset2 = arena.alocar(1024);
        assertEquals(512, offset2);
        assertEquals(1536, arena.getUtilizacaoBytes());

        // Descarte instantâneo em O(1)
        arena.liberarTudo();
        assertEquals(0, arena.getUtilizacaoBytes());
        assertTrue(arena.estaVazia());
        assertEquals(1024 * 1024, arena.getEspacoLivreBytes());
    }

    @Test
    public void testLimiteDeCapacidadeExcedido() {
        ArenaMemoria arena = new ArenaMemoria(1); // 1 MB = 1048576 B
        arena.alocar(1048570);

        Exception ex = assertThrows(RuntimeException.class, () -> arena.alocar(10));
        assertTrue(ex.getMessage().contains("Limite de capacidade da Arena de Memória excedido"));
    }

    @Test
    public void testParametrosInvalidos() {
        assertThrows(IllegalArgumentException.class, () -> new ArenaMemoria(-1));
        ArenaMemoria arena = new ArenaMemoria(1);
        assertThrows(IllegalArgumentException.class, () -> arena.alocar(-50));
    }

    @Test
    public void testToStringLegivel() {
        ArenaMemoria arena = new ArenaMemoria(2);
        arena.alocar(1024);
        String s = arena.toString();
        assertNotNull(s);
        assertTrue(s.contains("ArenaMemoria"));
        assertTrue(s.contains("Utilizado: 1024 B"));
        assertTrue(s.contains("Bloco de 2 MB"));
    }
}
