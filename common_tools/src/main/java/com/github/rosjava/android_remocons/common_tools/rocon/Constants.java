package com.github.rosjava.android_remocons.common_tools.rocon;

import org.ros.internal.message.DefaultMessageFactory;
import org.ros.internal.message.definition.MessageDefinitionReflectionProvider;

import rocon_std_msgs.PlatformInfo;
import rocon_std_msgs.Strings;

/**
 * General rocon android app constants and topic/parameter/service names
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class Constants {
    public static final int NFC_SSID_FIELD_LENGTH           =  16;
    public static final int NFC_PASSWORD_FIELD_LENGTH       =  16;
    public static final int NFC_MASTER_HOST_FIELD_LENGTH    =  16;
    public static final int NFC_MASTER_PORT_FIELD_LENGTH    =   2;
    public static final int NFC_APP_HASH_FIELD_LENGTH       =   4;
    public static final int NFC_EXTRA_DATA_FIELD_LENGTH     =   2;
    public static final int NFC_APP_RECORD_FIELD_LENGTH     =  56;
    public static final int NFC_PAYLOAD_LENGTH              =  56; // 16 + 16 + 16 + 2 + 4 + 2
    public static final int NFC_ULTRALIGHT_C_MAX_LENGTH     = 137;

    // unique identifier to key string variables between activities.
    static public final String ACTIVITY_SWITCHER_ID = "com.github.rosjava.android_remocons.common_tools.rocon.Constants";
    static public final String ACTIVITY_ROCON_REMOCON = "com.github.rosjava.android_remocons.rocon_remocon.Remocon";

    public static final rocon_std_msgs.PlatformInfo ANDROID_PLATFORM_INFO = makePlatformInfo();

    /**
     * Generate platform information, most specifically, the rocon uri string that is needed.
     *
     * @todo : this doesn't introspect the android phone...
     *
     * @return rocon_std_msgs.PlatformInfo : filled out platform information
     */
    private static PlatformInfo makePlatformInfo() {
        MessageDefinitionReflectionProvider messageDefinitionProvider = new MessageDefinitionReflectionProvider();
        DefaultMessageFactory messageFactory = new DefaultMessageFactory(messageDefinitionProvider);
        PlatformInfo platformInfo = messageFactory.newFromType(PlatformInfo._TYPE);
        // rocon:/hw/name/app_framework/os"
        platformInfo.setUri("rocon:/"
                        + Strings.URI_WILDCARD + "/" + Strings.URI_WILDCARD + "/"
                        + Strings.APPLICATION_FRAMEWORK_INDIGO + "/"
                        + Strings.OS_ICE_CREAM_SANDWICH + "|" + Strings.OS_JELLYBEAN + "|"
                        + Strings.OS_CHROME
        );
        platformInfo.setVersion(Strings.ROCON_VERSION);
        /* Not yet implemented */
        /* platformInfo.setIcon() */

        return platformInfo;
    }
}
