package math.simplex;

import math.simplex.exception.IncompatibleSimplexSolveException;
import math.simplex.exception.SimplexDataException;
import math.util.MathUtil;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import java.util.function.DoublePredicate;
import java.util.function.DoubleUnaryOperator;
import java.util.function.Function;

import static math.simplex.FunctionType.MAX;
import static math.simplex.Inequality.GE;
import static math.simplex.Inequality.LQ;

/**
 * Класс, с помощью которого можно решать задачи дискретной оптимизации. Работает поверх класса {@link Simplex} с
 * использованием <a href=https://ru.wikipedia.org/wiki/Метод_ветвей_и_границ>алгоритма Лэнд и Дойг (метод ветвей и границ)</a>.
 * Каждый узел решается в отдельном потоке (для увеличения скорости вычисления)
 */
public class DiscreteSimplex {

    /**
     * Рекордное значение (наиболее оптимальное значение). До тех пор, пока не вычислено - его значение null.
     */
    private volatile Double recordValue;

    /**
     * Полученный ответ для рекордного значения
     */
    private volatile SimplexAnswer answerValue;

    /**
     * Тип целевой функции. Необходим для выбора более оптимального значений функций из 2 предложенных (f<sub>1</sub> &lt
     * f<sub>2</sub>). При f &rarr min более оптимальное значение - f<sub>1</sub>, при f &rarr max - f<sub>2</sub>
     */
    private FunctionType functionType;

    /**
     * Предикаты (в количестве {@link #xCount}), которые определяют, является ли x<sub>i</sub> корректным значением,
     * удовлетворяющим условию задачи дискретной оптимизации (i = 1 .. {@link #xCount}). По умолчанию, используются
     * предикаты {@link #defaultPredicates(int)}, которые сравнивают число double (вещественное число) с приведённым long
     * (целым числом) c точностью до {@link Simplex#EPSILON}
     */
    private DoublePredicate[] correctValues;

    /**
     * Функции (унарные операторы) в количестве {@link #xCount}, которые преобразуют x<sub>i</sub> в некоторое значение
     * d<sub>i</sub> : d<sub>i</sub> &le x<sub>i</sub>, d<sub>i</sub> &isin D, i = 1 .. {@link #xCount}, D - дискретное
     * множество, удовлетворяющее условию задачи дискретной оптимизации. По умолчанию, используются унарные операторы
     * {@link #defaultLowerBoundFunctions(int)}, которые возвращают ближайшее "целое" число (представленное типом double,
     * {@link Math#floor(double)}), меньшее числа x<sub>i</sub>
     */
    private DoubleUnaryOperator[] lowerBoundFunctions;

    /**
     * Функции (унарные операторы) в количестве {@link #xCount}, которые преобразуют x<sub>i</sub> в некоторое значение
     * e<sub>i</sub> : e<sub>i</sub> &ge x<sub>i</sub>, e<sub>i</sub> &isin D, i = 1 .. {@link #xCount}, D - дискретное
     * множество, удовлетворяющее условию задачи дискретной оптимизации. По умолчанию, используются унарные операторы
     * {@link #defaultUpperBoundFunctions(int)}, которые возвращают ближайшее "целое" число (представленное типом double,
     * {@link Math#ceil(double)}), большее числа x<sub>i</sub>
     */
    private DoubleUnaryOperator[] upperBoundFunctions;

    /**
     * Обработчик ошибок при решении задачи симплекс-методом (все возможные типы ошибок при решении задачи
     * симплекс-методом лежат в пакете {@link math.simplex.exception}. По умолчанию - вывод в консоль {@link System#err}
     * сообщения ошибки ({@link Throwable#getMessage()}). Для установки собственного обработчика ошибок - воспользуйтесь
     * методом {@link #setExceptionHandler(Consumer)}
     */
    private Consumer<Throwable> exceptionHandler = exception -> System.err.println(exception.getMessage());

