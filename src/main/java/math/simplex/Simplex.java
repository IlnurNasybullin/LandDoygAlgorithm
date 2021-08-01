package math.simplex;

import math.simplex.exception.*;
import math.util.MathUtil;
import org.jblas.DoubleMatrix;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

import static math.simplex.FunctionType.MAX;
import static math.simplex.FunctionType.MIN;
import static math.simplex.Inequality.*;
import static org.jblas.DoubleMatrix.concatHorizontally;
import static org.jblas.DoubleMatrix.concatVertically;

/**
 * Класс, с помощью которого можно решить задачу <a href=https://ru.wikipedia.org/wiki/Линейное_программирование>линейного программирования</a>
 * <a href=https://ru.wikipedia.org/wiki/Симплекс-метод>симплекс-методом</a>. Вычисление симплекс-методом осуществлеяется
 * <a href=https://ru.wikipedia.org/wiki/Симплекс-метод>двухфазным методом</a> (методом внедрения искусственного базиса
 * и выведением искусственных переменных из базиса на первой фазе). Класс <b>не является потокобезопасным</b>, поэтому:<br/>
 * <ol>
 *     <li>Все предоставляемые гарантии работают лишь в том случае, если объект создавался и работал в потокобезопасном
 *     методе</li>
 *     <li>для возможности вычислений в параллельных потоках следует у основного объекта вызвать метод {@link #copy()}
 *     для копирования внутренних полей объекта в новый объект</li>
 * </ol>
 */
public class Simplex implements Serializable {

    private static final long versionUID = -8213458154269956034L;

    /**
     * Константа, используемая в качестве &#949; для сравнения двух чисел с плавающей точкой. Гарантируется, что с
     * точностью до &#949; решение (значение целевой функции и значения переменных x) будет правильным
     */
    public static final double EPSILON = 1e-8d;

    /**
     * Тип целевой функции. По умолчанию - {@link FunctionType#MIN}
     * @see #defaultFunctionType()
     */
    private FunctionType functionType;

    /**
     * Знаки неравенств для ограничений. По умолчанию - {@link Inequality#EQ}
     * @see #defaultInequalities(int)
     */
    private Inequality[] inequalities;

    /**
     * Нормализованы ли переменные x<sub>i</sub> (x<sub>i</sub> &ge 0), i = 1..n. По умолчанию - нормализованы (т.е.
     * присутствуют дополнительные ограничения)
     * @see #defaultNormalizedX(int)
     */
    private boolean[] normalizedX;

    /**
     * Расширенная матрица ограничений, над которой происходят все вычисления. В процессе вычислений приводится к
     * следующему виду:<br/>
     * <img src="../../../resources/math/simplex/img/A.png"/>
     * где P<sub>ij</sub> (j=1..n) - некоторый базис,<br/>
     * &#8710;<sub>JG</sub> - оценка Жордана-Гаусса (первое значение оценки (слева направо) - значение целевой функции),<br/>
     * B<sup>-1</sup>P<sub>0</sub> - вектор правых (свободных) частей ограничений, значения переменных x соответствующих
     * базисов,<br/>,
     * n - количество основных ограничений ограничений ({@link #B}),<br/>
     * m - количество переменных в целевой функции ({@link #C}), <br/>
     * [a &times b] - размерность матрицы - а строк и b столбцов,<br/>
     * A<sup>'</sup> - исходная матрица ограничений,<br/>
     * replacingX - см. {@link #replacingIndex} (этот столбец опционален, т.е. он может отсутствовать), <br/>
     * dopBasis - дополнительный базис (см. {@link #dopBasisIndex}), <br/>
     * artificialBasis - искусственный базис (см. {@link #artificialBasisIndex})<br/>
     * Гарантируется, что после канонизации задачи (приведении задачи к каноническому виду) вся матрица будет приведена
     * к этому виду без последней строки (строки оценок)
     * @see #canonize()
     */
    private DoubleMatrix A;

    /**
     * Вектор правых (свободных) частей ограничений. Количество столбцов определяет количество ограничений
     */
    private DoubleMatrix B;

    /**
     * Вектор-строка коэффициентов целевой функции. Изначально содержит лишь m коэффициентов, стоящих перед переменными
     * x, однако, в процессе канонизации задачи, с изменением матрицы {@link #A} вектор-строка {@link #C} также будет
     * меняться - добавляться соответствующие переменные (0) перед неизвестными x
     * @see #canonize()
     */
    private DoubleMatrix C;

    /**
     * Вектор-строка коэффициентов целевой функции, используемой лишь на первой фазе двухфазного симплекс-метода (в
     * качестве коэффициентов для новой целевой функции, которая должна быть обращена к 0).
     * @see #artificialBasis()
     * @see #calculateArtificialBasis()
     */
    private DoubleMatrix C_i;

    /**
     * Индекс j для заменяющей переменной x<sub>j</sub>. Заменящая переменная используется (т.е. добавляется некоторый
     * коэффициент в векторе-строке {@link #C} и столбец в матрице {@link #A}) в том случае, если отсутствует хотя бы одно
     * дополнительное ограничение, т.е. &exist x<sub>i</sub>: x<sub>i</sub> &#8817; 0  i=1..m (см. {@link #normalizedX}).
     * Индекс j может и отсутствовать (быть null). В этом случае все x<sub>i</sub> (i=1..m) считаются нормализованными.
     * Гарантируется, что после канонизации задачи можно будет достоверно знать значение индекса j.
     * @see #A
     * @see #canonize()
     * @see #replacingX()
     */
    private Integer replacingIndex;

