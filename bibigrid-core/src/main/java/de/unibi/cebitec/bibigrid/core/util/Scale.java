package de.unibi.cebitec.bibigrid.core.util;

public enum Scale {
    up("scale-up"), down("scale-down");

    private String name;

    private Scale(String name){
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}