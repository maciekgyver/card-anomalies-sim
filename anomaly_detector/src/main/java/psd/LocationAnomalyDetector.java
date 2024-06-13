package psd;

import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;

import java.io.IOException;

public class LocationAnomalyDetector extends KeyedProcessFunction<Long, Transaction, Alert> {

    private static final long serialVersionUID = 1L;

    private transient ValueState<Long> numberOfTransactions;
    private transient ValueState<Double> meanLatState;
    private transient ValueState<Double> varianceSumLatState;
    private transient ValueState<Double> meanLongState;
    private transient ValueState<Double> varianceSumLongState;

    @Override
    public void open(Configuration parameters) throws Exception {
        ValueStateDescriptor<Long> countDescriptor = new ValueStateDescriptor<>("numberOfTransactions", Long.class);;
        ValueStateDescriptor<Double> meanLatDescriptor = new ValueStateDescriptor<>("meanLat", Double.class);
        ValueStateDescriptor<Double> stdDevLatDescriptor = new ValueStateDescriptor<>("varianceSumLat", Double.class);

        ValueStateDescriptor<Double> meanLongDescriptor = new ValueStateDescriptor<>("meanLong", Double.class);
        ValueStateDescriptor<Double> stdDevLongDescriptor = new ValueStateDescriptor<>("varianceSumLong", Double.class);

        numberOfTransactions = getRuntimeContext().getState(countDescriptor);
        meanLatState = getRuntimeContext().getState(meanLatDescriptor);
        varianceSumLatState = getRuntimeContext().getState(stdDevLatDescriptor);

        meanLongState = getRuntimeContext().getState(meanLatDescriptor);
        varianceSumLongState = getRuntimeContext().getState(stdDevLatDescriptor);
    }

    // Welford's algorithm
    private void updateStatistics(double newValue) throws IOException {
        numberOfTransactions.update(numberOfTransactions.value() + 1);
        double oldLatDelta = newValue - meanLatState.value();
        meanLatState.update(meanLatState.value() + oldLatDelta / numberOfTransactions.value());
        double newLatDelta = newValue - meanLatState.value();
        varianceSumLatState.update(varianceSumLatState.value() + oldLatDelta * newLatDelta);

        double oldLongDelta = newValue - meanLongState.value();
        meanLongState.update(meanLongState.value() + oldLongDelta / numberOfTransactions.value());
        double newLongDelta = newValue - meanLongState.value();
        varianceSumLongState.update(varianceSumLongState.value() + oldLongDelta * newLongDelta);
    }

    private double variance(Long n, Double varSum) {
        return n > 1 ? varSum / (n - 1) : Double.POSITIVE_INFINITY;
    }

    private double stddev(Long n, Double varSum) throws IOException {
        return Math.sqrt(variance(n, varSum));
    }

    private double getLatZScore(double new_value) throws IOException {
        if (meanLatState.value() == null) {
            meanLatState.update(0.0);
            numberOfTransactions.update(0L);
            varianceSumLatState.update(0.0);
            return 0;
        }
        double std = stddev(numberOfTransactions.value(), varianceSumLatState.value());
        return std > 0 ? (new_value - meanLatState.value()) / std : 0;
    }

    private double getLongZScore(double new_value) throws IOException {
        if (meanLongState.value() == null) {
            meanLongState.update(0.0);
            numberOfTransactions.update(0L);
            varianceSumLongState.update(0.0);
            return 0;
        }
        double std = stddev(numberOfTransactions.value(), varianceSumLongState.value());
        return std > 0 ? (new_value - meanLongState.value()) / std : 0;
    }

    @Override
    public void processElement(
            Transaction transaction,
            Context context,
            Collector<Alert> collector) throws Exception {

        double latZScore = getLatZScore(transaction.getValue());
        if ((latZScore > 3 || latZScore < -3)  && numberOfTransactions.value() > 10) {
            collector.collect(new Alert(transaction.getTransactionId(), transaction.getCardNumber(), "Location Latitude Anomaly Detected", transaction.getValue(), latZScore));
        }
        double longZScore = getLongZScore(transaction.getValue());
        if ((longZScore > 3 || longZScore < -3) && numberOfTransactions.value() > 10) {
            collector.collect(new Alert(transaction.getTransactionId(), transaction.getCardNumber(), "Location Longitude Anomaly Detected", transaction.getValue(), longZScore));
        }
        updateStatistics(transaction.getValue());
    }
}