    /**
     * Индекс, с которого можно обратиться к первому базису дополнительного базиса. Дополнительный базис необходим для
     * выравнивания неравенст основных ограничений (т.е. добавлением дополнительной переменной с некоторым коэффициентов
     * для перевода неравенства в уравнение). Гарантируется, что после канонизации задачи можно будет достоверно узнать
     * индекс первого базиса дополнительного базиса.
     * @see #A
     * @see #canonize()
     * @see #equalization()
     */
    private int dopBasisIndex;

    /**
     * Индекс, с которого можно обратиться к первому базису искусственного базиса. Искусственный базис используется в
     * качестве первичного базиса в первой фазе двухфазного метода. Кроме того, он задействован и для тех случаев, когда
     * задача должна быть перерешена (см. {@link #changeB(double[])}, {@link #addConstraint(double[], Inequality, double)}).
     * Гарантируется, что после канонизации задачи можно будет достоверно узнать индекс первого базиса искусственного
     * базиса.
     * @see #A
     * @see #canonize()
     * @see #artificialBasis()
     */
    private int artificialBasisIndex;

    /**
     * Индексы ji (см. {@link #A}) первичных базисов (т.е. переменных x, лежащих в базисе)
     */
    private int[] bases;

    /**
     * Флажок, сигнализирующий о том, была ли задача уже решена.
     */
    private boolean solved;

    /**
     * Приватный конструктор, необходим для нормальной сериализации
     */
    private Simplex() {}

    /**
     * Приватный конструктор для создания объекта симплекса. Класс невозможно создать напрямую (для возможности
     * пользователю передачи лишь тех параметров, что нужны, используйте {@link Simplex.Builder}). На этом этапе
     * гарантируется канонизация задачи (<b>даже в том случае, если задача изначально представлена в каноническом виде</b>)
     */
    private Simplex(double[][] A, double[] B, double[] C, FunctionType functionType, Inequality[] inequalities,
                    boolean[] normalizedX) {
        this.functionType = functionType;
        this.inequalities = inequalities;
        this.normalizedX = normalizedX;

        this.A = new DoubleMatrix(A);
        this.B = new DoubleMatrix(B);
        this.C = new DoubleMatrix(1, C.length, C);
        this.solved = false;

        canonize();
    }

    /**
     * Канонизация задачи линейного программирования. Гарантируется следующий порядок канонизации:<br/>
     * <ol>
     *     <li>{@link #replacingX() замена переменных}</li>
     *     <li>{@link #canonizeObjectiveFunction() канонизация целевой функции} (приведение её к задаче минимизации)</li>
     *     <li>{@link #canonizeB() канонизация вектора правых (свободных) частей ограничений}</li>
     *     <li>{@link #equalization() выравнивание неравенст}</li>
     *     <li>{@link #artificialBasis() добавление искусственного базиса}</li>
     * </ol>
     * Кроме того, в процессе канонизации матрица {@link #A} и вектор-строка {@link #C} модернизируется, а переменным
     * {@link #replacingIndex}, {@link #dopBasisIndex}, {@link #artificialBasisIndex} присваиваются <b>относительно
     * неизменные</b> значения (т.е. значения могут измениться лишь в процессе перевычислении задачи (см.
     * {@link #changeB(double[])}, {@link #addConstraint(double[], Inequality, double)})). Гарантируется, что после
     * канонизации вектор-строка {@link #C} и матрица {@link #A} будут иметь одинакое количество столбцов
     */
    private void canonize() {
        replacingX();
        canonizeObjectiveFunction();
        canonizeB();
        equalization();
        addB0();
        artificialBasis();
    }

    /**
     * Добавление искусственного базиса - единичной матрицы размерности n &times; n, n - количество ограничений
     */
    private void artificialBasis() {
        artificialBasisIndex = C.length;
        A = concatHorizontally(A, DoubleMatrix.eye(B.length));
        C_i = concatHorizontally(DoubleMatrix.zeros(1, C.length), DoubleMatrix.ones(1, B.length));
    }

    /**
     * Добавление вектора B<sup>-1</sup>P<sub>0</sub> в матрицу {@link #A} и коэффицента перед x<sub>0</sub> (0) в
     * вектор-строку {@link #C}
     */
    private void addB0() {
        DoubleMatrix B0 = B.dup();
        A = concatHorizontally(B0, A);
        C = concatHorizontally(DoubleMatrix.scalar(0d), C);
    }

    /**
     * Выравнивание ограничений - перевод из неравенств/уравнений в уравнения путём добавления дополнительного базиса.
     * Технически, дополнительный базис будет представлять собой диагональную матрицу, где элементы a<sub>ii</sub> (i=1..n)
     * могут принимать следующие значения:<br/>
     * <ul>
     *     <li>1, если ограничение i представлено неравенством вида f<sub>i</sub>(X) &lt;/&le; b<sub>i</sub></li>
     *     <li>0, если ограничение i представлено в уравнения: f<sub>i</sub>(X) = b<sub>i</sub></li>
     *     <li>-1, если ограничение i представлено неравенством вида f<sub>i</sub>(X) &gt;/&ge; b<sub>i</sub></li>
     * </ul>
     * В процессе вычисления, дополнительный базис будет менять своё содержимое
     */
    private void equalization() {
        dopBasisIndex = C.length + 1;
        double[] dopBasisDiagonals = new double[B.length];
        for (int i = 0; i < inequalities.length; i++) {
            dopBasisDiagonals[i] = getInequalityCoeff(inequalities[i]);
        }

        C = concatHorizontally(C, DoubleMatrix.zeros(1, B.length));
        A = concatHorizontally(A, DoubleMatrix.diag(new DoubleMatrix(dopBasisDiagonals)));
    }

