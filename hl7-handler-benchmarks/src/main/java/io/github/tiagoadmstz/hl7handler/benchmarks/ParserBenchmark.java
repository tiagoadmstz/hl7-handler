package io.github.tiagoadmstz.hl7handler.benchmarks;

import io.github.tiagoadmstz.hl7handler.Hl7Handler;
import io.github.tiagoadmstz.hl7handler.api.Hl7Processor;
import io.github.tiagoadmstz.hl7handler.model.Hl7Message;
import io.github.tiagoadmstz.hl7handler.parser.DefaultHl7Parser;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(1)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
public class ParserBenchmark {

    private DefaultHl7Parser parser;
    private Hl7Processor processor;
    private String adtV25;
    private String mindrayOruV26;
    private String oruWith200Obx;

    @Setup(Level.Trial)
    public void setUp() {
        parser = new DefaultHl7Parser();
        adtV25 = readResource("/samples/sample-v25.hl7");
        mindrayOruV26 = readResource("/samples/versions/v26-mindray-oru.hl7");
        oruWith200Obx = buildOruWith200Obx();
        processor = Hl7Handler.builder()
                .withMappingContent(readResource("/mappings/paciente-mapping.yaml"))
                .build();
    }

    @Benchmark
    public Hl7Message parseAdtV25() {
        return parser.parse(adtV25);
    }

    @Benchmark
    public Hl7Message parseMindrayOruV26() {
        return parser.parse(mindrayOruV26);
    }

    @Benchmark
    public Hl7Message parseOruWith200Obx() {
        return parser.parse(oruWith200Obx);
    }

    @Benchmark
    public Map<String, Object> mapPaciente() {
        return processor.read(adtV25).toMap();
    }

    private static String readResource(String path) {
        try (InputStream in = ParserBenchmark.class.getResourceAsStream(path)) {
            if (in == null) {
                throw new IllegalStateException("Resource not found: " + path);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading resource " + path, e);
        }
    }

    private static String buildOruWith200Obx() {
        StringBuilder hl7 = new StringBuilder();
        hl7.append("MSH|^~\\&|LAB_SYSTEM|HOSPITAL_A|HL7_HANDLER|HOSPITAL_B|20261117120000||ORU^R01|MSG202611170001|P|2.5\r");
        hl7.append("PID|1||123456^^^HOSPITAL^MR||SILVA^MARIA^^^DRA||19900101|F\r");
        hl7.append("OBR|1|ORD0001|FIL0001|HEM^HEMOGRAMA^LN|||20261117110000\r");
        for (int i = 1; i <= 200; i++) {
            hl7.append("OBX|").append(i).append("|NM|EX").append(i).append("^EXAME ").append(i).append("^LN~ALT")
                    .append(i).append("^ALT^L||").append(90 + i % 20).append("^mg&MG^UCUM~")
                    .append(100 + i % 7).append("||70-99|N|||F\r");
        }
        return hl7.toString();
    }
}
