package com.example.wificonnect;

import com.google.gson.annotations.Expose;

import java.io.Serializable;

public class User implements Serializable {

    @Expose
    public int id;
    @Expose
    public String name;
    @Expose
    public int status;

}