    private double getInequalityCoeff(Inequality inequality) {
        if (inequality == GE || inequality == GR) {
            return -1;
        }

        if (inequality == LE || inequality == LQ) {
            return 1;
        }

        return 0;
    }

    /**
     * Канонизация вектора правых (свободных) частей ограничений - инвентирование ограничения i (i=1..n) в том случае,
     * если b<sub>i</sub> < 0. Инвертирование ограничения - замена знаков (с положительного на отрицательный, и наоборот) 
     * для всех коэффициентов ограничения и знака неравенства для самого ограничения.
     * @see Inequality#inversion(Inequality) 
     */
    private void canonizeB() {
        for (int i = 0; i < B.rows; i++) {
            if (B.get(i) < 0) {
                A.mulRow(i, -1);
                B.mulRow(i, -1);
                inequalities[i] = Inequality.inversion(inequalities[i]);
            }
        }
    }

    /**
     * Канонизация целевой функции - замена знаков коэффициентов целевой функции в том случае, если тип целевой функции - 
     * {@link FunctionType#MAX}
     */
    private void canonizeObjectiveFunction() {
        if (functionType == MAX) {
            C.negi();
        }
    }

    /**
     * Замена переменных - фактически происхожит лишь в том случае, если хотя бы одна из переменных x ненормализована
     * (не содержит дополнительного ограничения x<sub>i</sub> &ge; 0, i=1..n). Технически, для матрицы {@link #A}
     * представляет собой суммирование ненормализованных векторов (x) с противоположными знаками, для вектора-строки
     * {@link #C} - суммирование коэффициентов ненормализованных x с противоположными знаками.
     */
    private void replacingX() {
        double c_n = 0d;
        DoubleMatrix A_n = DoubleMatrix.zeros(A.rows, 1);

        for (int i = 0; i < normalizedX.length; i++) {
            if (!normalizedX[i]) {
                replacingIndex = C.length + 1;
                c_n -= C.get(i);
                A_n.subi(A.getColumn(i));
            }
        }

        if (replacingIndex != null) {
            C = concatHorizontally(C, DoubleMatrix.scalar(c_n));
            A = concatHorizontally(A, A_n);
        }
    }

    /**
     * Основной метод взаимодействия пользователя с классом. Служит для решения задачи линейного программирования
     * симплекс-методом. Гарантируется, что к моменту вызова данного метода, задача была уже канонизирована. Если задача
     * уже была решена - возвращается ответ, без перевычислений
     * @see #solveSimplex()
     */
    public SimplexAnswer solve() {
        if (isSolved()) {
            return createAnswer();
        }

        solveSimplex();
        return createAnswer();
    }

    /**
     * Создание объекта {@link SimplexAnswer}, возвращающегося в качестве ответа при решении/перевычислении задачи
     * линейного программирования (см. {@link #solve()}, {@link #changeB(double[])}, {@link #addConstraint(double[], Inequality, double)}).
     * В качестве вектора {@link SimplexAnswer#X()} устанавливаются лишь значения основных переменных, без значения
     * заменяющейся переменной, значений переменных дополнительного и искусственого базисов
     */
    private SimplexAnswer createAnswer() {
        double fx = A.get(A.rows - 1, 0);
        if (functionType == MAX) {
            fx = -fx;
        }

        double[] xPrepared = new double[C.length];
        Arrays.fill(xPrepared, 0d);
        for (int i = 0; i < bases.length; i++) {
            xPrepared[bases[i]] = A.get(i, 0);
        }
        int border = dopBasisIndex;
        if (replacingIndex != null) {
            border = replacingIndex;
            for (int i = 1; i < replacingIndex; i++) {
                if (!normalizedX[i - 1]) {
                    xPrepared[i] -= xPrepared[replacingIndex];
                }
            }
        }
        double[] x = Arrays.copyOfRange(xPrepared, 1, border);

        return new SimplexAnswer(x, fx);
    }

    /**
     * Решение задачи линейного программирования двухфазным методом. В процессе решения задачи возможны выбросы следующих
     * классов ошибок (все - лежат в пакете {@link math.simplex.exception}):<br/>
     * <ul>
     *     <li>{@link DifficultSimplexSolveException} - самая неприятная из ошибок. К сожалению, эта ошибка говорит лишь
     *     о том, что в процессе вычисления в первой фазе не удалось вывести из базиса искусственные переменные. Это
     *     связано с тем, что {@link #normalizeBasis(int) алгоритм вывода искусственного переменной из базиса}, который
     *     применяется лишь в случае получения оптимальных значений оценок Жордана-Гаусса, достаточно примитивен (для
     *     текущей реализации). Однако, на практике, такая ситуация (невывод искусственной переменной из базиса)
     *     встречается достаточно редко при решении задачи</li>
     *     <li>{@link IncompatibleSimplexSolveException} - исключение, выбрасываемое в том случае, если система основных
     *     ограничений несовместна. Технически, это означает, что на первой фазе, при получении оптимальных значений оценок
     *     Жордана-Гаусса значение целевой функции (искусственной) {@link #isApproximateValue(double, double, double) не
     *     близко} к нулю (&epsilon = {@link #EPSILON}). В отличие от предыдущего исключения, это исключение всегда
     *     достоверное, на него можно полагаться</li>
     *     <li>{@link UnlimitedFunctionMaximizeException} - исключение, выбрасываемое в том случае, если целевая функция
     *     неограниченно возрастает</li>
     *     <li>{@link UnlimitedFunctionMinimizeException} - исключение, выбрасываемое в том случае, если целевая функция
     *     неограниченно убывает</li>
     * </ul>
     * При выбросе любого из исключений <b>не рекомендуется</b> как-либо вообще взаимодействовать с этим объектом.
     * Изменённую задачу нужно решать в другом объекте, этот объект будет пригоден лишь для сборщика мусора.<br/>
     * Помимо исключений, есть ещё одна неприятность (нерешённая в текущей версии) - зацикливание работы. Это связано с
     * тем, что один и тот же набор перестановок переменных x выступает в качестве базиса. На практике, это случайно редко,
     * однако, если нужна гарантированная детерминированность при решении - создавайте объект и производите его решение в
     * отдельном потоке, с ограничением времени исполнения.
     */
    private void solveSimplex() {
        calculateArtificialBasis();
        C = concatHorizontally(C, DoubleMatrix.zeros(1, B.length));
        recalculateJordanGaussScore();
        recalculatesA(artificialBasisIndex);
        this.solved = true;
    }

