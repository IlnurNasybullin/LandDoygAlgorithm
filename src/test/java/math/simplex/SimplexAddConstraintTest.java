package math.simplex;

import com.fasterxml.jackson.databind.ObjectMapper;
import data.generation.test.SimplexTestData;
import math.simplex.exception.IncompatibleSimplexSolveException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;
import java.util.stream.Stream;

import static math.simplex.FunctionType.MAX;
import static math.simplex.Inequality.GE;
import static math.simplex.Inequality.LQ;

public class SimplexAddConstraintTest {

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_1.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_1_Success_Data")
    public void test_AddConstraint_1_Success(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_1_Success_Data() {
        double[][] A = {
                {50, 75},
                {60, 30},
                {10, 25}
        };
        double[] B = {15000, 12000, 5000};
        double[] C = {100, 120};

        Inequality[] inequalities = {GE, GE, LQ};

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 3};
        Inequality inequality = LQ;
        double bi = 360;

        double[] X = {240, 40};
        double fx = 28800;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_2.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_2_Success_Data")
    public void test_AddConstraint_2_Success(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_2_Success_Data() {
        double[][] A = {
                {-1, 1},
                {0, 1},
                {1, 0}
        };
        double[] B = {2, 1, 3};
        double[] C = {6, 10};

        Inequality[] inequalities = {LQ, LQ, LQ};
        FunctionType functionType = MAX;

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 0};
        Inequality inequality = LQ;
        double bi = 5;

        double[] X = {3, 1};
        double fx = 28;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_3.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_3_Success_Data")
    public void test_AddConstraint_3_Success(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_3_Success_Data() {
        double[][] A = {
                {5, -2},
                {1, -2},
                {1, 1}
        };
        double[] B = {4, -4, 4};
        double[] C = {1, 2};

        Inequality[] inequalities = {LQ, GE, LQ};
        FunctionType functionType = MAX;

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 0};
        Inequality inequality = LQ;
        double bi = 1.5;

        double[] X = {4d/3, 2 + 2d/3};
        double fx = 20d/3;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_4.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_4_Success_Data")
    public void test_AddConstraint_4_Success(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_4_Success_Data() {
        double[][] A = {
                {5, -2},
                {1, -2},
                {1, 1}
        };
        double[] B = {4, -4, 4};
        double[] C = {1, 2};

        Inequality[] inequalities = {LQ, GE, LQ};
        FunctionType functionType = MAX;

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 0};
        Inequality inequality = LQ;
        double bi = 1;

        double[] X = {1, 5d/2};
        double fx = 6;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_5.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_5_Success_Data")
    public void test_AddConstraint_5_Success(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_5_Success_Data() {
        double[][] A = {
                {1, 2},
                {2, 1},
                {-1, 1},
                {0, 1}
        };
        double[] B = {6, 8, 1, 2};
        double[] C = {3, 2};

        Inequality[] inequalities = {LQ, LQ, LQ, LQ};
        FunctionType functionType = MAX;

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 0};
        Inequality inequality = LQ;
        double bi = 4;

        double[] X = {10d/3, 4d/3};
        double fx = 12 + 2d/3;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_6.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_6_Success_Data")
    public void test_AddConstraint_6_Success(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_6_Success_Data() {
        double[][] A = {
                {1, 2},
                {2, 1},
                {-1, 1},
                {0, 1}
        };
        double[] B = {6, 8, 1, 2};
        double[] C = {3, 2};

        Inequality[] inequalities = {LQ, LQ, LQ, LQ};
        FunctionType functionType = MAX;

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 0};
        Inequality inequality = LQ;
        double bi = 3;

        double[] X = {3, 3d/2};
        double fx = 12;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_7.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_7_IncompatibleSimplexSolveException_Data")
    public void test_AddConstraint_7_IncompatibleSimplexSolveException_Success(Simplex simplex,
                                                                       double[] ai, Inequality inequality, double bi,
                                                                       Class<? extends Exception> exceptionClass) {
        Assertions.assertThrows(exceptionClass, () -> simplex.addConstraint(ai, inequality, bi));
    }

    public static Stream<Arguments> addConstraint_7_IncompatibleSimplexSolveException_Data() {
        double[][] A = {
                {1, 2},
                {2, 1},
                {1, 3},
                {0, 1}
        };
        double[] B = {6, 8, 9, 2};
        double[] C = {3, 2};

        Inequality[] inequalities = {LQ, LQ, LQ, LQ};
        FunctionType functionType = MAX;

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 1};
        Inequality inequality = GE;
        double bi = 5;

        Class<? extends Exception> exceptionClass = IncompatibleSimplexSolveException.class;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, exceptionClass));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_add_constraint_8.png"/>
     */
    @ParameterizedTest
    @MethodSource("addConstraint_8_Success_Data")
    public void test_AddConstraint_8_Success(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_8_Success_Data() {
        double[][] A = {
                {1, 2},
                {2, 1},
                {1, 3},
                {0, 1}
        };
        double[] B = {6, 8, 9, 2};
        double[] C = {3, 2};

        Inequality[] inequalities = {LQ, LQ, LQ, LQ};
        FunctionType functionType = MAX;

        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        simplex.solve();

        double[] ai = {1, 0};
        Inequality inequality = GE;
        double bi = 3.5;

        double[] X = {3.5, 1};
        double fx = 12.5;

        return Stream.of(Arguments.of(simplex, ai, inequality, bi, X, fx));
    }

    @ParameterizedTest
    @MethodSource("addConstraint_Random_Data")
    public void test_AddConstraint_Random(Simplex simplex, double[] ai, Inequality inequality, double bi,
                                             double[] expectedX, double expectedFx) {
        SimplexAnswer answer = simplex.addConstraint(ai, inequality, bi);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> addConstraint_Random_Data() throws IOException {
        SimplexTestData[] data = getTestData();
        SimplexTestData randomData = data[new Random().nextInt(data.length) - 1];
        Simplex.Builder builder = randomData.getSimpleData();

        Constraint addConstraint = randomData.getAddConstraint();
        double[] ai = addConstraint.getAi();
        double[][] A = addRow(builder.getA(), ai);

        double bi = addConstraint.getBi();
        double[] B = concatenate(builder.getB(), bi);

        double[] C = builder.getC();

        FunctionType functionType = builder.getFunctionType();

        Inequality inequality = addConstraint.getInequality();
        Inequality[] inequalities = concatenate(builder.getInequalities(), inequality);

        boolean[] normalizedX = builder.getNormalizedX();

        Simplex testSimplex = builder.build();
        Simplex resultSimplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .setNormalizedX(normalizedX)
                .build();

        testSimplex.solve();

        SimplexAnswer result = resultSimplex.solve();

        double[] X = result.X();
        double fx = result.fx();

        return Stream.of(Arguments.of(testSimplex, ai, inequality, bi, X, fx));
    }

    private static <T> T[] concatenate(T[] array, T value) {
        int index = array.length;
        T[] newArray = Arrays.copyOf(array, index + 1);
        newArray[index] = value;

        return newArray;
    }

    private static double[] concatenate(double[] vector, double v) {
        int index = vector.length;
        double[] newVector = Arrays.copyOf(vector, index + 1);
        newVector[index] = v;

        return newVector;
    }

    private static double[][] addRow(double[][] matrix, double[] row) {
        double[][] newMatrix = new double[matrix.length + 1][];
        int i = 0;
        for (; i < matrix.length; i++) {
            newMatrix[i] = matrix[i];
        }

        newMatrix[i] = row;

        return newMatrix;
    }

    private static SimplexTestData[] getTestData() throws IOException {
        Path path = Paths.get("src", "test", "resources", "math", "simplex", "data", "test_add_constraint_data.json");

        return new ObjectMapper().readerFor(SimplexTestData[].class).readValue(path.toFile());
    }
}
