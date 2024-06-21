package com.gerow.test.jmeter.functions;

import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;

import java.util.Collection;
import java.util.List;

public class UidToShowId extends AbstractFunction {
    private static final List<String> desc = List.of("uid");
    private static final String KEY = "__uidIdToShowId";
    private CompoundVariable uid;

    public static final char[] NUMBER_CODE_ARR = "132485769".toCharArray();
    public static final int PRIME_1 = 3;
    public static final int PRIME_2 = 5;

    @Override
    public List<String> getArgumentDesc() {
        return desc;
    }

    @Override
    public String getReferenceKey() {
        return KEY;
    }

    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkParameterCount(parameters, 1);
        uid = parameters.iterator().next();
    }

    public static Long getUserShowId(int uid) {
        uid = uid * PRIME_1;
        int size = 9;
        byte[] codeChar = new byte[size];
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < size; i++) {
            codeChar[i] = (byte) (uid % NUMBER_CODE_ARR.length);
            codeChar[i] = (byte) ((codeChar[i] + codeChar[0] * (i + 10)) % NUMBER_CODE_ARR.length);
            uid = uid / NUMBER_CODE_ARR.length;
        }
        for (int i = 1; i < size + 1; i++) {
            int index = (i * PRIME_2) % size;
            code.append(NUMBER_CODE_ARR[codeChar[index]]);
        }
        return Long.valueOf(code.toString());
    }

    @Override
    public String execute(SampleResult previousResult, Sampler currentSampler) throws InvalidVariableException {
        int showIdValue = Integer.parseInt(uid.execute().trim());
        Long showID = getUserShowId(showIdValue);
        return String.valueOf(showID);
    }
}
