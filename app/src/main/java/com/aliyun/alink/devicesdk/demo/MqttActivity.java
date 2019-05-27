package com.aliyun.alink.devicesdk.demo;


import android.util.Log;
import android.view.View;

import com.aliyun.alink.devicesdk.app.DemoApplication;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttPublishRequest;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttSubscribeRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSubscribeListener;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.log.IDGenerater;

public class MqttActivity extends BaseTemplateActivity {
    @Override
    protected void initViewData() {
        funcTV1.setText("");
        funcET1.setText("/sys/" + DemoApplication.productKey + "/" + DemoApplication.deviceName + "/get");
        funcBT1.setText("订阅");

        funcTV2.setText("/user/update");
        funcET2.setHint("填入qos的值0或1");
        funcBT2.setText("高级版发布");

        funcTV3.setText("/update");
        funcET3.setHint("填入qos的值0或1");
        funcBT3.setText("基础版发布");
        funcRL3.setVisibility(View.VISIBLE);

//        funcTV4.setText("RRPC");
//        funcET4.setHint("填入rrpc topic");
//        funcBT4.setText("基础版发布");
//        funcRL4.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LinkKit.getInstance().registerOnPushListener(notifyListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        LinkKit.getInstance().unRegisterOnPushListener(notifyListener);
    }

    private IConnectNotifyListener notifyListener = new IConnectNotifyListener() {
        @Override
        public void onNotify(String s, String s1, AMessage aMessage) {
            showToast("收到下行消息 topic=" + s1);
        }

        @Override
        public boolean shouldHandle(String s, String s1) {
            return true;
        }

        @Override
        public void onConnectStateChange(String s, ConnectState connectState) {

        }
    };

    @Override
    protected void onFunc1Click() {
        try {
            MqttSubscribeRequest subscribeRequest = new MqttSubscribeRequest();
            subscribeRequest.isSubscribe = true;
            subscribeRequest.topic = funcET1.getText().toString();
            LinkKit.getInstance().subscribe(subscribeRequest, new IConnectSubscribeListener() {
                @Override
                public void onSuccess() {
                    showToast("订阅成功");
                }

                @Override
                public void onFailure(AError aError) {
                    showToast("订阅失败 " + aError);
                }
            });
        } catch (Exception e) {
            showToast("数据异常");
        }
    }

    @Override
    protected void onFunc2Click() {
        try {
            MqttPublishRequest request = new MqttPublishRequest();
            // 支持 0 和 1， 默认0
            int qos = Integer.parseInt(funcET2.getText().toString());
            if (qos != 0 && qos != 1) {
                showToast("qos值非法，设置为0或1");
                return;
            }
            request.qos = qos;
            request.isRPC = false;
            request.topic = "/" + DemoApplication.productKey + "/" + DemoApplication.deviceName + "/user/update";
            request.msgId = String.valueOf(IDGenerater.generateId());
            // TODO 用户根据实际情况填写 仅做参考
            request.payloadObj = "{\"id\":\"" + request.msgId + "\", \"version\":\"1.0\"" + ",\"params\":{\"state\":\"1\"} }";
            LinkKit.getInstance().publish(request, new IConnectSendListener() {
                @Override
                public void onResponse(ARequest aRequest, AResponse aResponse) {
                    Log.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + aResponse + "]");
                    showToast("发布结果 " + (aResponse!=null?aResponse.getData():"null"));
                }

                @Override
                public void onFailure(ARequest aRequest, AError aError) {
                    Log.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                    showToast("发布失败 " + (aError!=null?aError.getCode():"null"));
                }
            });
        } catch (Exception e){
            showToast("发布异常 ");
        }
    }

    @Override
    protected void onFunc3Click() {
        try {
            MqttPublishRequest request = new MqttPublishRequest();
            // 支持 0 和 1， 默认0
            int qos = Integer.parseInt(funcET2.getText().toString());
            if (qos != 0 && qos != 1) {
                showToast("qos值非法，设置为0或1");
                return;
            }
            request.qos = qos;
            request.isRPC = false;
            request.topic = "/" + DemoApplication.productKey + "/" + DemoApplication.deviceName + "/update";
            request.msgId = String.valueOf(IDGenerater.generateId());
            // TODO 用户根据实际情况填写 仅做参考
            request.payloadObj = "{\"id\":\"" + request.msgId + "\", \"version\":\"1.0\"" + ",\"params\":{\"state\":\"1\"} }";
            LinkKit.getInstance().publish(request, new IConnectSendListener() {
                @Override
                public void onResponse(ARequest aRequest, AResponse aResponse) {
                    Log.d(TAG, "onResponse() called with: aRequest = [" + aRequest + "], aResponse = [" + aResponse + "]");
                    showToast("发布成功");
                }

                @Override
                public void onFailure(ARequest aRequest, AError aError) {
                    Log.d(TAG, "onFailure() called with: aRequest = [" + aRequest + "], aError = [" + aError + "]");
                    showToast("发布失败 " + (aError!=null?aError.getCode():"null"));
                }
            });
        } catch (Exception e){
            showToast("发布异常 ");
        }
    }

    @Override
    protected void onFunc4Click() {
//        final MqttRrpcRegisterRequest registerRequest = new MqttRrpcRegisterRequest();
//// rrpcTopic 替换成用户自己自定义的 RRPC topic
//        registerRequest.topic = funcET4.getText().toString();
//// rrpcReplyTopic 替换成用户自己定义的RRPC 响应 topic
////        registerRequest.replyTopic = rrpcReplyTopic;
//// 根据需要填写，一般不填
//// registerRequest.payloadObj = payload;
//// 先订阅 rrpcTopic
//// 云端发布消息到 rrpcTopic
//// 收到下行数据 回复云端（rrpcReplyTopic） 具体可参考 Demo 同步服务调用
//        LinkKit.getInstance().subscribeRRPC(registerRequest, new IConnectRrpcListener() {
//            @Override
//            public void onSubscribeSuccess(ARequest aRequest) {
//                showToast("订阅成功");
//                // 订阅成功
//            }
//
//            @Override
//            public void onSubscribeFailed(ARequest aRequest, AError aError) {
//                // 订阅失败
//                showToast("订阅失败");
//            }
//
//            @Override
//            public void onReceived(ARequest aRequest, IConnectRrpcHandle iConnectRrpcHandle) {
//                // 收到云端下行
//                showToast("收到下行数据");
//                // 响应获取成功
//                if (iConnectRrpcHandle != null){
//                    AResponse aResponse = new AResponse();
//                    // 仅供参考，具体返回云端的数据用户根据实际场景添加到data结构体
//                    aResponse.data = "{\"id\":\"" + 123 + "\", \"code\":\"200\"" + ",\"data\":{} }";
//                    iConnectRrpcHandle.onRrpcResponse(registerRequest.replyTopic, aResponse);
//                }
//            }
//
//            @Override
//            public void onResponseSuccess(ARequest aRequest) {
//                showToast("响应成功");
//                // RRPC 响应成功
//            }
//
//            @Override
//            public void onResponseFailed(ARequest aRequest, AError aError) {
//                showToast("响应失败");
//                // RRPC 响应失败
//            }
//        });
    }

    @Override
    protected void onFunc5Click() {

    }

    @Override
    protected void onFunc6Click() {

    }
}
