package math.simplex;

import org.hamcrest.Matcher;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.DoubleStream;
import java.util.stream.Stream;

import static math.simplex.FunctionType.MAX;
import static math.simplex.Inequality.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DiscreteSimplexTest {

    /**
     * <img src="../../../resources/math/simplex/img/test_discrete_simplex_1.png"/>
     */
    @ParameterizedTest
    @MethodSource("boundedKnapsack_1_Success_Data")
    public void test_BoundedKnapsack_1_Success(Simplex.Builder builder, double[] expectedX, double expectedFx) {
        DiscreteSimplex simplex = new DiscreteSimplex();
        SimplexAnswer answer = simplex.solve(builder);

        System.out.println(Arrays.toString(answer.X()));
        System.out.println(answer.fx());

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> boundedKnapsack_1_Success_Data() {
        double[][] A = {
                {1, 2, 3, 1, 1},
                {1, 1, 1, 1, 1},
                {1, 0, 0, 0, 0},
                {0, 1, 0, 0, 0},
                {0, 0, 1, 0, 0},
                {0, 0, 0, 1, 0},
                {0, 0, 0, 0, 1}
        };

        double[] B = {7, 5, 1, 3, 3, 1, 2};
        double[] C = {2, 3, 2, 4, 1};

        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, EQ, LQ, LQ, LQ, LQ, LQ};

        Simplex.Builder builder = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities);

        double[] X = {0, 3, 0, 1, 0};
        double fx = 13;

        return Stream.of(Arguments.of(builder, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_discrete_simplex_2.png"/>
     */
    @ParameterizedTest
    @MethodSource("_0_1_KnapsackProblem_1_Success_Data")
    public void test_0_1_KnapsackProblem_1_Success(Simplex.Builder builder, double[] expectedX, double expectedFx) {
        DiscreteSimplex simplex = new DiscreteSimplex();
        SimplexAnswer answer = simplex.solve(builder);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _0_1_KnapsackProblem_1_Success_Data() {
        double[][] A = {
                {1, 2, 1, 2, 3, 3, 2},
                {1, 2, 1, 3, 2, 2, 2},
                {1, 0, 0, 0, 0, 0, 0},
                {0, 1, 0, 0, 0, 0, 0},
                {0, 0, 1, 0, 0, 0, 0},
                {0, 0, 0, 1, 0, 0, 0},
                {0, 0, 0, 0, 1, 0, 0},
                {0, 0, 0, 0, 0, 1, 0},
                {0, 0, 0, 0, 0, 0, 1},
        };

        double[] B = {6, 8, 1, 1, 1, 1, 1, 1, 1};
        double[] C = {3, 12, 5, 12, 11, 10, 7};

        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, LQ, LQ, LQ, LQ, LQ, LQ, LQ, LQ};

        Simplex.Builder builder = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities);

        double[] X = {1, 1, 1, 1, 0, 0, 0};
        double fx = 32;

        return Stream.of(Arguments.of(builder, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_discrete_simplex_3.png"/>
     */
    @ParameterizedTest
    @MethodSource("boundedKnapsack_2_Success_Data")
    public void test_BoundedKnapsack_2_Success(Simplex.Builder builder, Set<Double[]> expectedX, double expectedFx) {
        DiscreteSimplex simplex = new DiscreteSimplex();
        SimplexAnswer answer = simplex.solve(builder);

        Double[] X = DoubleStream.of(answer.X()).boxed().toArray(Double[]::new);

        assertThat(expectedX, hasItem(arrayCloseTo(X, Simplex.EPSILON)));
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Matcher<Double[]> arrayCloseTo(Double[] array, double error) {
        List<Matcher<? super Double>> matchers = new ArrayList<>();
        for (double d : array) {
            matchers.add(closeTo(d, error));
        }
        return arrayContaining(matchers);
    }

    public static Stream<Arguments> boundedKnapsack_2_Success_Data() {
        double[][] A = {
                {2, 4, 3, 1}
        };

        double[] B = {7};
        double[] C = {5, 4, 8, 2};

        FunctionType functionType = MAX;
        Inequality[] inequalities = {EQ};

        Simplex.Builder builder = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities);

        Double[] X1 = {2d, 0d, 1d, 0d};
        Double[] X2 = {0d, 0d, 2d, 1d};
        double fx = 18;

        return Stream.of(Arguments.of(builder, Set.of(X1, X2), fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_discrete_simplex_4.png"/>
     */
    @ParameterizedTest
    @MethodSource("integerProgramming_1_Success_Data")
    public void test_IntegerProgramming_1_Success(Simplex.Builder builder, double[] expectedX, double expectedFx) {
        DiscreteSimplex simplex = new DiscreteSimplex();
        SimplexAnswer answer = simplex.solve(builder);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> integerProgramming_1_Success_Data() {
        double[][] A = {
                {1, 2},
                {1, 2},
                {2, 1},
        };

        double[] B = {10, 2, 10};
        double[] C = {12, 7};

        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, GE, LQ};

        Simplex.Builder builder = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities);

        double[] X = {4, 2};
        double fx = 62;

        return Stream.of(Arguments.of(builder, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_discrete_simplex_5.png"/>
     */
    @ParameterizedTest
    @MethodSource("integerProgramming_2_Success_Data")
    public void test_IntegerProgramming_2_Success(Simplex.Builder builder, double[] expectedX, double expectedFx) {
        DiscreteSimplex simplex = new DiscreteSimplex();
        SimplexAnswer answer = simplex.solve(builder);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> integerProgramming_2_Success_Data() {
        double[][] A = {
                {2, 11},
                {1, 1},
                {4, -5},
        };

        double[] B = {38, 7, 5};
        double[] C = {-1, -1};

        Inequality[] inequalities = {LQ, LQ, LQ};

        Simplex.Builder builder = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setInequalities(inequalities);

        double[] X = {3, 2};
        double fx = -5;

        return Stream.of(Arguments.of(builder, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_discrete_simplex_6.png"/>
     */
    @ParameterizedTest
    @MethodSource("integerProgramming_3_Success_Data")
    public void test_IntegerProgramming_3_Success(Simplex.Builder builder, double[] expectedX, double expectedFx) {
        DiscreteSimplex simplex = new DiscreteSimplex();
        SimplexAnswer answer = simplex.solve(builder);

        Assertions.assertArrayEquals(expectedX, answer.X(), Simplex.EPSILON);
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> integerProgramming_3_Success_Data() {
        double[][] A = {
                {3, 3},
                {6, -3},
        };

        double[] B = {11, 1};
        double[] C = {2, 5};

        Inequality[] inequalities = {GE, LQ};

        Simplex.Builder builder = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setInequalities(inequalities);

        double[] X = {1, 3};
        double fx = 17;

        return Stream.of(Arguments.of(builder, X, fx));
    }

    /**
     * <img src="../../../resources/math/simplex/img/test_discrete_simplex_7.png"/>
     */
    @ParameterizedTest
    @MethodSource("_0_1_KnapsackProblem_2_Success_Data")
    public void test_0_1_KnapsackProblem_2_Success(Simplex.Builder builder, Set<Double[]> expectedX, double expectedFx) {
        DiscreteSimplex simplex = new DiscreteSimplex();
        SimplexAnswer answer = simplex.solve(builder);

        Double[] X = DoubleStream.of(answer.X()).boxed().toArray(Double[]::new);

        assertThat(expectedX, hasItem(arrayCloseTo(X, Simplex.EPSILON)));
        Assertions.assertEquals(expectedFx, answer.fx(), Simplex.EPSILON);
    }

    public static Stream<Arguments> _0_1_KnapsackProblem_2_Success_Data() {
        double[][] A = {
                {2, 2, 1, 2},
                {1, 0, 0, 0},
                {0, 1, 0, 0},
                {0, 0, 1, 0},
                {0, 0, 0, 1},
        };

        double[] B = {4, 1, 1, 1, 1};
        double[] C = {5, 3, 4, 4};

        FunctionType functionType = MAX;
        Inequality[] inequalities = {LQ, LQ, LQ, LQ, LQ};

        Simplex.Builder builder = new Simplex.Builder()
                .setA(A)
                .setB(B)
                .setC(C)
                .setFunctionType(functionType)
                .setInequalities(inequalities);

        Double[] X1 = {1d, 0d, 1d, 0d};
        Double[] X2 = {1d, 0d, 0d, 1d};
        double fx = 9;

        return Stream.of(Arguments.of(builder, Set.of(X1, X2), fx));
    }
}