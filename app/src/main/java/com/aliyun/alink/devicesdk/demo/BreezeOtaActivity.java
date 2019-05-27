package com.aliyun.alink.devicesdk.demo;

import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.Spinner;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.aliyun.alink.devicesdk.adapter.SubDeviceListAdapter;
import com.aliyun.alink.devicesdk.app.DemoApplication;
import com.aliyun.alink.dm.api.BaseInfo;
import com.aliyun.alink.dm.api.DeviceInfo;
import com.aliyun.alink.dm.model.ResponseModel;
import com.aliyun.alink.linkkit.api.LinkKit;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceActionListener;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceChannel;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceConnectListener;
import com.aliyun.alink.linksdk.channel.gateway.api.subdevice.ISubDeviceRemoveListener;
import com.aliyun.alink.linksdk.cmp.connect.channel.MqttRrpcRequest;
import com.aliyun.alink.linksdk.cmp.core.base.AMessage;
import com.aliyun.alink.linksdk.cmp.core.base.ARequest;
import com.aliyun.alink.linksdk.cmp.core.base.AResponse;
import com.aliyun.alink.linksdk.cmp.core.base.ConnectState;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectNotifyListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectRrpcHandle;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectRrpcListener;
import com.aliyun.alink.linksdk.cmp.core.listener.IConnectSendListener;
import com.aliyun.alink.linksdk.tools.AError;
import com.aliyun.alink.linksdk.tools.ALog;
import com.aliyun.iot.breeze.api.IBreeze;
import com.aliyun.iot.breeze.api.IBreezeDevice;
import com.aliyun.iot.breeze.biz.BreezeHelper;
import com.aliyun.iot.breeze.ota.LinkOTABusiness;
import com.aliyun.iot.breeze.ota.Util;
import com.aliyun.iot.breeze.ota.OtaError;
import com.aliyun.iot.breeze.ota.api.ILinkOTABusiness;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 *
 *  Copyright (c) 2014-2016 Alibaba Group. All rights reserved.
 *  License-Identifier: Apache-2.0
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may
 *  not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 *  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */


public class BreezeOtaActivity extends BaseActivity {

    private static final String KEY_MAC = "KEY_MAC";
    private List<DeviceInfo> subDeviceTripleInfoList = new ArrayList<>();

    private String testPublishTopic = "/sys/{productKey}/{deviceName}/thing/event/property/post";
    private String testSubscribePropertyService = "/sys/{productKey}/{deviceName}/thing/service/property/set";
    private String testSubscribeService = "/sys/{productKey}/{deviceName}/thing/service/+";
    private String testSubscribeSyncService = "/sys/{productKey}/{deviceName}/rrpc/request/+";
    private String[] subscribeServiceList = {testSubscribePropertyService, testSubscribeService, testSubscribeSyncService};


    private Spinner subDeviceListSpinner = null;
    private DeviceInfo selectedSubdeviceInfo = null;
    private SubDeviceListAdapter subDeviceListAdapter = null;
    private EditText publishPayloadET = null;
    private ISubDeviceChannel mSubDeviceChannel;
    private String clientId;
    private String sign;