    /**
     * Количество переменных x. Вычисляется на основании вектора C (коэффициентов целевой функции c<sub>i</sub> перед
     * соответствующими x<sub>i</sub>).
     * @see Simplex.Builder#getC()
     */
    private int xCount;

    /**
     * Количество основных ограничений в первоисходной симплексной задаче. Вычисляется на основании вектора B - правых
     * частей ограничений, b<sub>i</sub>
     * @see Simplex.Builder#getB()
     */
    private int oldConstraintsCount;

    /**
     * Объект для блокировки к доступу (записи) некоторых ресурсов ({@link #recordValue}, {@link #answerValue})
     */
    private final ReentrantLock lock = new ReentrantLock();

    /**
     * Основной метод для взаимодействия пользователя с классом. В качестве аргумента передаётся настроенный
     * {@link Simplex.Builder} для решения задачи линейного программирования симплекс-методом, предикаты
     * ({@link #correctValues}) и унарные операторы ({@link #lowerBoundFunctions}, {@link #upperBoundFunctions})
     * вычисляются по умолчанию, в качестве ответа возвращается {@link SimplexAnswer}, содеражщий вычисленные значения
     * вектора X ({@link SimplexAnswer#getX()}) и значение функции ({@link SimplexAnswer#getFx()}). В процессе вычисления
     * возможны все выбросы исключений при решении задачи симплекс-методом ({@link math.simplex.exception}), которые, при
     * решении во всех узлах (кроме корневого) будут перехвачены обработчиком ошибок {@link #exceptionHandler}
     */
    public SimplexAnswer solve(Simplex.Builder simplexBuilder) {
        initCounts(simplexBuilder);
        initializeValues(simplexBuilder, defaultLowerBoundFunctions(xCount), defaultUpperBoundFunctions(xCount),
                defaultPredicates(xCount));
        solveSimplex(simplexBuilder);
        return answerValue;
    }

    public static DoublePredicate[] defaultPredicates(int xCount) {
        DoublePredicate predicate = value -> {
            long longValue = (long) value;
            return isApproximateValue(value, longValue);
        };

        DoublePredicate[] predicates = new DoublePredicate[xCount];
        Arrays.fill(predicates, predicate);

        return predicates;
    }

    private static boolean isApproximateValue(double value, double actual) {
        return MathUtil.isApproximateValue(value, actual, Simplex.EPSILON);
    }

    public static DoubleUnaryOperator[] defaultUpperBoundFunctions(int xCount) {
        DoubleUnaryOperator operator = Math::ceil;

        DoubleUnaryOperator[] operators = new DoubleUnaryOperator[xCount];
        Arrays.fill(operators, operator);

        return operators;
    }

    public static DoubleUnaryOperator[] defaultLowerBoundFunctions(int xCount) {
        DoubleUnaryOperator operator = Math::floor;

        DoubleUnaryOperator[] operators = new DoubleUnaryOperator[xCount];
        Arrays.fill(operators, operator);

        return operators;
    }

    /**
     * Расширенный метод для взаимодействия пользователя с классом. В качестве аргумента передаётся настроенный
     * {@link Simplex.Builder} для решения задачи линейного программирования симплекс-методом, предикаты
     * ({@link DoublePredicate}) и унарные операторы ({@link DoubleUnaryOperator}) для определения дополнительных
     * ограничений, устанавливаемых задачей дискретной оптимизации; в качестве ответа возвращается {@link SimplexAnswer},
     * содеражщий вычисленные значения вектора X ({@link SimplexAnswer#getX()}) и значение функции
     * ({@link SimplexAnswer#getFx()}). В процессе вычисления возможны все выбросы исключений при решении задачи
     * симплекс-методом ({@link math.simplex.exception}), которые, при решении во всех узлах (кроме корневого) будут
     * перехвачены обработчиком ошибок {@link #exceptionHandler}
     */
    public SimplexAnswer solve(Simplex.Builder simplexBuilder, DoubleUnaryOperator[] lowerBoundFunctions,
                                      DoubleUnaryOperator[] upperBoundFunctions, DoublePredicate[] predicates) {
        initCounts(simplexBuilder);

        validateArrays(simplexBuilder.getC(), lowerBoundFunctions, upperBoundFunctions, predicates);
        initializeValues(simplexBuilder, lowerBoundFunctions, upperBoundFunctions, predicates);
        solveSimplex(simplexBuilder);
        return answerValue;
    }

