package com.bang.apns.application;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.concurrent.PushNotificationFuture;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLException;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.ExecutionException;


/***
 * APNS DNS 쿼리 후 알람 발생시마다 DNS쿼리로 가져온 IP로 APNS Notification을 보냄
 */

@Component
@Slf4j
public class APNSConfig {

    @Value("${apns.ip}")
    public String apnsIp;

    @Value("${apns.length}")
    public String apnsSplitLength;

    @Value("${apns.file-path")
    public String filePath;

    @Value("${apns.password")
    public String apnsPassword;

    public void connect() {

        try {

            InetAddress dnsresult[] = InetAddress.getAllByName(apnsIp);
            String[] ipAddress = new String[dnsresult.length];

            for (int i=0; i<dnsresult.length; i++) {
                ipAddress[i] = String.valueOf(StringUtils.substring(String.valueOf(dnsresult[i]),Integer.parseInt(apnsSplitLength)));
                ApnsClient apnsClient = new ApnsClientBuilder()
                        .setApnsServer(ipAddress[i],443)
                        .setClientCredentials(new File(filePath), apnsPassword)
                        .build();

                APNSClientInfo apnsClientInfo = new APNSClientInfo(apnsClient, ipAddress[i], APNSStatus.ENABLED);

                APNSConnections.apnsClientList.add(apnsClientInfo);

                log.debug("APNS Connection : {}", ipAddress[i]);
            }




        }catch (UnknownHostException e) {
            log.error("UnknownHostException Error : {}",e.getMessage());
        } catch (SSLException e) {
            log.error("SSLException Error : {}",e.getMessage());
        } catch (IOException e) {
            log.error("IOException Error : {}",e.getMessage());
        }

    }

    /***
     * 최초 MESSAGE보낼 때 apnsClientList에 ApnsClient 객체 넣어줌 그리고 상태값 변경하여 해당 상태값에따라 message push
     * @param token
     * @param topic
     * @param payload
     */
    public void sendAPNSNotification(String token, String topic, String payload){


        if(APNSConnections.apnsClientList.size() == 0){
            connect();
            log.info("connected APNS server");
        }
        int i = 0;
        for(APNSClientInfo apnsInfo : APNSConnections.apnsClientList) {

            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(token, topic, payload);

            try {

                PushNotificationFuture<SimpleApnsPushNotification, PushNotificationResponse<SimpleApnsPushNotification>> sendNotificationFuture = apnsInfo.getApnsClient().sendNotification(pushNotification);

                PushNotificationResponse<SimpleApnsPushNotification> pushNotificationResponse = sendNotificationFuture.get();

                if (pushNotificationResponse.isAccepted()) {
                    log.debug("Push notification accepted by APNs gateway.");
                    log.debug("----------> Push Ip : {}", apnsInfo.getIp());
                    log.debug("----------> Push topic : {}", topic);
                    log.debug("----------> Push token : {}", token);
                    log.debug("----------> Push payload : {}", payload);

                    /**
                     * APNS Notification 성공 처리 로직
                     */

                    break;
                } else {
                    log.error("Notification rejected by the APNs gateway: {}, {}", pushNotificationResponse.getRejectionReason(), apnsInfo.getIp());

                    if (i == (APNSConnections.apnsClientList.size() - 1)) {
                        log.error("----------> APNS All Ip Failure");
                        log.error("----------> Push topic : {}", topic);
                        log.error("----------> Push token : {}", token);
                        log.debug("----------> Push payload : {}", payload);

                       /**
                         * 모든 IP에 대해서 실패 했을 경우 처리 로직 구현
                         */

                    }
                    /**
                     * APNS Notification 실패 처리 로직
                     */

                    i++;
                }
            }catch (ExecutionException | InterruptedException e) {
                log.error("Failed to send push notification : {}", e.getMessage());
            }

        }
    }
}
