package data.generation.test;

import math.simplex.Constraint;
import math.simplex.FunctionType;
import math.simplex.Inequality;
import math.simplex.Simplex;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import static math.simplex.FunctionType.MAX;
import static math.simplex.FunctionType.MIN;
import static math.simplex.Inequality.*;

public class RandomTaskGenerator {

    private final ThreadLocalRandom random;
    private final String threadName;
    private final Inequality[] allowedInequalities =
            {LQ, GE, EQ};
//    Inequality.values();

    public RandomTaskGenerator(String threadName) {
        random = ThreadLocalRandom.current();
        this.threadName = threadName;
    }

    public Simplex.Builder generateSimplexBuilder() {
        int xCount = random.nextInt(3, 8);
        int constraintsCount = random.nextInt(2, 6);

        return new Simplex.Builder()
                .setA(generateA(constraintsCount, xCount))
                .setB(generateB(constraintsCount))
                .setC(generateC(xCount))
                .setFunctionType(generateFunctionType())
                .setInequalities(generateInequalities(constraintsCount))
                .setNormalizedX(generateNormalizedX(xCount));
    }

    private boolean[] generateNormalizedX(int xCount) {
        boolean[] normalizedX = new boolean[xCount];
        for (int i = 0; i < xCount; i++) {
            normalizedX[i] = random.nextBoolean();
        }

        System.out.printf("(Thread - %s) normalizedX - %s\n", threadName, Arrays.toString(normalizedX));

        return normalizedX;
    }

    private Inequality[] generateInequalities(int constraintsCount) {
        Inequality[] inequalities = new Inequality[constraintsCount];
        for (int i = 0; i < inequalities.length; i++) {
            inequalities[i] = getRandomInequality();
        }

        System.out.printf("(Thread - %s) Inequalities - %s\n", threadName, Arrays.toString(inequalities));

        return inequalities;
    }

    private Inequality getRandomInequality() {
        int length = allowedInequalities.length;
        return allowedInequalities[random.nextInt(length)];
    }

    private FunctionType generateFunctionType() {
        return random.nextBoolean() ? MIN : MAX;
    }

    private double[] generateC(int xCount) {
        double[] C = new double[xCount];
        for (int i = 0; i < xCount; i++) {
            do {
                C[i] = random.nextInt(-5, 6);
            } while (C[i] == 0d);
        }

        System.out.printf("(Thread - %s) C - %s\n", threadName, Arrays.toString(C));

        return C;
    }

    private double[] generateB(int constraintsCount) {
        double[] B = new double[constraintsCount];
        for (int i = 0; i < constraintsCount; i++) {
            B[i] = getRandomBi();
        }

        System.out.printf("(Thread - %s) B - %s\n", threadName, Arrays.toString(B));

        return B;
    }

    private double[][] generateA(int constraintsCount, int xCount) {
        double[][] A = new double[constraintsCount][xCount];
        for (int i = 0; i < constraintsCount; i++) {
            for (int j = 0; j < xCount; j++) {
                A[i][j] = getRandomAij();
            }
        }

        System.out.printf("(Thread - %s) A - %s\n", threadName, Arrays.deepToString(A));

        return A;
    }

    private double getRandomAij() {
        return random.nextInt(-10, 11);
    }

    public double[] generateChangeB(double[] B) {
        double[] changeB = new double[B.length];

        for (int i = 0; i < B.length; i++) {
            changeB[i] = random.nextInt(-5, 6) * 10;
        }

        System.out.printf("(Thread - %s) changed B - %s\n", threadName, Arrays.toString(changeB));

        return changeB;
    }

    private double getRandomBi() {
        return random.nextInt(10, 101) * 10;
    }

    public Constraint generateConstraint(int xCount) {
        double[] ai = new double[xCount];
        for (int i = 0; i < xCount; i++) {
            ai[i] = getRandomAij();
        }

        Inequality inequality = getRandomInequality();
        double bi = getRandomBi();

        Constraint constraint = new Constraint();
        constraint.setAi(ai);
        constraint.setBi(bi);
        constraint.setInequality(inequality);

        return constraint;
    }

}
