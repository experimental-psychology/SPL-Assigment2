package spl.lae;

import java.io.IOException;

import parser.*;

public class Main {
    public static void main(String[] args) throws IOException {
        String outputPath = "output.json";
        LinearAlgebraEngine lae = null;

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

            lae = new LinearAlgebraEngine(numThreads);
            ComputationNode resultNode = lae.run(root);

            OutputWriter.write(resultNode.getMatrix(), outputPath);

            // >>> Gilad tester instrumentation (success path)
            System.out.println(lae.getWorkerReport());

        } catch (Throwable t) {
            try {
                OutputWriter.write(t.getMessage(), outputPath);
            } catch (Throwable ignored) {}

            // >>> Gilad tester instrumentation (error path too)
            if (lae != null) {
                System.out.println(lae.getWorkerReport());
            }

            System.err.println(t.getMessage());
            t.printStackTrace();
        }
    }
}
