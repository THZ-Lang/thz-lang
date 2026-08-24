package thz.lang.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

import thz.lang.runtime.BlocoMemoria;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class BlocoMemoriaBench {

    @Benchmark
    public void alocarLiberar(Blackhole bh) {
        BlocoMemoria bloco = new BlocoMemoria(1);
        bh.consume(bloco.alocar(64));
        bloco.liberarTudo();
    }

    @Benchmark
    public void multiplasAlocacoes(Blackhole bh) {
        BlocoMemoria bloco = new BlocoMemoria(1);
        for (int i = 0; i < 1000; i++) {
            bh.consume(bloco.alocar(64));
        }
        bloco.liberarTudo();
    }

    @Benchmark
    public void alocacaoGrande(Blackhole bh) {
        BlocoMemoria bloco = new BlocoMemoria(1);
        bh.consume(bloco.alocar(1024 * 1024));
        bloco.liberarTudo();
    }

    @Benchmark
    public void javaObjectAllocation(Blackhole bh) {
        bh.consume(new Object());
    }

    @Benchmark
    public void javaArrayListAllocation(Blackhole bh) {
        bh.consume(new java.util.ArrayList<>(16));
    }
}
