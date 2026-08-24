package thz.lang.bench;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(1)
@State(Scope.Thread)
public class LayoutBench {

    private static final int N = 10_000;

    // SoA (Structure of Arrays)
    private final int[] soaQtd = new int[N];
    private final double[] soaVal = new double[N];

    // AoS (Array of Structures) — Java record array
    private final double[][] aos = new double[N][2];

    @Setup
    public void setup() {
        for (int i = 0; i < N; i++) {
            soaQtd[i] = 10;
            soaVal[i] = 150.5;
            aos[i][0] = 10;
            aos[i][1] = 150.5;
        }
    }

    @Benchmark
    public void soaScan(Blackhole bh) {
        double acc = 0;
        for (int i = 0; i < N; i++) {
            acc += soaQtd[i] * soaVal[i];
        }
        bh.consume(acc);
    }

    @Benchmark
    public void aosScan(Blackhole bh) {
        double acc = 0;
        for (int i = 0; i < N; i++) {
            acc += aos[i][0] * aos[i][1];
        }
        bh.consume(acc);
    }
}