    private void recalculateJordanGaussScore() {
        DoubleMatrix JordanGaussScore = JordanGaussScore(C.get(0, bases),
                A.getRange(0, A.rows - 1, 0, A.columns), C);
        A.putRow(A.rows - 1, JordanGaussScore);
    }

    /**
     * Вычисление для первой фазы двухфазного метода
     */
    private void calculateArtificialBasis() {
        bases = IntStream.range(artificialBasisIndex, C_i.length).toArray();
        DoubleMatrix C_B = C_i.get(0, bases);
        DoubleMatrix JorganGaussScore = JordanGaussScore(C_B, A, C_i);
        A = concatVertically(A, JorganGaussScore);

        recalculatesA(C_i.length);
        if (!isApproximateValue(A.get(A.rows - 1, 0), 0d, EPSILON)) {
            throw new IncompatibleSimplexSolveException("The system is incompatible!");
        }

        normalizeBases();
    }

    private DoubleMatrix JordanGaussScore(DoubleMatrix C_basis, DoubleMatrix A, DoubleMatrix C) {
        return C_basis.mmul(A).subi(C);
    }

    /**
     * Нормализация базиса - вывод искусственных переменных из базиса
     */
    private void normalizeBases() {
        for (int i =0; i < bases.length; i++) {
            normalizeBasis(i);
        }
    }

    /**
     * Циклический пересчёт матрицы {@link #A} до тех пор, пока оценки Жордана-Гаусса не будут удовлеторительными;
     * потенциально - бесконечен (см. {@link #solveSimplex()}). Используется в обоих фазах двухфазного метода
     */
    private void recalculatesA(int border) {
        Integer inputIndex;
        Integer outputIndex;

        while (true) {
            inputIndex = firstPositive(A.getColumnRange(A.rows - 1, 0, border));
            if (inputIndex == null) {
                break;
            }

            outputIndex = minThetaRowIndex(inputIndex);
            if (outputIndex == null) {
                if (functionType == MAX) {
                    throw new UnlimitedFunctionMaximizeException("The function can be unlimited maximize!");
                } else {
                    throw new UnlimitedFunctionMinimizeException("The function can be unlimited minimize!");
                }
            }

            recalculateA(inputIndex, outputIndex);
            changeBases(inputIndex, outputIndex);
        }
    }

    private void changeBases(Integer inputIndex, Integer outputIndex) {
        bases[outputIndex] = inputIndex;
    }

    private String matrixPrintString(DoubleMatrix matrix) {
        return matrix.toString("%f", "[", "]", ", ", "\n ");
    }

    private void recalculateA(int inputBasisIndex, int outputBasisIndex) {
        DoubleMatrix outputBasisRow = A.getRow(outputBasisIndex).div(A.get(outputBasisIndex, inputBasisIndex));
        A.putRow(outputBasisIndex, outputBasisRow);

        double k;
        for (int i = 0; i < A.rows; i++) {
            if (i == outputBasisIndex) {
                continue;
            }

            k = -A.get(i, inputBasisIndex);

            DoubleMatrix row = A.getRow(i);
            row.addi(A.getRow(outputBasisIndex).muli(k));
            A.putRow(i, row);
        }
    }

    /**
     * Нахождение индекса входящего базиса (для прямой задачи линейного программирования, применяется в процессе
     * {@link #recalculatesA(int) перевычислении матрицы} {@link #A})
     * @return
     */
    private Integer minThetaRowIndex(int outputBasisColumnIndex) {
        DoubleMatrix P_O = A.getRowRange(0, A.rows - 1, 0);
        DoubleMatrix P_J = A.getRowRange(0, A.rows - 1, outputBasisColumnIndex);

        double minTheta = Double.POSITIVE_INFINITY;
        Integer minIndex = null;

        double theta;
        for(int i = 0; i < A.rows - 1; i++) {
            double a_ij = P_J.get(i);
            double a_i0 = P_O.get(i);

            if (!isApproximateValue(a_ij, 0d, EPSILON) && a_ij > 0) {
                theta = a_i0 / a_ij;
                if (theta < minTheta) {
                    minTheta = theta;
                    minIndex = i;
                }
            }
        }

        return minIndex;
    }

    private Integer firstPositive(DoubleMatrix vector) {
        for (int i = 1; i < vector.length; i++) {
            double value = vector.get(i);
            if (!isApproximateValue(value, 0d, EPSILON) && value > 0) {
                return i;
            }
        }

        return null;
    }

    /**
     * Сравнение чисел с плавающей точкой. Два числа (value и actual) считаются достаточно близкими, если
     * |value - actual| < epsilon
     * @see MathUtil#isApproximateValue(double, double, double)
     */
    private boolean isApproximateValue(double value, double actual, double epsilon) {
        return MathUtil.isApproximateValue(value, actual, epsilon);
    }

