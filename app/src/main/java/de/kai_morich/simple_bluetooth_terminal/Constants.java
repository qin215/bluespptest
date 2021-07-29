package de.kai_morich.simple_bluetooth_terminal;

class Constants {

    // values have to be globally unique
    static final String INTENT_ACTION_DISCONNECT = BuildConfig.APPLICATION_ID + ".Disconnect";
    static final String NOTIFICATION_CHANNEL = BuildConfig.APPLICATION_ID + ".Channel";
    static final String INTENT_CLASS_MAIN_ACTIVITY = BuildConfig.APPLICATION_ID + ".MainActivity";

    // values have to be unique within each app
    static final int NOTIFY_MANAGER_START_FOREGROUND_SERVICE = 1001;

    static final byte PSENSOR_RACE_CAL_CT = 3;
    static final byte PSENSOR_RACE_CAL_G2 = 4;
    static final byte PSENSOR_RACE_SWITCH_DBG = 5;
    static final byte PSENSOR_RACE_ENTER_DISCOVERY = 6;

    static final byte PSENSOR_RACE_ENTER_TWS_PAIRING = 7;
    static final byte PSENSOR_RACE_SENSOR_INIT_OK = 8;
    static final byte PSENSOR_RACE_IN_EAR = 9;
    static final byte PSENSOR_RACE_ANC_ON = 0XC;

    static final byte PSENSOR_RACE_ANC_OFF = 0XD;
    static final byte PSENSOR_RACE_SPP_LOG_ON = 0XE;
    static final byte PSENSOR_RACE_SPP_LOG_OFF = 0XF;

    static final byte PSENSOR_CHECK_CUSTOMER_UI = 0X12;
    static final byte PSENSOR_CHECK_PRODUCT_MODE = 0X13;

    static final byte PSENSOR_SET_PRODUCT_MODE = 0x15;
    static final byte PSENSOR_CLEAN_PRODUCT_MODE = 0x16;
    static final byte PSENSOR_GET_CALI_STATUS = 0x17;

    static final byte PSENSOR_SET_CUSTOMER_UI = 0X18;
    static final byte PSENSOR_CLEAN_CUSTOMER_UI = 0X19;
    static final byte PSENSOR_GET_INEAR_STATUS = 0x1A;
    static final byte PSENSOR_GET_NEAR_THRESHOLD_HIGH = 0X1B;


    static final byte PSENSOR_GET_NEAR_THRESHOLD_LOW = 0X1C;
    static final byte PSENSOR_GET_FAR_THRESHOLD_HIGH = 0X1D;
    static final byte PSENSOR_GET_FAR_THRESHOLD_LOW = 0X1E;
    static final byte PSENSOR_GET_RAW_DATA_HIGH = 0X20;



    static final byte PSENSOR_GET_RAW_DATA_LOW = 0x21;
    static final byte PSENSOR_QUERY_CALI_STATUS = 0X22;

    static final byte PSENSOR_ANC_HIGH = 0X23;
    static final byte PSENSOR_ANC_LOW = 0X24;
    static final byte PSENSOR_ANC_WIND = 0X25;
    static final byte PSENSOR_CHANNEL_LEFT = 0X26;
    static final byte PSENSOR_CHANNEL_RIGHT = 0X27;

    static final byte PSENSOR_ONE_PARAM_END = 0X28;

    static final byte PSENSOR_GET_CALI_DATA = 0X30;
    static final byte PSENSOR_GET_RAW_DATA = 0x31;

    static final int CUSTOMER_RACE_CMD_ID = 0X2000;

    static final byte PROTOCOL_FRAME_START = 0X5;
    static final byte PROTOCOL_FRAME_REQ = 0X5A;
    static final byte PROTOCOL_FRAME_RSP = 0X5B;

    static final byte LEFT_CHANNEL = 0;
    static final byte RIGHT_CHANNEL = 1;

    static final byte PARAM_INDEX = 8;
    static final byte SIDE_INDEX = 7;
    static final byte EVENT_INDEX = 6;

    static final byte NOT_EXIST = (byte)0XFF;
    static final byte CALIBRATED = 1;
    static final byte NOT_CALIBRATED = 0;

    static final int CALIDATA_LEN = 6;

    static final int  PSENSOR_QUERY_CALI_FAIL = 0;
    static final int  PSENSOR_QUERY_CALI_SUCCESS = 1;
    static final int  PSENSOR_QUERY_CALI_DOING = 2;


    static final int CUSTOMER_UI_INDEX 	        =	0;
    static final int CUSTOMER_PRODUCT_INDEX		=	1;
    static final int CUSTOMER_PSENSOR_SIM_INDEX =	2;			// 开发板模拟出入耳
    static final int CUSTOMER_SPP_LOG_INDEX	    =	3;

    private Constants() {}
}
