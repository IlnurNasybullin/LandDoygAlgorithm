package math.simplex.exception;

/**
 * This exception is NOT guarantee that the original problem was unsolvable. Unfortunately, the tasks are existed that
 * difficult to solve algorithmically
 */
public class DifficultSimplexSolveException extends SimplexSolveException {
    public DifficultSimplexSolveException() {
        super();
    }

    public DifficultSimplexSolveException(String s) {
        super(s);
    }
}
