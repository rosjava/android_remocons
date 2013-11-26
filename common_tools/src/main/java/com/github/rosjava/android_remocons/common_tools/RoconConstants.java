package com.github.rosjava.android_remocons.common_tools;

import org.ros.internal.message.DefaultMessageFactory;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;

import rocon_std_msgs.PlatformInfo;

/**
 * General Rocon Android apps constants and topic/parameter/service names
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class RoconConstants {
    public static final int NFC_SSID_FIELD_LENGTH           =  16;
    public static final int NFC_PASSWORD_FIELD_LENGTH       =  16;
    public static final int NFC_MASTER_HOST_FIELD_LENGTH    =  16;
    public static final int NFC_MASTER_PORT_FIELD_LENGTH    =   2;
    public static final int NFC_APP_HASH_FIELD_LENGTH       =   4;
    public static final int NFC_EXTRA_DATA_FIELD_LENGTH     =   2;
    public static final int NFC_APP_RECORD_FIELD_LENGTH     =  56;
    public static final int NFC_PAYLOAD_LENGTH              =  56; // 16 + 16 + 16 + 2 + 4 + 2
    public static final int NFC_ULTRALIGHT_C_MAX_LENGTH     = 137;

    public static final String CONCERT_NAME_PARAM      = "/concert/name";
    public static final String CONCERT_INFO_TOPIC      = "/concert/info";
    public static final String CONCERT_ROLES_TOPIC     = "/concert/interactions/roles";

    public static final String GET_APP_INFO_SRV        = "/concert/interactions/get_app";
    public static final String GET_ROLES_AND_APPS_SRV  = "/concert/interactions/get_roles_and_apps";
    public static final String REQUEST_INTERACTION_SRV = "/concert/interactions/request_interaction";

    public static final rocon_std_msgs.PlatformInfo ANDROID_PLATFORM_INFO = makePlatformInfo();

    private static rocon_std_msgs.PlatformInfo makePlatformInfo() {
        MessageDefinitionReflectionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
        DefaultMessageFactory messageFactory = new DefaultMessageFactory(messageDefinitionProvider);
        rocon_std_msgs.PlatformInfo platformInfo = messageFactory.newFromType(rocon_std_msgs.PlatformInfo._TYPE);
    
        platformInfo.setOs(PlatformInfo.OS_ANDROID);
        platformInfo.setVersion(PlatformInfo.VERSION_ANDROID_JELLYBEAN);
        platformInfo.setPlatform(PlatformInfo.PLATFORM_TABLET);
        platformInfo.setSystem(PlatformInfo.SYSTEM_ROSJAVA);
        platformInfo.setName(PlatformInfo.NAME_ANY);
        
        return platformInfo;
    }
}
