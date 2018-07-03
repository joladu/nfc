package com.st.st25nfc.type5.st25dv;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by lenovo on 2018/4/16.
 */

public class CommondSendResponseEntity {

    private boolean isRes;
    private String timeStamp;
    private String title;
    private String commandStr;
    private String resolveStr;

    public boolean getIsRes() {
        return isRes;
    }

    public void setIsRes(boolean res) {
        isRes = res;
    }

    public String getResolveStr() {
        return resolveStr;
    }

    public void setResolveStr(String resolveStr) {
        this.resolveStr = resolveStr;
    }

    public CommondSendResponseEntity() {
        timeStamp = new SimpleDateFormat("HH:mm:ss:SSS", Locale.getDefault()).format(new Date());
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getCommandStr() {
        return commandStr;
    }

    public void setCommandStr(String commandStr) {
        this.commandStr = commandStr;
    }
}
