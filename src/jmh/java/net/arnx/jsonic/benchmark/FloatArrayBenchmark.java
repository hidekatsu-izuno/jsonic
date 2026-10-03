package net.arnx.jsonic.benchmark;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Float conversion with small exact operands and values requiring full rounding. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class FloatArrayBenchmark {
    @Param({"small", "decimal", "rounding", "scientific", "quoted"}) public String distribution;
    @Param({"string", "reader"}) public String source;
    @Param({"100", "1000"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private String input;
    private boolean reader;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        reader = "reader".equals(source);
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if ("decimal".equals(distribution)) text.append(12345 + i).append(".625");
            else if ("rounding".equals(distribution)) {
                text.append(BigDecimal.valueOf(9007199254740993L + 2L * i, 5).toPlainString());
            } else if ("scientific".equals(distribution)) text.append(i).append("e2");
            else {
                if ("quoted".equals(distribution) && (i & 1) != 0) text.append('"');
                text.append(i);
                if ("quoted".equals(distribution) && (i & 1) != 0) text.append('"');
            }
        }
        input = text.append(']').toString();
        float[] expected = jackson.readValue(input, float[].class);
        if (!Arrays.equals(expected, json.parse(input, float[].class))
                || !Arrays.equals(expected, json.parse(new StringReader(input), float[].class))) {
            throw new IllegalStateException("Decoded float values differ");
        }
    }

    @Benchmark public float[] jsonicDecode() throws Exception {
        return reader ? json.parse(new StringReader(input), float[].class) : json.parse(input, float[].class);
    }
    @Benchmark public float[] jacksonDecode() throws Exception {
        return reader ? jackson.readValue(new StringReader(input), float[].class) : jackson.readValue(input, float[].class);
    }
}
