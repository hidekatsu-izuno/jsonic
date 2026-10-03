package net.arnx.jsonic.benchmark;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Numeric binding through character and UTF-8 streams. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class StreamNumberBenchmark {
    @Param({"small", "decimal", "scientific"})
    public String distribution;
    private JSON json;
    private JsonMapper jackson;
    private String input;
    private byte[] bytes;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) text.append(',');
            if ("small".equals(distribution)) text.append(i);
            else if ("scientific".equals(distribution)) text.append(i).append("e2");
            else text.append(123456789000L + i).append(".12345");
        }
        input = text.append(']').toString();
        bytes = input.getBytes(StandardCharsets.UTF_8);
        double[] expected = jackson.readValue(input, double[].class);
        if (!Arrays.equals(expected, json.parse(new StringReader(input), double[].class))
                || !Arrays.equals(expected, json.parse(new ByteArrayInputStream(bytes), double[].class))) {
            throw new IllegalStateException("Decoded numeric values differ");
        }
    }

    @Benchmark public double[] jsonicReader() throws Exception {
        return json.parse(new StringReader(input), double[].class);
    }
    @Benchmark public double[] jsonicStream() throws Exception {
        return json.parse(new ByteArrayInputStream(bytes), double[].class);
    }
    @Benchmark public double[] jacksonReader() throws Exception {
        return jackson.readValue(new StringReader(input), double[].class);
    }
    @Benchmark public double[] jacksonStream() throws Exception {
        return jackson.readValue(new ByteArrayInputStream(bytes), double[].class);
    }
}
