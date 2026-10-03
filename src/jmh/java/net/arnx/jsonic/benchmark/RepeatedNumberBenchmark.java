package net.arnx.jsonic.benchmark;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** Repeated and changing tokens outside the shared small-integer range. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class RepeatedNumberBenchmark {
    @Param({"double", "float", "int", "long"}) public String type;
    @Param({"repeat", "blocks", "unique", "scales"}) public String distribution;
    @Param({"reader", "stream"}) public String source;
    @Param({"100", "1000"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private Class<?> target;
    private String input;
    private byte[] bytes;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
        target = "double".equals(type) ? double[].class : "float".equals(type) ? float[].class
                : "int".equals(type) ? int[].class : long[].class;
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if (i % 13 == 0) { text.append("null"); continue; }
            int offset = "unique".equals(distribution) ? i : "blocks".equals(distribution) ? i / 32 : 0;
            boolean floating = target == float[].class || target == double[].class;
            text.append((floating ? 12345 : 100000) + offset);
            if (floating) text.append(".625");
            if ("scales".equals(distribution)) {
                if (!floating) text.append(".0");
                if ((i & 1) != 0) text.append('0');
            }
        }
        input = text.append(']').toString();
        bytes = input.getBytes(StandardCharsets.UTF_8);
        if (!JSON.encode(jsonicDecode()).equals(JSON.encode(jacksonDecode()))) {
            throw new IllegalStateException("Decoded typed values differ");
        }
    }

    @Benchmark public Object jsonicDecode() throws Exception {
        return "reader".equals(source) ? json.parse(new StringReader(input), target)
                : json.parse(new ByteArrayInputStream(bytes), target);
    }
    @Benchmark public Object jacksonDecode() throws Exception {
        return "reader".equals(source) ? jackson.readValue(new StringReader(input), target)
                : jackson.readValue(new ByteArrayInputStream(bytes), target);
    }
}