    /**
     * Создание нового объекта {@link Simplex} с копированием (полным, глубоким) всех внутренних полей данного объекта.
     * Полезно для распараллеливании вычисления задачи
     */
    public Simplex copy() {
        Simplex copy = new Simplex();
        copy.A = A.dup();
        copy.B = B.dup();
        copy.C = C.dup();
        copy.C_i = C_i.dup();

        copy.bases = Arrays.copyOf(bases, bases.length);
        copy.normalizedX = Arrays.copyOf(normalizedX, normalizedX.length);
        copy.inequalities = Arrays.copyOf(inequalities, inequalities.length);

        copy.functionType = functionType;
        copy.replacingIndex = replacingIndex;
        copy.dopBasisIndex = dopBasisIndex;
        copy.artificialBasisIndex = artificialBasisIndex;
        copy.solved = solved;

        return copy;
    }

    /**
     * Замена значения правой части ограничения с перевычислением результата. Практически тоже самое, что и
     * {@link #changeB(double[])}, но вместо всех правых частей ограничений заменяется лишь одно
     * @param index - индекс значения в векторе {@link #changeB(int, double)}
     * @param bi - новое значение ограничения
     * @see #changeB(double[])
     */
    public SimplexAnswer changeB(int index, double bi) {
        if (!isSolved()) {
            throw new SimplexStateException("The system hasn't yet solved!");
        }

        checkBiIndex(index);

        B.put(index, bi);
        changeB(B);
        recalculateNegativePB();
        return createAnswer();
    }

    private void checkBiIndex(int index) {
        if (index >= B.length || index < 0) {
            throw new SimplexDataException(String.format("Incorrect index %d for vector B with length %d", index,
                    B.length));
        }
    }

    /**
     * Замена значений правой части ограничений с перевычислением результата. Перевычисление происходит
     * <a href=https://kpfu.ru/staff_files/F_1742196799/Andrianova_Repina_Praktikum_system_theory.pdf>по правилам
     * двойственного симплекс-метода</a>. В процессе вызова этого метода (а также метода {@link #changeB(int, double)})
     * возможны выбросы следующих исключений:<br/>
     * <ul>
     *     <li>{@link SimplexDataException} -  исключение, выбрасываемое в том случае, если размерность переданного
     *     массива не соответствует размерности вектора {@link #B} (или для метода {@link #changeB(int, double)} - индекс
     *     не лежит в векторе {@link #B}). "Безопасное" исключение, не портящее внутреннее состояние объекта, поэтому
     *     после исправления данных можно будет снова вызвать этот метод</li>
     *     <li>{@link SimplexStateException} - исключение, выбрасывается в том случае, если задача ещё не решена. В
     *     отличие от остальных исключений, при выбросе этого исключения гарантируется, что после вызова метода
     *     {@link #solve()} и его корректного завершения (<b>в том же самом потоке</b>), это исключение больше не будет
     *     выброшено</li>
     *     <li>{@link IncompatibleSimplexSolveException} - исключение, выбрасываемое в том случае, если новая система
     *     ограничений несовместна. Технически, это означает, что не удалось {@link #recalculateNegativePB()
     *     перевычислить B<sup>-1</sup>P<sub>0</sub>}</li>
     * </ul>
     */
    public SimplexAnswer changeB(double[] B) {
        if (!isSolved()) {
            throw new SimplexStateException("The system hasn't yet solved!");
        }

        checkB(B);
        changeB(new DoubleMatrix(B));
        recalculateNegativePB();
        return createAnswer();
    }

    /**
     * Перевычисление B<sup>-1</sup>P<sub>0</sub> - фактически происходит до тех пор, пока существует переменная < 0 в
     * столбце B<sup>-1</sup>P<sub>0</sub> матрицы {@link #A}
     */
    private void recalculateNegativePB() {
        Integer outputIndex;
        Integer inputIndex;
        while (true) {
            outputIndex = getMaxNegative(A.getRowRange(0, A.rows - 1, 0));
            if (outputIndex == null) {
                break;
            }

            inputIndex = minThetaColumnIndex(outputIndex);
            if (inputIndex == null) {
                throw new IncompatibleSimplexSolveException("The new system is incompatible!");
            }

            recalculateA(inputIndex, outputIndex);
            changeBases(inputIndex, outputIndex);
        }
    }

    /**
     * Поиск индекса входящего базиса; осуществляется при {@link #recalculateNegativePB()
     * перевычислении B<sup>-1</sup>P<sub>0</sub>}
     */
    private Integer minThetaColumnIndex(Integer outputBasisRowIndex) {
        double minTheta = Double.POSITIVE_INFINITY;
        Integer index = null;

        DoubleMatrix P_JordanGaussScore = A.getColumnRange(A.rows - 1, 1, artificialBasisIndex);
        DoubleMatrix P_i = A.getColumnRange(outputBasisRowIndex, 1, artificialBasisIndex);

        double theta;
        for (int j = 1; j < artificialBasisIndex; j++) {
            double p_ij = P_i.get(j - 1);
            double p_jg_j = P_JordanGaussScore.get(j - 1);

            if (!isApproximateValue(p_ij, 0d, EPSILON) && p_ij < 0) {
                theta = p_jg_j / p_ij;
                if (theta < minTheta) {
                    minTheta = theta;
                    index = j;
                }
            }
        }

        return index;
    }

    private Integer getMaxNegative(DoubleMatrix vector) {
        Integer index = null;
        double min = Double.POSITIVE_INFINITY;

        for (int i = 0; i < vector.length; i++) {
            double value = vector.get(i);
            if (!isApproximateValue(value, 0d, EPSILON) && value < 0 && value < min) {
                min = value;
                index = i;
            }
        }

        return index;
    }

