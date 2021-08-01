package math.simplex;

import math.simplex.exception.UnlimitedFunctionMaximizeException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static math.simplex.FunctionType.MAX;
import static math.simplex.Inequality.GE;
import static math.simplex.Inequality.LQ;

public class SimplexTest {

    /**
     * <img src="../../../resources/math/simplex/img/test_1.png"/>
     */
    @ParameterizedTest
    @MethodSource("_1_Success_Data")
    public void test_1_Success(double[][] A, double[] B, double[] C, double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .build();
        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _1_Success_Data() {
        double[][] A = {
                {-1, 1, 1, 2, -3},
                {1, 1, 4, 1, -8},
                {0, 1, 1, 0, -4}
        };
        double[] B = {4, 3, -4};
        double[] C = {-1, -1, 1, 3, 7};
        double[] X = {5, 0, 0, 6, 1};
        double fx = 20d;

        return Stream.of(Arguments.of(
            A, B, C, X, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_2.png"/>
     */
    @ParameterizedTest
    @MethodSource("_2_Success_Data")
    public void test_2_Success(double[][] A, double[] B, double[] C, FunctionType functionType,
                               Inequality[] inequalities, double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();
        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _2_Success_Data() {
        double[][] A = {
                {1, 0},
                {0, 1},
                {1, 1},
                {1, 2}
        };
        double[] B = {40, 30, 60, 80};
        double[] C = {2, 3};
        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, LQ, LQ, LQ};

        double[] X = {40, 20};
        double fx = 140d;

        return Stream.of(Arguments.of(
                A, B, C, functionType, inequalities, X, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_3.png"/>
     */
    @ParameterizedTest
    @MethodSource("_3_Success_Data")
    public void test_3_Success(double[][] A, double[] B, double[] C, Inequality[] inequalities,
                               double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setInequalities(inequalities)
                .build();
        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _3_Success_Data() {
        double[][] A = {
                {2, -1, 1},
                {4, -2, 1},
                {3, 0, 1}
        };
        double[] B = {1, -2, 5};
        double[] C = {1, -1, -3};

        Inequality[] inequalities = {LQ, GE, LQ};

        double[] X = {1d/3, 11d/3, 4};
        double fx = -46d/3;

        return Stream.of(Arguments.of(
                A, B, C, inequalities, X, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_4.png"/>
     */
    @ParameterizedTest
    @MethodSource("_4_Success_Data")
    public void test_4_Success(double[][] A, double[] B, double[] C, FunctionType functionType,
                               Inequality[] inequalities, double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();
        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _4_Success_Data() {
        double[][] A = {
                {-2, 3},
                {1, 1},
                {3, -5}
        };
        double[] B = {12, 9, 3};
        double[] C = {1, -1};
        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, LQ, LQ};

        double[] X = {6, 3};
        double fx = 3d;

        return Stream.of(Arguments.of(
                A, B, C, functionType, inequalities, X, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_5.png"/>
     */
    @ParameterizedTest
    @MethodSource("_5_Success_UnlimitedFunctionMaximizeException_Data")
    public void test_5_Success_UnlimitedFunctionMaximizeException(double[][] A, double[] B, double[] C,
                                                                 FunctionType functionType, Inequality[] inequalities,
                                                                 Class<? extends Throwable> expectedExceptionClass) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        Assertions.assertThrows(expectedExceptionClass, simplex::solve);
    }

    public static Stream<Arguments> _5_Success_UnlimitedFunctionMaximizeException_Data() {
        double[][] A = {
                {-2, 3},
                {3, -5}
        };
        double[] B = {12, 3};
        double[] C = {1, 1};
        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, LQ};

        Class<? extends Throwable> expectedException = UnlimitedFunctionMaximizeException.class;

        return Stream.of(Arguments.of(
                A, B, C, functionType, inequalities, expectedException
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_6.png"/>
     */
    @ParameterizedTest
    @MethodSource("_6_Success_Data")
    public void test_6_Success(double[][] A, double[] B, double[] C, FunctionType functionType,
                               Inequality[] inequalities, double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _6_Success_Data() {
        double[][] A = {
                {-2, 3},
                {1, 1},
                {3, -5}
        };
        double[] B = {12, 9, 3};
        double[] C = {1, -1};
        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, LQ, GE};

        double[] X = {9, 0};
        double fx = 9;

        return Stream.of(Arguments.of(
                A, B, C, functionType, inequalities, X, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_7.png"/>
     */
    @ParameterizedTest
    @MethodSource("_7_Success_Data")
    public void test_7_Success(double[][] A, double[] B, double[] C, FunctionType functionType,
                               Inequality[] inequalities, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities)
                .build();

        SimplexAnswer answer = simplex.solve();

        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _7_Success_Data() {
        double[][] A = {
                {-2, 3},
                {1, 1},
                {1, -1}
        };
        double[] B = {12, 9, 3};
        double[] C = {1, -1};
        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, LQ, LQ};

        double fx = 3;

        return Stream.of(Arguments.of(
                A, B, C, functionType, inequalities, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_8.png"/>
     */
    @ParameterizedTest
    @MethodSource("_8_Success_Data")
    public void test_8_Success(double[][] A, double[] B, double[] C, FunctionType functionType,
                               double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .build();

        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _8_Success_Data() {
        double[][] A = {
                {1, 3, 2, 2},
                {2, 2, 1, 1}
        };
        double[] B = {3, 3};
        double[] C = {5, 3, 4, -1};
        FunctionType functionType = MAX;

        double[] X = {1, 0, 1, 0};
        double fx = 9;

        return Stream.of(Arguments.of(
                A, B, C, functionType, X, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_9.png"/>
     */
    @ParameterizedTest
    @MethodSource("_9_Success_Data")
    public void test_9_Success(double[][] A, double[] B, double[] C, Inequality[] inequalities, boolean[] normalizedX,
                               double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setInequalities(inequalities)
                .setNormalizedX(normalizedX)
                .build();

        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _9_Success_Data() {
        double[][] A = {
                {1, 1},
                {0, -1}
        };
        double[] B = {1, 3};
        double[] C = {-1, 2};

        Inequality[] inequalities = {LQ, LQ};
        boolean[] normalizedX = {false, false};

        double[] X = {4, -3};
        double fx = -10;

        return Stream.of(Arguments.of(
                A, B, C, inequalities, normalizedX, X, fx
        ));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_10.png"/>
     */
    @ParameterizedTest
    @MethodSource("_10_Success_Data")
    public void test_10_Success(double[][] A, double[] B, double[] C, boolean[] normalizedX,
                               double[] expectedX, double expectedFx) {
        Simplex simplex = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setNormalizedX(normalizedX)
                .build();

        SimplexAnswer answer = simplex.solve();

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _10_Success_Data() {
        double[][] A = {
                {2, -1, 0, -2, -2},
                {-1, 2, 1, 1, 0},
                {1, -2, 0, 2, 3}
        };
        double[] B = {4, 8, 6};
        double[] C = {1, 2, -2, 1, 1};

        boolean[] normalizedX = {true, true, false, true, false};

        double[] X = {3, 0, 11, 0, 1};
        double fx = -18;

        return Stream.of(Arguments.of(
                A, B, C, normalizedX, X, fx
        ));
    }
}
