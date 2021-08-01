package math.simplex.exception;

/**
 * This exception guarantees that the original problem statement was unsolvable - the system of basic restrictions was
 * incompatible
 */
public class IncompatibleSimplexSolveException extends SimplexSolveException {
    public IncompatibleSimplexSolveException() {
        super();
    }

    public IncompatibleSimplexSolveException(String s) {
        super(s);
    }
}