    /**
     * Замена вектора {@link #B} и {@link #recalculateB0(DoubleMatrix) пересчёт B<sup>-1</sup>P<sub>0</sub> в матрице}
     * {@link #A}
     */
    private void changeB(DoubleMatrix B) {
        this.B = B;
        recalculateB0(B);
    }

    /**
     * Пересчёт B<sup>-1</sup>P<sub>0</sub> в матрице {@link #A}. Для пересчёта используется искусственный базис
     */
    private void recalculateB0(DoubleMatrix B) {
        DoubleMatrix firstBasis = A.getRange(0, A.rows - 1, artificialBasisIndex, A.columns);
        DoubleMatrix BP0 = firstBasis.mmul(B);
        DoubleMatrix C_bas = C.get(bases);
        double fx = C_bas.dot(BP0);

        A.putColumn(0, concatVertically(BP0, DoubleMatrix.scalar(fx)));
    }

    private void checkB(double[] B) {
        if (this.B.length != B.length) {
            throw new SimplexDataException(
                    String.format("New free constraints' vector (%s) is incompatible with old free constraints' vector "+
                                    "(%s)", Arrays.toString(B), matrixPrintString(this.B)));
        }
    }

    /**
     * Добавление нового ограничения с перевычислением симплекса. Перевычисление происходит
     * <a href=https://kpfu.ru/staff_files/F_1742196799/Andrianova_Repina_Praktikum_system_theory.pdf>по правилам
     * двойственного симплекс-метода</a> (с использованием искусственного базиса). В процессе вызова этого метода
     * возможны выбросы следующих исключений:<br/>
     * <ul>
     *     <li>{@link SimplexStateException} - исключение, выбрасывается в том случае, если задача ещё не решена. В
     *     отличие от остальных исключений, при выбросе этого исключения гарантируется, что после вызова метода
     *     {@link #solve()} и его корректного завершения (<b>в том же самом потоке</b>), это исключение больше не будет
     *     выброшено</li>
     *     <li>{@link SimplexDataException} - исключение, выбрасывается в том случае, если размерность переданного массива
     *     не совпадает с размерность переменных x (изначально определённых пользователем, количество вычисляется как длина
     *     массива {@link #normalizedX}). "Безопасное" исключение, не портящее внутреннее состояние объекта, поэтому
     *     после исправления данных можно будет снова вызвать этот метод</li>
     *     <li>{@link IncompatibleSimplexSolveException} - исключение, выбрасываемое в том случае, если новая система
     *     ограчений несовместна. Технически, происходит выбрасывание исключения {@link DifficultSimplexSolveException}
     *     как невозможность {@link #normalizeBasis(int) вывести искусственную переменную из базиса}, для текущей
     *     реализации,этот вызов будет отловлен и вместо не выброшено исключение {@link IncompatibleSimplexSolveException}.</li>
     * </ul>
     * Добавление нового ограничения - достаточно затратная операция. В процессе добавления происходят следующие изменения:
     * <ol>
     *     <li>добавляется знак неравенства в конец массива {@link #inequalities}</li>
     *     <li>добавляется bi в конец вектора {@link #B}</li>
     *     <li>добавляется нулевой столбец (a<sub>d</sub>) для дополнительного базиса в матрицу {@link #A}</li>
     *     <li>смещается индекс {@link #artificialBasisIndex}</li>
     *     <li>добавляется нулевой столбец (a<sub>a</sub>) для искусственного базиса</li>
     *     <li>добавляется строка (ai) в матрицу {@link #A} в конец всех базисных строк (до оценки Жордана-Гаусса). Все
     *     значения для {@link #replacingX() замещающего столбца} (если есть), {@link #equalization() дополнительного} и
     *     {@link #artificialBasis() искусственного базиса} заполняются также, как заполнялись бы при {@link #canonize()
     *     канонизации}</li>
     * </ol>
     * Таким образом, матрица {@link #A} модернизируется следующим образом:<br/>
     * <img src="../../../resources/math/simplex/img/A_new.png"/>
     */
    public SimplexAnswer addConstraint(double[] ai, Inequality inequality, double bi) {
        if (!isSolved()) {
            throw new SimplexStateException("The system hasn't yet solved!");
        }

        checkAi(ai);
        addToInequalities(inequality);
        addToB(bi);
        addToA(ai, inequality, bi);
        addNewBasis();
        formingPseudoBasis(A.rows - 2);
        try {
            normalizeBasis(bases.length - 1);
        } catch (DifficultSimplexSolveException e) {
            throw new IncompatibleSimplexSolveException("The system is incompatible!");
        }
        recalculateNegativePB();

        return createAnswer();
    }

    /**
     * Алгоритм выведения искусственной переменной из базиса. На вход подаётся индекс массива {@link #bases}, значение
     * которого является индексом базиса i (переменной x<sub>i</sub>), которую нужно вывести из базиса. Если переменная
     * не искусственная, то ничего не происходит, в противном случае - осуществляется
     * {@link #getBasisForNormalisation(int, DoubleMatrix, Set) поиск базиса, на который можно было по заменить (вывести
     * через него) искусственный базис} . Если существует такой базис - происходит замена базиса, и алгоритм заканчивается,
     * в противном случае - выкидывается исключение {@link DifficultSimplexSolveException}
     */
    private void normalizeBasis(int outputBasis) {
        int outputBasisIndex = bases[outputBasis];
        if (outputBasisIndex < artificialBasisIndex) {
            return;
        }

        Set<Integer> bases = IntStream.of(this.bases)
                .boxed().collect(Collectors.toSet());

        Integer inputBasis =
                getBasisForNormalisation(outputBasis, A.getColumnRange(A.rows - 1, 1, artificialBasisIndex), bases);

        if (inputBasis == null) {
            throw new DifficultSimplexSolveException("The system is difficult to solve. " +
                    "It is necessary to express an artificial basis as a linear combination " +
                    "of non-artificial bases");
        }

        recalculateA(inputBasis, outputBasis);
        changeBases(inputBasis, outputBasis);
    }

