package psd;

import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.annotation.JsonProperty;

public class Transaction {
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("user_id")
    private String userId;
    @JsonProperty("card_num")
    private Long cardNumber;
    @JsonProperty("value")
    private Double value;
    @JsonProperty("timestamp")
    private Long timestamp;
    @JsonProperty("location")
    private Location location;
    @JsonProperty("balance")
    private Double balance;

    public Double getValue() {
        return value;
    }

    public Long getCardNumber() {
        return cardNumber;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String toString() {
        return "Transaction{" +
                "transactionId='" + transactionId + '\'' +
                ", userId='" + userId + '\'' +
                ", cardNumber=" + cardNumber +
                ", value=" + value +
                ", timestamp=" + timestamp +
                ", location=" + location +
                ", balance=" + balance +
                '}';
    }
}
