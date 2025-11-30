package dev.povod;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.prowidesoftware.swift.model.mt.AbstractMT;
import com.prowidesoftware.swift.model.mt.AbstractMTAdapter;
import com.prowidesoftware.swift.model.SwiftBlock2;
import com.prowidesoftware.swift.model.SwiftBlock2Adapter;

public class SwiftToJson {
    private static Gson gson = (new GsonBuilder()).registerTypeAdapter(AbstractMT.class, new AbstractMTAdapter())
            .registerTypeAdapter(SwiftBlock2.class, new SwiftBlock2Adapter()).create();

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: swift-to-json <glob or list of files>");
            System.exit(1);
        }

        ExecutorService pool = Executors.newFixedThreadPool(
                Runtime.getRuntime().availableProcessors());

        List<Path> files = resolveInputs(args);

        for (Path file : files) {
            pool.submit(() -> processFile(file));
        }

        pool.shutdown();
        pool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
    }

    private static List<Path> resolveInputs(String[] args) throws Exception {
        List<Path> result = new ArrayList<>();

        for (String arg : args) {
            // If literal file exists → use directly
            Path p = Paths.get(arg);
            if (Files.exists(p)) {
                result.add(p.toAbsolutePath());
                continue;
            }

            // Otherwise treat as glob
            result.addAll(resolveGlob(arg));
        }

        return result;
    }

    private static List<Path> resolveGlob(String pattern) throws Exception {

        String cmd = "import glob, json;" +
                "print(json.dumps(glob.glob('" + pattern.replace("\\", "\\\\") + "', recursive=True)))";
        ;
        ProcessBuilder pb = new ProcessBuilder(
                "python3",
                "-c",
                cmd);
        pb.redirectErrorStream(true);
        Process p = pb.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));

        String json = reader.readLine(); // read output
        p.waitFor();

        while (true) {
            String v = reader.readLine();
            System.out.println(v);
            if (v == null)
                break;
        }

        return parseJsonToPaths(json);
    }

    private static List<Path> parseJsonToPaths(String json) {
        List<Path> paths = new ArrayList<>();

        if (json == null || json.isEmpty())
            return paths;

        // Lightweight parsing to avoid requiring a JSON library:
        // Remove brackets [ ] and quotes "
        json = json.trim();
        if (json.startsWith("["))
            json = json.substring(1);
        if (json.endsWith("]"))
            json = json.substring(0, json.length() - 1);

        if (json.isBlank())
            return paths;

        String[] parts = json.split(",");

        for (String p : parts) {
            String cleaned = p.trim().replace("\"", "");
            if (!cleaned.isEmpty()) {
                Path path = Paths.get(cleaned);
                paths.add(path);
            }
        }

        return paths;
    }

    private static void processFile(Path file) {
        if (file.toString().endsWith(".json")) return;
        System.out.println("Processing: " + file);

        Path output = Path.of(file.toString() + ".json");

        try (BufferedReader reader = Files.newBufferedReader(file);
                BufferedWriter writer = Files.newBufferedWriter(output)) {

            writer.write("[\n");
            boolean first = true;
            String rawMsg;
            while ((rawMsg = getRawMessage(reader)) != "") {
                AbstractMT msg = AbstractMT.parse(rawMsg);
                if (!first)
                    writer.write(",\n");
                first = false;
                writer.write(gson.toJson(msg, AbstractMT.class));
            }

            writer.write("\n]");
        } catch (Exception e) {
            System.err.println(e);
            try {
                Files.delete(output);
            } catch (IOException exc) {}
            throw new RuntimeException(e);
        }
    }

    private static String getRawMessage(BufferedReader reader) throws Exception {
        String line;
        String message = "";
        for (; (line = reader.readLine()) != null;) {
            if (!line.startsWith("{1:")) {
                continue;
            }
            message = line + "\n";
            while (!(line = reader.readLine()).startsWith("-}")) {
                message += line + "\n";
            }
            message += line;
            break;
        }
        return message;
    }
}
