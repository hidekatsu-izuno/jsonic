package net.arnx.jsonic.benchmark;

import java.io.StringWriter;
import java.util.concurrent.TimeUnit;
import org.openjdk.jmh.annotations.*;
import net.arnx.jsonic.JSON;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(value = 2, jvmArgsAppend = {"-Xms256m", "-Xmx256m"})
@State(Scope.Benchmark)
public class OutputCapacityBenchmark {
    @Param({"bean", "ascii700", "ascii1100", "japanese700", "escaped700"}) public String shape;
    @Param({"0", "1", "4", "100"}) public int count;
    private JSON json;
    private Object[] values;

    @Setup(Level.Trial) public void setup() throws Exception {
        json = new JSON();
        values = new Object[count];
        for (int i = 0; i < count; i++) {
            if ("bean".equals(shape)) {
                BeanBenchmark.Item item = new BeanBenchmark.Item();
                item.id = i;
                item.setName("item-" + i);
                item.active = true;
                item.price = 123.5;
                item.tags = new String[] {"new", "sale"};
                values[i] = item;
            } else if ("ascii700".equals(shape)) values[i] = "a".repeat(700);
            else if ("ascii1100".equals(shape)) values[i] = "a".repeat(1100);
            else if ("japanese700".equals(shape)) values[i] = "日".repeat(700);
            else values[i] = "\"\n\\".repeat(234);
        }
        StringWriter writer = new StringWriter();
        json.format(values, writer);
        if (!json.format(values).equals(writer.toString())) throw new IllegalStateException("Formatted values differ");
    }

    @Benchmark public String jsonicEncode() { return json.format(values); }
}
