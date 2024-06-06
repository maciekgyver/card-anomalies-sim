package psd;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class ValueAnomalyDetector extends KeyedProcessFunction<Long, Transaction, Alert> {

    private static final long serialVersionUID = 1L;

    private transient ValueState<Long> numberOfTransactions;
    private transient ValueState<Double> meanState;
    private transient ValueState<Double> varianceSumState;

    @Override
    public void open(Configuration parameters) {
        ValueStateDescriptor<Long> numFlagDescriptor = new ValueStateDescriptor<>(
                "numOfTransactions",
                Types.LONG);
        numberOfTransactions = getRuntimeContext().getState(numFlagDescriptor);

        ValueStateDescriptor<Double> meanFlagDescriptor = new ValueStateDescriptor<>(
                "mean",
                Types.DOUBLE);
        meanState = getRuntimeContext().getState(meanFlagDescriptor);

        ValueStateDescriptor<Double> varianceSumFlagDescriptor = new ValueStateDescriptor<>(
                "varianceSum",
                Types.DOUBLE);
        varianceSumState = getRuntimeContext().getState(varianceSumFlagDescriptor);
    }

    // Welford's algorithm
    private void updateStatistics(double newValue) throws IOException {
        numberOfTransactions.update(numberOfTransactions.value() + 1);
        double old_delta = newValue - meanState.value();
        meanState.update(meanState.value() + old_delta / numberOfTransactions.value());
        double new_delta = newValue - meanState.value();
        varianceSumState.update(varianceSumState.value() + old_delta * new_delta);
    }

    private double variance() throws IOException {
        return numberOfTransactions.value() > 1 ? varianceSumState.value() / (numberOfTransactions.value() - 1) : Double.POSITIVE_INFINITY;
    }

    private double stddev() throws IOException {
        return Math.sqrt(variance());
    }

    private double getZScore(double new_value) throws IOException {
        double std = stddev();
        return std > 0 ? (new_value - meanState.value()) / std : 0;
    }

    @Override
    public void processElement(
            Transaction transaction,
            Context context,
            Collector<Alert> collector) throws Exception {

        double zScore = getZScore(transaction.getValue());
        if (zScore > 3 || zScore < -3) {
            collector.collect(new Alert(transaction.getTransactionId(), transaction.getCardNumber(), "Value Anomaly Detected", transaction.getValue(), zScore));
        }
        updateStatistics(transaction.getValue());
    }
}
