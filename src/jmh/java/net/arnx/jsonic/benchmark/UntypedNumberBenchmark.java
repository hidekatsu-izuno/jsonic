package net.arnx.jsonic.benchmark;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

/** JSONIC's public untyped numbers compared with Jackson's decimal list binding. */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class UntypedNumberBenchmark {
    @Param({"small", "decimal", "scientific"}) public String distribution;
    @Param({"string", "reader", "stream"}) public String source;
    private JSON json;
    private JsonMapper jackson;
    private String input;
    private byte[] bytes;
    private static final TypeReference<List<BigDecimal>> DECIMALS = new TypeReference<>() {};

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < 100; i++) {
            if (i > 0) text.append(',');
            if ("decimal".equals(distribution)) text.append(123456789000L + i).append(".12345");
            else if ("scientific".equals(distribution)) text.append(i).append("e2");
            else text.append(i);
        }
        input = text.append(']').toString();
        bytes = input.getBytes(StandardCharsets.UTF_8);
        if (!jsonicDecode().equals(jacksonDecode())) throw new IllegalStateException("Decimal values or scales differ");
    }

    @Benchmark public List<?> jsonicDecode() throws Exception {
        if ("reader".equals(source)) return json.parse(new StringReader(input));
        if ("stream".equals(source)) return json.parse(new ByteArrayInputStream(bytes));
        return json.parse(input);
    }
    @Benchmark public List<BigDecimal> jacksonDecode() throws Exception {
        if ("reader".equals(source)) return jackson.readValue(new StringReader(input), DECIMALS);
        if ("stream".equals(source)) return jackson.readValue(new ByteArrayInputStream(bytes), DECIMALS);
        return jackson.readValue(input, DECIMALS);
    }
}
