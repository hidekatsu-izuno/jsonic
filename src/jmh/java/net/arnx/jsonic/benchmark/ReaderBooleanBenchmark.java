package net.arnx.jsonic.benchmark;

import java.io.StringReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import net.arnx.jsonic.JSON;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class ReaderBooleanBenchmark {
    @Param({"100", "1000"}) public int count;
    @Param({"literal", "quoted", "numeric", "numericLast", "quotedLast"}) public String distribution;
    private JSON json;
    private JsonMapper jackson;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES).build();
        StringBuilder text = new StringBuilder("[");
        for (int i = 0; i < count; i++) {
            if (i > 0) text.append(',');
            if (i % 13 == 0) text.append("null");
            else if ("numeric".equals(distribution) || ("numericLast".equals(distribution) && i == count - 1)) {
                text.append(i % 2 == 0 ? 0 : 1);
            } else {
                boolean quoted = "quoted".equals(distribution) || ("quotedLast".equals(distribution) && i == count - 1);
                if (quoted) text.append('"');
                text.append(i % 2 != 0);
                if (quoted) text.append('"');
            }
        }
        input = text.append(']').toString();
        if (!Arrays.equals(jsonicDecode(), jacksonDecode())) throw new IllegalStateException("Boolean values differ");
    }

    @Benchmark public boolean[] jsonicDecode() throws Exception {
        return json.parse(new StringReader(input), boolean[].class);
    }
    @Benchmark public boolean[] jacksonDecode() throws Exception {
        return jackson.readValue(new StringReader(input), boolean[].class);
    }
}
