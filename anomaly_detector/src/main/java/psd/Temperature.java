package psd;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

public class Temperature {
    @JsonProperty("termometer_id")
    private Integer termometerId;
    private String datetime;
    private Float value;

    public void setTermometerId(Integer termometerId) {
        this.termometerId = termometerId;
    }

    public void setDatetime(String timestamp) {
        this.datetime = timestamp;
    }

    public void setValue(Float temperature) {
        this.value = temperature;
    }

    @Override
    public String toString() {
        return "Temperature{" +
                "termometerId=" + termometerId +
                ", datetime=" + datetime +
                ", value=" + value +
                '}';
    }

    public Integer getTermometerId() {
        return termometerId;
    }

    public String getDatetime() {
        return datetime;
    }

    public Float getValue() {
        return value;
    }
}
