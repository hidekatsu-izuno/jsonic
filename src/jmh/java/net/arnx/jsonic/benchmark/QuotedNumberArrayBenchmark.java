package net.arnx.jsonic.benchmark;

import java.io.StringReader;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import tools.jackson.databind.json.JsonMapper;
import net.arnx.jsonic.JSON;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class QuotedNumberArrayBenchmark {
    @Param({"float", "double"}) public String type;
    @Param({"numeric", "quoted", "mixed", "escaped", "formatted"}) public String distribution;
    @Param({"string", "reader"}) public String source;
    @Param({"100", "1000"}) public int count;
    private JSON json;
    private JsonMapper jackson;
    private Class<?> target;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        target = "float".equals(type) ? float[].class : double[].class;
        if ("formatted".equals(distribution)) json.setNumberFormat("0.00");
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            boolean quoted = !"numeric".equals(distribution) && (!"mixed".equals(distribution) || (i & 1) != 0);
            if (quoted) text.append('"');
            if ("escaped".equals(distribution)) text.append("\\u0031"); else text.append('1');
            text.append(2345 + i).append(".625");
            if (quoted) text.append('"');
        }
        input = text.append(']').toString();
        if (!JSON.encode(jsonicDecode()).equals(JSON.encode(jacksonDecode()))) {
            throw new IllegalStateException("Floating array values differ");
        }
    }

    @Benchmark public Object jsonicDecode() throws Exception {
        return "reader".equals(source) ? json.parse(new StringReader(input), target) : json.parse(input, target);
    }
    @Benchmark public Object jacksonDecode() throws Exception {
        return "reader".equals(source) ? jackson.readValue(new StringReader(input), target) : jackson.readValue(input, target);
    }
}
