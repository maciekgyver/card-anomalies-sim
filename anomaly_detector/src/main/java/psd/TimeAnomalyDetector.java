package psd;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class TimeAnomalyDetector extends KeyedProcessFunction<Long, Transaction, Alert> {

    private static final long serialVersionUID = 1L;

    private transient ValueState<Long> numberOfTransactions;
    private transient ValueState<Long> lastTransactionTimeState;
    private transient ValueState<Double> meanState;
    private transient ValueState<Double> varianceSumState;

    @Override
    public void open(Configuration parameters) throws Exception {
        ValueStateDescriptor<Long> countDescriptor = new ValueStateDescriptor<>("numberOfTransactions", Long.class);
        ValueStateDescriptor<Double> meanDescriptor = new ValueStateDescriptor<>("mean", Double.class);
        ValueStateDescriptor<Double> stdDevDescriptor = new ValueStateDescriptor<>("varianceSum", Double.class);
        ValueStateDescriptor<Long> lastTransactionTimeDescriptor = new ValueStateDescriptor<>("lastTransactionTime", Long.class);

        numberOfTransactions = getRuntimeContext().getState(countDescriptor);
        meanState = getRuntimeContext().getState(meanDescriptor);
        varianceSumState = getRuntimeContext().getState(stdDevDescriptor);
        lastTransactionTimeState = getRuntimeContext().getState(lastTransactionTimeDescriptor);
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
        if (meanState.value() == null) {
            meanState.update(0.0);
            numberOfTransactions.update(0L);
            varianceSumState.update(0.0);
            return 0;
        }
        double std = stddev();
        return std > 0 ? (new_value - meanState.value()) / std : 0;
    }

    @Override
    public void processElement(
            Transaction transaction,
            Context context,
            Collector<Alert> collector) throws Exception {
        if (lastTransactionTimeState.value() != null) {
            double zScore = getZScore(transaction.getValue());
            if (zScore > 3 || zScore < -3 && numberOfTransactions.value() > 10) {
                collector.collect(new Alert(transaction.getTransactionId(), transaction.getCardNumber(), "Time Anomaly Detected", transaction.getValue(), zScore));
            }
            updateStatistics(transaction.getTimestamp() - lastTransactionTimeState.value());
            lastTransactionTimeState.update(transaction.getTimestamp());
        }
        else {
            lastTransactionTimeState.update(transaction.getTimestamp());
        }
    }
}
