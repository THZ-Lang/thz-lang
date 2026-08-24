package thz.lang.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.math.BigInteger;
import java.util.concurrent.TimeUnit;

import thz.lang.runtime.DecimalFixo;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class DecimalBench {

    private DecimalFixo a;
    private DecimalFixo b;

    @Setup
    public void setup() {
        a = DecimalFixo.deTexto("150.5000", 4);
        b = DecimalFixo.deTexto("18.0000", 4);
    }

    @Benchmark
    public void somar(Blackhole bh) {
        bh.consume(a.somar(b));
    }

    @Benchmark
    public void multiplicar(Blackhole bh) {
        bh.consume(a.multiplicar(b));
    }

    @Benchmark
    public void dividir(Blackhole bh) {
        bh.consume(a.dividir(b));
    }

    @Benchmark
    public void numberAdd(Blackhole bh) {
        double x = 150.5;
        double y = 18.0;
        bh.consume(x + y);
    }

    @Benchmark
    public void numberMul(Blackhole bh) {
        double x = 150.5;
        double y = 18.0;
        bh.consume(x * y);
    }
}
