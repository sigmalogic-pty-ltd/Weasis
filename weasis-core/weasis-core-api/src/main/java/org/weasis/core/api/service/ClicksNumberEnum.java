package org.weasis.core.api.service;

public enum ClicksNumberEnum {
    SINGLE(1), DOUBLE(2), TRIPLE(3);

    private Integer clicksNumber;

    ClicksNumberEnum(Integer clicksNumber) {
        setClicksNumber(clicksNumber);
    }

    public Integer getClicksNumber() {
        return clicksNumber;
    }

    public void setClicksNumber(Integer clicksNumber) {
        this.clicksNumber = clicksNumber;
    }
}
