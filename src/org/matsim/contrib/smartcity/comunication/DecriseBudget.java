package org.matsim.contrib.smartcity.comunication;

public class DecriseBudget extends ComunicationMessage {

    private int amount;
    private String reason;

    public DecriseBudget(ComunicationEntity sender, int amount) {
        super(sender);
        this.amount = amount;
    }

    public DecriseBudget(ComunicationEntity sender, int amount, String reason) {
        this(sender, amount);
        this.reason = reason;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
