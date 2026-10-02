package net.arnx.jsonic.benchmark;

import java.util.Arrays;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Numeric distributions beyond the small identifiers in BeanBenchmark. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class NumericArrayBenchmark {
    @Param({"small", "wide", "decimal", "rounding"})
    public String distribution;
    private JSON json;
    private JsonMapper jackson;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) text.append(',');
            if ("small".equals(distribution)) text.append(i);
            else if ("wide".equals(distribution)) text.append(123456789000L + i);
            else if ("rounding".equals(distribution)) {
                text.append(BigDecimal.valueOf(9007199254740993L + 2L * i, 5).toPlainString());
            } else text.append(123456789000L + i).append(".12345");
        }
        input = text.append(']').toString();
        if (!Arrays.equals(json.parse(input, double[].class), jackson.readValue(input, double[].class))) {
            throw new IllegalStateException("Decoded numeric values differ");
        }
    }

    @Benchmark public double[] jsonicDecode() { return json.parse(input, double[].class); }
    @Benchmark public double[] jacksonDecode() throws Exception { return jackson.readValue(input, double[].class); }
}
