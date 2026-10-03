package net.arnx.jsonic.benchmark;

import java.io.StringReader;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import net.arnx.jsonic.JSON;
import net.arnx.jsonic.JSONEventType;
import net.arnx.jsonic.JSONReader;
import tools.jackson.databind.json.JsonMapper;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class ReaderStringBenchmark {
    @Param({"100"}) public int count;
    @Param({"plain", "escaped", "long", "refill"}) public String distribution;
    private JSON json;
    private JsonMapper jackson;
    private String input;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        jackson = JsonMapper.builder().build();
        String[] values = new String[count];
        for (int i = 0; i < count; i++) {
            if (i % 13 == 0) continue;
            if ("escaped".equals(distribution)) values[i] = "日本語\"名\n" + i;
            else if ("long".equals(distribution)) values[i] = "x".repeat(1000) + i;
            else if ("refill".equals(distribution)) values[i] = "x".repeat(3000) + i;
            else values[i] = "item-" + i;
        }
        input = jackson.writeValueAsString(values);
        if (!Arrays.equals(values, jsonicDecode()) || !Arrays.equals(values, jacksonDecode())) {
            throw new IllegalStateException("Decoded strings differ");
        }
    }

    @Benchmark public String[] jsonicDecode() throws Exception {
        return json.parse(new StringReader(input), String[].class);
    }
    @Benchmark public String[] jacksonDecode() throws Exception {
        return jackson.readValue(new StringReader(input), String[].class);
    }

    @Benchmark public int jsonicStringEvents() throws Exception {
        JSONReader reader = json.getReader(input);
        int length = 0;
        JSONEventType event;
        while ((event = reader.next()) != null) {
            if (event == JSONEventType.STRING) length += reader.getString().length();
        }
        return length;
    }
}
