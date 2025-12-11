package com.luxshare.home.bean;

/**
 * 实现用于单选
 */
public class SingleChoiceItem {
    private String nicky;
    private String name;
    private boolean isSelected = false;

    public String getNicky() {
        return nicky;
    }

    public void setNicky(String nicky) {
        this.nicky = nicky;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return isSelected;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }
}
