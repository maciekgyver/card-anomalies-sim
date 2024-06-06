package psd;

public class Transaction {
    private String transactionId;
    private String userId;
    private Long cardNumber;
    private Double value;
    private Long timestamp;
    private Location location;
    private Double balance;

    public Transaction(String transactionId, String userId, Long cardNumber, Double value, Long timestamp, Location location, Double balance) {
        this.transactionId = transactionId;
        this.userId = userId;
        this.cardNumber = cardNumber;
        this.value = value;
        this.timestamp = timestamp;
        this.location = location;
        this.balance = balance;
    }

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
