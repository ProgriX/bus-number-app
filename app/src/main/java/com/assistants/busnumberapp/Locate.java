package com.assistants.busnumberapp;

public abstract class Locate {

    public abstract String getLocateSign();
    public abstract String getBus(BusSounding.Bus bus);

    public abstract String getGuideMsg(int guideMsg);
}