    private void solveSimplex(Simplex.Builder simplexBuilder) {
        solve(simplexBuilder.build(), Simplex::solve, new Integer[xCount * 2], -1);
    }

    private void initCounts(Simplex.Builder simplexBuilder) {
        this.xCount = simplexBuilder.getC().length;
        this.oldConstraintsCount = simplexBuilder.getB().length;
    }

    /**
     * Решении задачи линейного программирования симплекс-методом. Для корневого узла вызывается {@link Simplex#solve()},
     * для всех остальных узлов - либо {@link Simplex#addConstraint(double[], Inequality, double)} (при добавлении нового
     * ограничения), либо {@link Simplex#changeB(int, double)} (при замене уже добавленного ограничения на новое (замена
     * значения правой части ограничения)). В случае неудовлетворения вычисленного вектора X условиям задачи дискретной
     * оптимизации происходит перевычисление задачи (переход к новому узлу) {@link #resolve(Simplex, Integer[], int, double, int)};
     * в случае удовлетворения - сравнение вычисленного значения с {@link #recordValue} и установка {@link #setAnswer(SimplexAnswer)}
     * в том случае, если значение более оптимальное, чем {@link #recordValue}.
     */
    private void solve(Simplex simplex, Function<Simplex, SimplexAnswer> solver, Integer[] biOrder,
                       int constraintCount) {
        SimplexAnswer answer = solver.apply(simplex);
        double[] X = answer.X();
        double fx = answer.fx();

        if (!isBetter(fx, recordValue)) {
            return;
        }

        Integer xIndex = getInvalidXIndex(X);
        if (xIndex == null) {
            setAnswer(answer);
            return;
        }

        resolve(simplex, biOrder, constraintCount, X[xIndex], xIndex);
    }

    /**
     * Установка вычисленного решения ({@link SimplexAnswer}) в том случае, если вычисленное решение оптимальнее
     * {@link #recordValue}
     */
    private void setAnswer(SimplexAnswer answer) {
        double fx = answer.fx();

        try {
            lock.lock();
            if (!isBetter(fx, recordValue)) {
                return;
            }

            answerValue = answer;
            recordValue = fx;
        } finally {
            lock.unlock();
        }

    }

    /**
     * Сравнение двух значений - нового найденного и рекордного - является ли новое
     * значение более оптимальным, чем рекордного
     * @param newValue - новое значение функции,
     *        oldValue - рекордное значение функции
     */
    private boolean isBetter(double newValue, Double oldValue) {
        if (oldValue == null) {
            return true;
        }

        return functionType == MAX ? newValue > oldValue : newValue < oldValue;
    }

    private Integer getInvalidXIndex(double[] x) {
        for (int i = 0; i < x.length; i++) {
            if (!correctValues[i].test(x[i])) {
                return i;
            }
        }

        return null;
    }

