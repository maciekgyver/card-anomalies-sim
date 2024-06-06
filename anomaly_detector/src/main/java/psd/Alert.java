package psd;

public class Alert {
    private String transactionId;
    private Long cardNumber;
    private String message;
    private Double value;
    private Double zScore;

    public Alert(String transactionId, Long cardNumber, String message, Double value, Double zScore) {
        this.transactionId = transactionId;
        this.cardNumber = cardNumber;
        this.message = message;
        this.value = value;
        this.zScore = zScore;
    }

    public String toString() {
        return "Alert{" +
                "transactionId='" + transactionId + '\'' +
                ", cardNumber=" + cardNumber +
                ", message='" + message + '\'' +
                ", value=" + value +
                ", zScore=" + zScore +
                '}';
    }
}