    /**
     * Поиск базиса, на который можно было по заменить (вывести через него) искусственный базис. Базис должен удовлетворять
     * следующим условиям:<br/>
     * <ol>
     *     <li>Не быть искусственной переменной</li>
     *     <li>Не лежать в базисе</li>
     *     <li>Оценка Жордана-Гаусса для этого базиса должна быть &ge; 0 (для сохранения оптимальности)</li>
     *     <li>Значение вектора (a<sub>ij</sub>, i - индекс заменяемого базиса, j - индекс замещающаего базиса) не должно
     *     быть {@link #isApproximateValue(double, double, double) сравнимым} с 0</li>
     * </ol>
     * Если не находится базис, удовлетворяющий всем этим условиям - возвращается null
     */
    private Integer getBasisForNormalisation(int outputBasis, DoubleMatrix vector, Set<Integer> bases) {
        for (int i = 0; i < vector.length; i++) {
            int basisIndex = i + 1;
            double basisValue = A.get(outputBasis, basisIndex);
            if (bases.contains(basisIndex) || isApproximateValue(basisValue, 0d, EPSILON)) {
                continue;
            }

            double value = vector.get(i);
            if (isApproximateValue(value, 0d, EPSILON) && value >= 0) {
                return basisIndex;
            }
        }

        return null;
    }

    /**
     * Формирование псевдобазиса (т.е. базиса, в который входит искусственная переменная) при добавлении нового ограничения
     */
    private void formingPseudoBasis(int pseudoBasisRow) {
        double k;
        int basisIndex;
        DoubleMatrix putRow = A.getRow(pseudoBasisRow);
        int i = 0;
        for (; i < bases.length - 1; i++) {
            basisIndex = bases[i];
            k = A.get(pseudoBasisRow, basisIndex);
            DoubleMatrix subRow = A.getRow(i).muli(k);
            putRow.subi(subRow);
        }

        A.putRow(pseudoBasisRow, putRow);
        recalculateA(bases[i], pseudoBasisRow);
    }

    private void addNewBasis() {
        int index = bases.length;
        bases = Arrays.copyOf(bases, index + 1);
        bases[index] = C.length - 1;
    }

    private void addToA(double[] ai, Inequality inequality, double bi) {
        double replacingXValue = 0d;
        double replacingCValue = 0d;

        DoubleStream.Builder values = DoubleStream.builder().add(bi);
        for (int i = 0; i < ai.length; i++) {
            values.add(ai[i]);
            if (!normalizedX[i]) {
                replacingXValue -= ai[i];
                replacingCValue -= C.get(i + 1);
            }
        }

        if (replacingIndex != null) {
            values.add(replacingXValue);
            C.put(replacingIndex, C.get(replacingIndex) - replacingCValue);
        }

        DoubleMatrix dopBasisRow = concatHorizontally(DoubleMatrix.zeros(1, A.rows - 1),
                DoubleMatrix.scalar(getInequalityCoeff(inequality)));

        DoubleMatrix artificialBasisRow = concatHorizontally(DoubleMatrix.zeros(1, A.rows - 1),
                DoubleMatrix.scalar(1));

        addNewDopAndArtificialCi();

        DoubleMatrix A_withoutArtificial = A.getRange(0, A.rows, 0, artificialBasisIndex);
        DoubleMatrix A_i = A.getRange(0, A.rows, artificialBasisIndex, A.columns);

        A_withoutArtificial = concatHorizontally(A_withoutArtificial, DoubleMatrix.zeros(A.rows));
        A_i = concatHorizontally(A_i, DoubleMatrix.zeros(A.rows));

        A = concatHorizontally(A_withoutArtificial, A_i);

        double[] rowValues = values.build().toArray();
        DoubleMatrix addRow = concatHorizontally(new DoubleMatrix(1, rowValues.length, rowValues), dopBasisRow);
        addRow = concatHorizontally(addRow, artificialBasisRow);

        DoubleMatrix A_withoutJordanGaussScore = A.getRange(0, A.rows - 1, 0, A.columns);
        DoubleMatrix JordanGaussScore = A.getRow(A.rows - 1);
        
        A = concatVertically(A_withoutJordanGaussScore, addRow);
        A = concatVertically(A, JordanGaussScore);

        artificialBasisIndex++;
    }

    private void addNewDopAndArtificialCi() {
        DoubleMatrix C_withoutArtificial = C.getColumnRange(0, 0, artificialBasisIndex);
        DoubleMatrix C_i = C.getColumnRange(0, artificialBasisIndex, C.columns);

        C_withoutArtificial = concatHorizontally(C_withoutArtificial, DoubleMatrix.scalar(0));
        C_i = concatHorizontally(C_i, DoubleMatrix.scalar(0));

        C = concatHorizontally(C_withoutArtificial, C_i);
    }

    private void addToB(double bi) {
        B = concatVertically(B, DoubleMatrix.scalar(bi));
    }

    private void addToInequalities(Inequality inequality) {
        int index = inequalities.length;
        inequalities = Arrays.copyOf(inequalities, index + 1);
        inequalities[index] = inequality;
    }

    private void checkAi(double[] ai) {
        if (ai == null || ai.length != normalizedX.length) {
            throw new SimplexDataException(
                    String.format("New constraint matrix vector (%s) is incompatible with matrix A (%s)!",
                            Arrays.toString(ai), matrixPrintString(A)));
        }
    }

