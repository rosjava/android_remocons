package com.github.rosjava.android_remocons.common_tools;

/**
 * General Rocon Android apps constants and topic/parameter/service names
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public interface RoconConstants {
    public static final int NFC_SSID_FIELD_LENGTH           =  16;
    public static final int NFC_PASSWORD_FIELD_LENGTH       =  16;
    public static final int NFC_MASTER_HOST_FIELD_LENGTH    =  16;
    public static final int NFC_MASTER_PORT_FIELD_LENGTH    =   2;
    public static final int NFC_NFC_APP_ID_FIELD_LENGTH     =   2;
    public static final int NFC_EXTRA_DATA_FIELD_LENGTH     =   2;
    public static final int NFC_APP_RECORD_FIELD_LENGTH     =  58;
    public static final int NFC_PAYLOAD_LENGTH              =  54; // 16 + 16 + 16 + 2 + 2 + 2
    public static final int NFC_ULTRALIGHT_C_MAX_LENGTH     = 137;

    public static final String CONCERT_NAME_PARAM      = "/concert/name";
    public static final String CONCERT_INFO_TOPIC      = "/concert/info";
    public static final String CONCERT_ROLES_TOPIC     = "/concert/interactions/roles";

    public static final String GET_ROLES_AND_APPS_SRV  = "/concert/interactions/get_roles_and_apps";
    public static final String REQUEST_INTERACTION_SRV = "/concert/interactions/request_interaction";
}