package com.bang.apns.application;

import com.eatthepath.pushy.apns.ApnsClient;
import lombok.Data;

@Data
public class APNSClientInfo {
    private ApnsClient apnsClient;
    private String ip;
    private APNSStatus status;

    public APNSClientInfo(ApnsClient apnsClient, String ip, APNSStatus status){
        this.apnsClient = apnsClient;
        this.ip = ip;
        this.status = status;
    }
}