    private BreezeHelper.DeviceInfo connectDeviceInfo;
    private IBreezeDevice connectDevice;
    private EditText mMac;
    private SharedPreferences mSharePre;
    private ILinkOTABusiness mOta;
    private boolean mConnectionHasConnected;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_breeze_ota);
        initViews();
    }

    private void initViews() {
//        publishPayloadET = findViewById(R.id.id_publish_payload);

        mSharePre = getSharedPreferences(BreezeOtaActivity.class.getName(), MODE_PRIVATE);
        mMac = findViewById(R.id.mac);
        mMac.setText(mSharePre.getString(KEY_MAC, ""));
    }

    public void prepareOta(View view){
        mac = mMac.getText().toString();
        if (TextUtils.isEmpty(mac)
                || mac.length() != 17){
            showToast("mac 地址非法");
            return;
        }

        mSharePre.edit().putString(KEY_MAC, mac).commit();

        connect2Mac();
    }

    /**
     * 添加子设备到网关
     * 子设备动态注册之后　可以拿到子设备的 deviceSecret 信息，签名的时候需要使用到
     * 签名方式 sign = hmac_md5(deviceSecret, clientId123deviceNametestproductKey123timestamp1524448722000)
     *
     * @param view
     */
    public void addSubDevice(View view) {

        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("无有效已动态注册的设备，添加失败");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        LinkKit.getInstance().getGateway().gatewayAddSubDevice(info, new ISubDeviceConnectListener() {
            @Override
            public String getSignMethod() {
                ALog.d(TAG, "getSignMethod() called");
                return "sha256";
            }

            @Override
            public Map<String, Object> getSignExtraData() {
                return null;
            }
            @Override
            public String getSignValue() {
                ALog.d(TAG, "getSignValue() called");
                Map<String, String> signMap = new HashMap<>();
                signMap.put("productKey", info.productKey);
                signMap.put("deviceName", info.deviceName);
//                signMap.put("timestamp", String.valueOf(System.currentTimeMillis()));
                signMap.put("clientId", getClientId());
//                return SignUtils.hmacSign(signMap, info.deviceSecret);

                return sign;
            }

            @Override
            public String getClientId() {
                ALog.d(TAG, "getClientId() called");
                return clientId;
            }

            @Override
            public void onConnectResult(boolean isSuccess, ISubDeviceChannel iSubDeviceChannel, AError aError) {
                ALog.d(TAG, "onConnectResult() called with: isSuccess = [" + isSuccess + "], iSubDeviceChannel = [" + iSubDeviceChannel + "], aError = [" + aError + "]");
                if (isSuccess) {
                    showToast("子设备添加成功");
                    log(TAG, "子设备添加成功 " + getPkDn(info));

                    mSubDeviceChannel = iSubDeviceChannel;
                    subDevOnline(null);
                } else {

                    log(TAG, "子设备添加失败 " + getPkDn(info));
                }
            }

            //@Override
            public void onDataPush(String s, String s1) {

            }

            //@Override
            public void onDataPush(String s, AMessage s1) {
                ALog.d(TAG, "收到子设备下行数据  onDataPush() called with: s = [" + s + "], s1 = [" + s1 + "]");
            }
        });

    }


    /**
     * 网关添加子设备之后才能代理子设备上线
     *
     * @param view
     */
    private void subDevOnline(View view) {
        if (selectedSubdeviceInfo == null || TextUtils.isEmpty(selectedSubdeviceInfo.productKey) ||
                TextUtils.isEmpty(selectedSubdeviceInfo.deviceName)) {
            showToast("无有效已动态注册的设备，上线失败");
            return;
        }
        final DeviceInfo info = selectedSubdeviceInfo;
        LinkKit.getInstance().getGateway().gatewaySubDeviceLogin(info, new ISubDeviceActionListener() {
            @Override
            public void onSuccess() {
                ALog.d(TAG, "onSuccess() called");
                showToast("子设备上线成功");
                log(TAG, "子设备上线成功" + getPkDn(info));

//                mOta = new LinkOTABusiness(connectDevice, Util.getMqttChannel(connectDeviceInfo), connectDeviceInfo);
                mOta.init();
                mOta.registerOtaPushListener(new ILinkOTABusiness.IOtaPushListener() {
                    @Override
                    public void onNewVersion(String s, String s1) {

                        log(TAG, "收到云端新固件推送通知，可以开始/停止 OTA升级");
                    }
                });

                log(TAG, "OTA 准备工作就绪,等待云端推送 OTA 固件信息");
            }

            @Override
            public void onFailed(AError aError) {
                ALog.d(TAG, "onFailed() called with: aError = [" + aError + "]");
                showToast("子设备上线失败");
                log(TAG, "子设备上线失败" + getPkDn(info));
            }
        });
    }

    public void startOta(View view){
        if (null == mOta) {
            return;
        }

        mOta.startUpgrade("", false, ILinkOTABusiness.DEVICE_TYPE_BLE, new ILinkOTABusiness.IOtaListener() {
            @Override
            public void onNotification(int type, ILinkOTABusiness.IOtaError error) {
                String status = LinkOTABusiness.toOtaTypeStr(type);
                if(null != error && error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR) {
                    status += " " + error.getData();

                    if (type == TYPE_TRANSMIT || type == TYPE_DOWNLOAD) {
                        status += "%";
                    }
                }
                if(null != error && error.getCode() != ILinkOTABusiness.IOtaError.NO_ERROR) {
                    status += " " + OtaError.toCodeString(error.getCode());
                }


                log(TAG, status);
                if (type == TYPE_FINISH ) {
                    if (error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR) {
                        status = "OTA 成功";
                    } else {
                        status = "OTA 失败";
                    }
                    final String finalStatus = status;
                    mMac.post(new Runnable() {
                                  @Override
                                  public void run() {
                                      logStr = "";
                                      log(TAG, finalStatus);
                                  }
                              }
                    );
                }

                String message = "";
                try {
                    if (type == TYPE_CHECK) {
                        message = "debug:OTA:Check";
                        if (error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR) {
                            message += ":" + status;
                        }
                    } else if (type == TYPE_DOWNLOAD) {
                        if (error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR
                                && "0".equalsIgnoreCase((String) error.getData())) {
                            message = "debug:OTA:Download:start";
                        }
                        if (error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR
                                && "100".equalsIgnoreCase((String) error.getData())) {
                            message = "debug:OTA:Download:complete";
                        }
                        if (error.getCode() != ILinkOTABusiness.IOtaError.NO_ERROR) {
                            message += "debug:OTA:Download:" + status;
                        }
                    } else if (type == TYPE_TRANSMIT) {
                        if (error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR
                                && "0".equalsIgnoreCase((String) error.getData())) {
                            message = "debug:OTA:Transmit:start";
                        }
                        if (error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR
                                && "100".equalsIgnoreCase((String) error.getData())) {
                            message = "debug:OTA:Transmit:complete";
                        }
                        if (error.getCode() != ILinkOTABusiness.IOtaError.NO_ERROR) {
                            message += "debug:OTA:Transmit:" + status;
                        }
                    } else if (type == TYPE_REBOOT) {
                        message = "debug:OTA:Reboot";
                        if (error.getCode() == ILinkOTABusiness.IOtaError.NO_ERROR) {
                            message += ":" + status;
                        }
                    } else if (type == TYPE_FINISH) {
                        message = "debug:OTA:Finish";
                        if (error.getCode() != ILinkOTABusiness.IOtaError.NO_ERROR) {
                            message += ":" + status;
                        }
                    }

                    if (!TextUtils.isEmpty(message)) {
                        log(TAG, message);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    public void stopOta(View view){
        if (null == mOta) {
            return;
        }

        mOta.stopUpgrade();
    }

    private List<BaseInfo> getSubDevList() {
        return DemoApplication.mDeviceInfoData.subDevice;
    }


    private String getPkDn(DeviceInfo info) {
        if (info == null) {
            return null;
        }
        return "[pk=" + info.productKey + ",dn=" + info.deviceName + "]";
    }



    String mac = "C1:90:F8:77:16:0A";
    public void connect2Mac(){
        mConnectionHasConnected = false;
        log(TAG, "连接蓝牙设备 mac:" + mac);
        DemoApplication.BREEZE.open(false, mac, new IBreeze.ConnectionCallback() {
            @Override
            public void onConnectionStateChange(final IBreezeDevice device, int state, int status) {
                if (state == BluetoothProfile.STATE_CONNECTED){
                    log(TAG, "连接蓝牙设备成功");
                    if (mConnectionHasConnected){
                        return;
                    }
                    mConnectionHasConnected = true;
                    connectDevice = device;
                    BreezeHelper.getDeviceInfo(device, new BreezeHelper.IDeviceInfoCallback() {
                        @Override
                        public void onDeviceInfo(BreezeHelper.DeviceInfo deviceInfo) {
                            Log.i(TAG, "device info:" + deviceInfo);

                            if (null == deviceInfo){
                                log(TAG, "获取设备签名失败");
                                return;
                            }

                            connectDeviceInfo = deviceInfo;
                            selectedSubdeviceInfo = new DeviceInfo();
                            selectedSubdeviceInfo.deviceName = deviceInfo.deviceName;
                            selectedSubdeviceInfo.productKey = deviceInfo.productKey;
                            clientId = deviceInfo.scanRecord.getModelIdHexStr();
                            sign = deviceInfo.sign;
                            addSubDevice(null);
                        }
                    });
                } else if (state == BluetoothProfile.STATE_DISCONNECTED){
                    showToast("蓝牙设备连接断开");
                    log(TAG, "蓝牙设备连接断开");
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        DemoApplication.BREEZE.close(mac, new IBreeze.ConnectionCallback() {
            @Override
            public void onConnectionStateChange(IBreezeDevice iBreezeDevice, int i, int i1) {

            }
        });

        if (null != mOta){
            mOta.deInit();
        }
    }
}