    /**
     * Перевычисление задачи симплекс-методом, с добавлением нового ограничения или изменением правой части ограничения.
     * Перевычисление двух узлов происходит параллельно: первый узел исполнятся асинхронно, второй узел исполняется на
     * том же самом потоке, на котором был вызван метод {@link #resolve(Simplex, Integer[], int, double, int)}. Все
     * необходимые данные для асинхронного потока копируются.
     */
    private void resolve(Simplex simplex, Integer[] biOrder, int constraintCount, double x, int xIndex) {
        double lowerBound = lowerBoundFunctions[xIndex].applyAsDouble(x);
        double upperBound = upperBoundFunctions[xIndex].applyAsDouble(x);

        Simplex copy = simplex.copy();

        Integer[] lowerBiOrder = Arrays.copyOf(biOrder, biOrder.length);
        Integer[] upperBiOrder = biOrder;

        boolean[] isAdded = {false};
        Function<Simplex, SimplexAnswer> lowerBoundFunction = getBoundFunction(biOrder, xIndex, lowerBound, LQ, isAdded);
        if (isAdded[0]) {
            lowerBiOrder[xIndex] = constraintCount + 1;
        }

        Function<Simplex, SimplexAnswer> upperBoundFunction = getBoundFunction(biOrder, xIndex + xCount,
                upperBound, GE, isAdded);
        if (isAdded[0]) {
            upperBiOrder[xIndex + xCount] = constraintCount + 1;
        }

        CompletableFuture<Void>  future = CompletableFuture
                .runAsync(() -> solve(copy, lowerBoundFunction, lowerBiOrder, lowerBiOrder[xIndex]))
                .exceptionally(exception -> {
                    exceptionHandler.accept(exception);
                    return null;
                });

        try {
            solve(simplex, upperBoundFunction, upperBiOrder, upperBiOrder[xIndex + xCount]);
        } catch (IncompatibleSimplexSolveException e) {
            exceptionHandler.accept(e);
        }

        future.join();
    }

    private Function<Simplex, SimplexAnswer> getBoundFunction(Integer[] biOrder, int xIndex, double bound,
                                                              Inequality inequality, boolean[] isAdded) {
        Integer order = biOrder[xIndex];
        if (order != null) {
            isAdded[0] = false;
            return changeBFunction(order + oldConstraintsCount, bound);
        } else {
            isAdded[0] = true;
            return addConstraintFunction(xIndex % xCount, inequality, bound);
        }
    }

    private Function<Simplex, SimplexAnswer> addConstraintFunction(int xIndex, Inequality inequality, double bi) {
        double[] ai = createAi(xIndex);
        return simplex -> simplex.addConstraint(ai, inequality, bi);
    }

    private Function<Simplex, SimplexAnswer> changeBFunction(int index, double bi) {
        return simplex -> simplex.changeB(index, bi);
    }

    private double[] createAi(int xIndex) {
        double[] ai = new double[xCount];
        Arrays.fill(ai, 0);
        ai[xIndex] = 1d;

        return ai;
    }

    private void initializeValues(Simplex.Builder simplexBuilder, DoubleUnaryOperator[] lowerBoundFunctions,
                                  DoubleUnaryOperator[] upperBoundFunctions, DoublePredicate[] predicates) {
        this.lowerBoundFunctions = lowerBoundFunctions;
        this.upperBoundFunctions = upperBoundFunctions;
        this.correctValues = predicates;

        functionType = simplexBuilder.getFunctionType();
        if (functionType == null) {
            functionType = Simplex.defaultFunctionType();
        }

        answerValue = null;
        recordValue = null;
    }

    private void validateArrays(double[] C, DoubleUnaryOperator[] lowerBoundFunctions,
                                       DoubleUnaryOperator[] upperBoundFunctions, DoublePredicate[] predicates) {
        if (C == null) {
            throw new SimplexDataException("C vector is null!");
        }

        validateArrayLength(xCount, lowerBoundFunctions);
        validateArrayLength(xCount, upperBoundFunctions);
        validateArrayLength(xCount, predicates);
    }

    private <T> void validateArrayLength(int length, T[] array) {
        if (array == null || array.length != length) {
            throw new SimplexDataException(String.format("Invalid data: (%s) for this array!", Arrays.toString(array)));
        }
    }

    public void setExceptionHandler(Consumer<Throwable> exceptionHandler) {
        this.exceptionHandler = exceptionHandler;
    }
}