    public boolean isSolved() {
        return solved;
    }

    /**
     * Класс Builder для построения {@link Simplex}. Реализован для удобства инициализации данных.
     */
    public static class Builder implements Serializable {
        private double[][] A;
        private double[] B;
        private double[] C;
        private FunctionType functionType;
        private Inequality[] inequalities;
        private boolean[] normalizedX;

        private final static long serialVersionUID = 6036275088181504743L;

        public Builder() {}

        public double[][] getA() {
            return A;
        }

        public double[] getB() {
            return B;
        }

        public double[] getC() {
            return C;
        }

        public FunctionType getFunctionType() {
            return functionType;
        }

        public Inequality[] getInequalities() {
            return inequalities;
        }

        public boolean[] getNormalizedX() {
            return normalizedX;
        }

        public Builder setA(double[][] A) {
            this.A = A;
            return this;
        }

        public Builder setB(double[] B) {
            this.B = B;
            return this;
        }

        public Builder setC(double[] C) {
            this.C = C;
            return this;
        }

        public Builder setFunctionType(FunctionType functionType) {
            this.functionType = functionType;
            return this;
        }

        public Builder setInequalities(Inequality[] inequalities) {
            this.inequalities = inequalities;
            return this;
        }

        public Builder setNormalizedX(boolean[] normalizedX) {
            this.normalizedX = normalizedX;
            return this;
        }

        /**
         * Создание объекта {@link Simplex}. До создания объекта данные проходят валидацию (на null (для обязательных
         * данных) и соответствие размерностей), в случае наличия ошибки выбрасывается исключение
         * {@link SimplexDataException}. Для успешного создания объекта необходимо {@link #setA(double[][]) установить
         * матрицу ограничений A}, {@link #setB(double[]) вектор правых (свободных) частей ограничений} и {@link
         * #setC(double[]) вектор свободных коэффициентов целевой функции}. Остальные поля будут заполнены по умолчанию -
         * {@link #defaultFunctionType()}, {@link #defaultInequalities(int)}, {@link #defaultNormalizedX(int)}. Гарантируется,
         * что данные (вектора и массивы) будут скопированы (полностью, глубоко) для невозможности испортить значение по
         * внешней ссылке
         * @see #Simplex(double[][], double[], double[], FunctionType, Inequality[], boolean[])
         */
        public Simplex build() {
            validateMatrices();
            initializeNullValues();
            checkArrays();
            copyArrays();
            return new Simplex(A, B, C, functionType, inequalities, normalizedX);
        }

        private void copyArrays() {
            double[][] A = new double[this.A.length][];
            for (int i = 0; i < A.length; i++) {
                A[i] = Arrays.copyOf(this.A[i], this.A[i].length);
            }

            this.A = A;

            B = Arrays.copyOf(B, B.length);
            C = Arrays.copyOf(C, C.length);
            inequalities = Arrays.copyOf(inequalities, inequalities.length);
            normalizedX = Arrays.copyOf(normalizedX, normalizedX.length);
        }

        private void checkArrays() {
            if (inequalities.length != B.length) {
                throw new SimplexDataException(
                        String.format("Inequalities (%s) is incompatible with vector B (%s)",
                                Arrays.toString(inequalities), Arrays.toString(B)));
            }

            if (normalizedX.length != C.length) {
                throw new SimplexDataException(
                        String.format("NormalizedX array (%s) is incompatible with vector C (%s)",
                                Arrays.toString(normalizedX), Arrays.toString(C)));
            }
        }

        private void initializeNullValues() {
            initializeFunctionType();
            initializeInequalities();
            initializeNormalizedX();
        }

        private void initializeNormalizedX() {
            if (normalizedX == null) {
                normalizedX = defaultNormalizedX(C.length);
            }
        }

        private void initializeInequalities() {
            if (inequalities == null) {
                inequalities = defaultInequalities(B.length);
            }
        }

        private void initializeFunctionType() {
            if (functionType == null) {
                functionType = defaultFunctionType();
            }
        }

        private void validateMatrices() {
            if (A == null || A.length == 0) {
                throw new SimplexDataException("Empty or null coefficients' matrix (A)!");
            }

            if (B == null || B.length == 0) {
                throw new SimplexDataException("Empty or null free constraints' vector (B)!");
            }

            if (C == null || C.length == 0) {
                throw new SimplexDataException("Empty or null objective function's vector (C)!");
            }

            int rows = B.length;
            if (A.length != rows) {
                throw new SimplexDataException(
                        String.format("Matrix A (%s) is incompatible with matrix B (%s)",Arrays.deepToString(A),
                                Arrays.toString(B)));
            }

            int columns = C.length;
            if (!isRectangleMatrix(A, columns)) {
                throw new SimplexDataException(
                        String.format("Matrix A (%s) isn't rectangle matrix and is incompatible with vector C (%s)",
                                Arrays.deepToString(A), Arrays.toString(B)));
            }
        }

        private boolean isRectangleMatrix(double[][] matrix, int columns) {
            for (double[] row: matrix) {
                if (row.length != columns) {
                    return false;
                }
            }

            return true;
        }
    }

    public static Inequality[] defaultInequalities(int size) {
        Inequality[] inequalities = new Inequality[size];
        Arrays.fill(inequalities, EQ);
        return inequalities;
    }

    public static boolean[] defaultNormalizedX(int size) {
        boolean[] normalizedX = new boolean[size];
        Arrays.fill(normalizedX, true);
        return normalizedX;
    }

    public static FunctionType defaultFunctionType() {
        return MIN;
    }

}
