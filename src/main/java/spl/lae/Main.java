package spl.lae;

import java.io.IOException;
import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String outputPath = "output.json";
        try {
            if (args == null || args.length != 3) {
                throw new IllegalArgumentException(
                        "Usage: java -jar target/lga-1.0.jar <numberOfThreads> <inputFilePath> <outputFilePath>"
                );
            }

            int numThreads;
            try {
                numThreads = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("numberOfThreads must be an integer", e);
            }
            if (numThreads <= 0) {
                throw new IllegalArgumentException("numberOfThreads must be positive");
            }

            String inputPath = args[1];
            outputPath = args[2];

            InputParser parser = new InputParser();
            ComputationNode root = parser.parse(inputPath);

            LinearAlgebraEngine engine = new LinearAlgebraEngine(numThreads);
            ComputationNode resultNode = engine.run(root);

            OutputWriter.write(resultNode.getMatrix(), outputPath);
            System.out.println(engine.getWorkerReport());

            // --- Gilad Tester instrumentation:
            System.out.println(engine.getWorkerReport());

        } catch (Throwable t) {
            try {
                OutputWriter.write(t.getMessage(), outputPath);
            } catch (Throwable ignored) {
            }
            System.err.println(t.getMessage());
            t.printStackTrace();
        }
    }
}
