package data.generation.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import math.simplex.Constraint;
import math.simplex.Simplex;
import math.simplex.SimplexAnswer;
import math.simplex.exception.DifficultSimplexSolveException;
import math.simplex.exception.IncompatibleSimplexSolveException;
import math.simplex.exception.UnlimitedFunctionExtremumException;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static data.generation.test.SensitivityAnalysis.add_constraint;

public class Main {

    private static int ID = 0;

    private static List<SimplexTestData> builders(int threadId) {
        String threadName = "thread-" + threadId;
        List<SimplexTestData> testDataList = new ArrayList<>();
        RandomTaskGenerator taskGenerator = new RandomTaskGenerator(threadName);

        SimplexTestData testData;
        int size = 0;
        int xCount;
        while(size < 100) {
            ID++;
            System.out.println("ID - " + ID);
            testData = new SimplexTestData();
            Simplex.Builder simpleData = taskGenerator.generateSimplexBuilder();
            xCount = simpleData.getC().length;

            testData.setSimpleData(simpleData);
            Simplex simplex = simpleData.build();
            Class<? extends Exception> exceptionClass = null;
            String exceptionMessage = null;
            try {
                System.out.printf("(Thread - %s) solving...\n", threadName);
                SimplexAnswer simplexAnswer = simplex.solve();
                testData.setSimplexAnswer(simplexAnswer);
                System.out.printf("(Thread - %s) solved\n", threadName);
            } catch (IncompatibleSimplexSolveException | DifficultSimplexSolveException | UnlimitedFunctionExtremumException e) {
                System.err.printf("(Thread - %s) Exception: %s\n", threadName, e.getMessage());
                continue;
            }

            try {
                Constraint constraint = taskGenerator.generateConstraint(xCount);
                testData.setAddConstraint(constraint);
                testData.setAnalysisType(add_constraint);

                System.out.printf("(Thread - %s) recalculating...\n", threadName);
                SimplexAnswer analysisAnswer = simplex.addConstraint(constraint.getAi(), constraint.getInequality(), constraint.getBi());
                testData.setAnalysisAnswer(analysisAnswer);
                System.out.printf("(Thread - %s) recalculated\n", threadName);
                testDataList.add(testData);
                size++;
            } catch (IncompatibleSimplexSolveException | DifficultSimplexSolveException e) {
                exceptionMessage = e.getMessage();
                exceptionClass = e.getClass();
                System.err.printf("(Thread - %s) Exception: %s\n", threadName, exceptionMessage);
                testData.setExceptionClass(exceptionClass);
                testData.setExceptionMessage(exceptionMessage);
            }
        }

        return testDataList;
    }

    public static void main(String[] args) {
        List<SimplexTestData> testData = builders(1);
        Path success = Paths.get("src", "test", "resources", "math", "simplex" ,"data", "test_add_constraint_data.json");
        writeJSONToFile(success, testData);
    }

    private static void writeJSONToFile(Path path, List<SimplexTestData> simplexTestData) {
        ObjectWriter mapper = new ObjectMapper().writerWithDefaultPrettyPrinter();
        try(BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8, StandardOpenOption.CREATE)) {
            mapper.writeValue(writer, simplexTestData);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
