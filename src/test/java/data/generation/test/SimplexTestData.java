package data.generation.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import math.simplex.Constraint;
import math.simplex.Simplex;
import math.simplex.SimplexAnswer;

import java.io.Serializable;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class SimplexTestData implements Serializable {

    private Simplex.Builder simpleData;
    private SimplexAnswer simplexAnswer;

    private SensitivityAnalysis analysisType;

    private double[] changeB;
    private Constraint addConstraint;
    private SimplexAnswer analysisAnswer;

    private Class<? extends Exception> exceptionClass;
    private String exceptionMessage;

    public Simplex.Builder getSimpleData() {
        return simpleData;
    }

    public void setSimpleData(Simplex.Builder simpleData) {
        this.simpleData = simpleData;
    }

    public SimplexAnswer getSimplexAnswer() {
        return simplexAnswer;
    }

    public void setSimplexAnswer(SimplexAnswer simplexAnswer) {
        this.simplexAnswer = simplexAnswer;
    }

    public SensitivityAnalysis getAnalysisType() {
        return analysisType;
    }

    public void setAnalysisType(SensitivityAnalysis analysisType) {
        this.analysisType = analysisType;
    }

    public double[] getChangeB() {
        return changeB;
    }

    public void setChangeB(double[] changeB) {
        this.changeB = changeB;
    }

    public SimplexAnswer getAnalysisAnswer() {
        return analysisAnswer;
    }

    public void setAnalysisAnswer(SimplexAnswer analysisAnswer) {
        this.analysisAnswer = analysisAnswer;
    }

    public Class<? extends Exception> getExceptionClass() {
        return exceptionClass;
    }

    public void setExceptionClass(Class<? extends Exception> exceptionClass) {
        this.exceptionClass = exceptionClass;
    }

    public String getExceptionMessage() {
        return exceptionMessage;
    }

    public void setExceptionMessage(String exceptionMessage) {
        this.exceptionMessage = exceptionMessage;
    }

    public Constraint getAddConstraint() {
        return addConstraint;
    }

    public void setAddConstraint(Constraint addConstraint) {
        this.addConstraint = addConstraint;
    }
}
