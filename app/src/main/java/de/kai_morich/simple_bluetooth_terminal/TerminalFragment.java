package de.kai_morich.simple_bluetooth_terminal;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.media.MediaScannerConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.method.ScrollingMovementMethod;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

public class TerminalFragment extends Fragment implements ServiceConnection, SerialListener {

    private enum Connected { False, Pending, True }

    private String deviceAddress;
    private SerialService service;

    private TextView receiveText;
    private TextView sendText;
    private TextUtil.HexWatcher hexWatcher;

    private Connected connected = Connected.False;
    private boolean initialStart = true;
    private boolean hexEnabled = false;
    private boolean pendingNewline = false;
    private String newline = TextUtil.newline_crlf;
    private boolean autoScroll = true;


    class psensor_cali_data
    {
        public byte flag;
        public int inear_value;
        public int outear_value;
    }


    /*
     * Lifecycle
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
        deviceAddress = getArguments().getString("device");
    }

    @Override
    public void onDestroy() {
        if (connected != Connected.False)
            disconnect();
        getActivity().stopService(new Intent(getActivity(), SerialService.class));
        super.onDestroy();
    }

    @Override
    public void onStart() {
        super.onStart();
        if(service != null)
            service.attach(this);
        else
            getActivity().startService(new Intent(getActivity(), SerialService.class)); // prevents service destroy on unbind from recreated activity caused by orientation change
    }

    @Override
    public void onStop() {
        if(service != null && !getActivity().isChangingConfigurations())
            service.detach();
        super.onStop();
    }

    @SuppressWarnings("deprecation") // onAttach(context) was added with API 23. onAttach(activity) works for all API versions
    @Override
    public void onAttach(@NonNull Activity activity) {
        super.onAttach(activity);
        getActivity().bindService(new Intent(getActivity(), SerialService.class), this, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onDetach() {
        try { getActivity().unbindService(this); } catch(Exception ignored) {}
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        if(initialStart && service != null) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder binder) {
        service = ((SerialService.SerialBinder) binder).getService();
        service.attach(this);
        if(initialStart && isResumed()) {
            initialStart = false;
            getActivity().runOnUiThread(this::connect);
        }
    }

    @Override
    public void onServiceDisconnected(ComponentName name) {
        service = null;
    }

    /*
     * UI
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_terminal, container, false);
        receiveText = view.findViewById(R.id.receive_text);                          // TextView performance decreases with number of spans
        receiveText.setTextColor(getResources().getColor(R.color.colorRecieveText)); // set as default color to reduce number of spans
        receiveText.setMovementMethod(ScrollingMovementMethod.getInstance());

        sendText = view.findViewById(R.id.send_text);
        hexWatcher = new TextUtil.HexWatcher(sendText);
        hexWatcher.enable(hexEnabled);
        sendText.addTextChangedListener(hexWatcher);
        sendText.setHint(hexEnabled ? "HEX mode" : "");

        View sendBtn = view.findViewById(R.id.send_btn);
        sendBtn.setOnClickListener(v -> send(sendText.getText().toString()));

        View ckProductBtn = view.findViewById(R.id.id_ck_product_mode);
        ckProductBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_CHECK_PRODUCT_MODE);
            sendBinaryData(cmd);
        });

        View cleanProdutBtn = view.findViewById(R.id.id_clean_product_mode);
        cleanProdutBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_CLEAN_PRODUCT_MODE);
            sendBinaryData(cmd);
        });

        View setProductBtn = view.findViewById(R.id.id_set_product_mode);
        setProductBtn.setOnClickListener(v-> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_SET_PRODUCT_MODE);
            sendBinaryData(cmd);
        });

        View getCaliBtn = view.findViewById(R.id.id_get_cali_data);
        getCaliBtn.setOnClickListener(v-> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_GET_CALI_DATA);
            sendBinaryData(cmd);
        });

        View getRawdataBtn = view.findViewById(R.id.id_get_raw_data);
        getRawdataBtn.setOnClickListener(v-> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_GET_RAW_DATA);
            sendBinaryData(cmd);
        });

        View getInearStaBtn = view.findViewById(R.id.id_get_inear_status);
        getInearStaBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_GET_INEAR_STATUS);
            sendBinaryData(cmd);
        });

        View sppOnBtn = view.findViewById(R.id.id_set_spp_log);
        sppOnBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_RACE_SPP_LOG_ON);
            sendBinaryData(cmd);
        });

        View sppOffBtn = view.findViewById(R.id.id_clean_spp_log);
        sppOffBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_RACE_SPP_LOG_OFF);
            sendBinaryData(cmd);
        });

        View outEarCaliBtn = view.findViewById(R.id.id_outear_cali);
        outEarCaliBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_RACE_CAL_CT);
            sendBinaryData(cmd);
        });

        View inEarCaliBtn = view.findViewById(R.id.id_inear_cali);
        inEarCaliBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_RACE_CAL_G2);
            sendBinaryData(cmd);
        });

        View ancHighBtn = view.findViewById(R.id.id_anc_high);
        ancHighBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_ANC_HIGH);
            sendBinaryData(cmd);
        });

        View ancLowBtn = view.findViewById(R.id.id_set_anc_low);
        ancLowBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_ANC_LOW);
            sendBinaryData(cmd);
        });

        View ancWindBtn = view.findViewById(R.id.id_set_anc_wind);
        ancWindBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_ANC_WIND);
            sendBinaryData(cmd);
        });

        View switchLeftChannBtn = view.findViewById(R.id.id_switch_left_channel);
        switchLeftChannBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_CHANNEL_LEFT);
            sendBinaryData(cmd);
        });

        View switchRightChannBtn = view.findViewById(R.id.id_switch_right_channel);
        switchRightChannBtn.setOnClickListener(v -> {
            byte[] cmd = consCommandByte(Constants.PSENSOR_CHANNEL_RIGHT);
            sendBinaryData(cmd);
        });

        FloatingActionButton mFAButton = (FloatingActionButton) view.findViewById(R.id.fab_save_log_id);
        mFAButton.setCompatElevation(5.0F); //设置FloatingActionButton的高度值，产生相应的阴影效果
        mFAButton.setRippleColor(Color.parseColor("#FFFFFFFF")); //设置涟漪效果颜色
        //
        mFAButton.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FF13C6DA"))); //设置Fab的背景颜色
        //mFAButton.setVisibility(View.INVISIBLE); //设置Fab的可见性
        mFAButton.setSize(FloatingActionButton.SIZE_AUTO); // 设置Fab的大小

        /* 为FloatingActionButton设置点击事件 */
        mFAButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(getActivity(), "begin to save log", Toast.LENGTH_SHORT).show();
                saveLogFile();
//                mFAButton.hide(new FloatingActionButton.OnVisibilityChangedListener() {
//                    @Override
//                    public void onHidden(FloatingActionButton fab) {
//                        super.onHidden(fab);
//                        Toast.makeText(getActivity(), "OnShown", Toast.LENGTH_SHORT).show();
//                        Log.d(TAG, "Hide");
//                        Log.d(TAG, "onHidden: " + mFAButton.getRippleColor());
//                        Log.d(TAG, "onHidden: " + mFAButton.getCompatElevation());
//                        //Log.d(TAG, "onHidden: " + mFAButton.getElevation());
//                    }
//                });
            }
        });



        return view;
    }

    @Override
    public void onCreateOptionsMenu(@NonNull Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_terminal, menu);
        menu.findItem(R.id.hex).setChecked(hexEnabled);
        menu.findItem(R.id.autoscroll).setChecked(autoScroll);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.clear) {
            receiveText.setText("");
            return true;
        } else if (id == R.id.newline) {
            String[] newlineNames = getResources().getStringArray(R.array.newline_names);
            String[] newlineValues = getResources().getStringArray(R.array.newline_values);
            int pos = java.util.Arrays.asList(newlineValues).indexOf(newline);
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setTitle("Newline");
            builder.setSingleChoiceItems(newlineNames, pos, (dialog, item1) -> {
                newline = newlineValues[item1];
                dialog.dismiss();
            });
            builder.create().show();
            return true;
        } else if (id == R.id.hex) {
            hexEnabled = !hexEnabled;
            sendText.setText("");
            hexWatcher.enable(hexEnabled);
            sendText.setHint(hexEnabled ? "HEX mode" : "");
            item.setChecked(hexEnabled);
            return true;
        }
        else if (id == R.id.autoscroll)
        {
            autoScroll = !autoScroll;
            item.setChecked(autoScroll);

            if (autoScroll) {
                receiveText.setGravity(Gravity.BOTTOM);
            } else
            {
                receiveText.setGravity(Gravity.NO_GRAVITY);
            }

            return true;
        }
        else {
            return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Serial + UI
     */
    private void connect() {
        try {
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            BluetoothDevice device = bluetoothAdapter.getRemoteDevice(deviceAddress);
            status("connecting...");
            connected = Connected.Pending;
            SerialSocket socket = new SerialSocket(getActivity().getApplicationContext(), device);
            service.connect(socket);
        } catch (Exception e) {
            onSerialConnectError(e);
        }
    }

    private void disconnect() {
        connected = Connected.False;
        service.disconnect();
    }

    private void send(String str) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            if(hexEnabled) {
                StringBuilder sb = new StringBuilder();
                TextUtil.toHexString(sb, TextUtil.fromHexString(str));
                TextUtil.toHexString(sb, newline.getBytes());
                msg = sb.toString();
                data = TextUtil.fromHexString(msg);
            } else {
                msg = str;
                data = (str + newline).getBytes();
            }
            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(data);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void sendBinaryData(byte[] cmd) {
        if(connected != Connected.True) {
            Toast.makeText(getActivity(), "not connected", Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            String msg;
            byte[] data;
            StringBuilder sb = new StringBuilder();
            TextUtil.toHexString(sb, cmd);
            msg = sb.toString();

            SpannableStringBuilder spn = new SpannableStringBuilder(msg + '\n');
            spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorSendText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            receiveText.append(spn);
            service.write(cmd);
        } catch (Exception e) {
            onSerialIoError(e);
        }
    }

    private void receiveBinary(byte[] data) {
        int len;
        int raceid;
        boolean error = true;

        if ((data[0] == Constants.PROTOCOL_FRAME_START) && (data[1] == Constants.PROTOCOL_FRAME_RSP)) {
            receiveText.append(TextUtil.toHexString(data) + '\n');

            len = data[3] & 0xff;
            len <<= 8;
            len |= data[2] & 0xff;

            if (len + 4 == data.length)
            {
                raceid = data[5] & 0xff;
                raceid <<= 8;
                raceid |= data[4] & 0xff;

                if (raceid == Constants.CUSTOMER_RACE_CMD_ID)
                {
                    int cmd = data[Constants.EVENT_INDEX];
                    int index = Constants.PARAM_INDEX;
                    int result = data[index];
                    index += 1;
                    error = false;
                    switch (cmd)
                    {
                        case Constants.PSENSOR_CLEAN_PRODUCT_MODE: {
                            if (result == 1) {
                                Toast.makeText(service, "clean product mode success", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(service, "clean product mode failed.", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }

                        case Constants.PSENSOR_SET_PRODUCT_MODE: {
                            if (result == 1) {
                                Toast.makeText(service, "set product mode success", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(service, "set product mode failed.", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }

                        case Constants.PSENSOR_CHECK_PRODUCT_MODE: {
                            boolean customer_ui =  ((result >> Constants.CUSTOMER_UI_INDEX) & 0x1) == 1;
                            boolean product_mode = ((result >> Constants.CUSTOMER_PRODUCT_INDEX) & 0x1) == 1;

                            if (!customer_ui && product_mode) {
                                Toast.makeText(service, "product mode", Toast.LENGTH_SHORT).show();
                            } else if (customer_ui && !product_mode) {
                                Toast.makeText(service, "user mode", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(service, "earphone error", Toast.LENGTH_SHORT).show();
                            }
                            break;
                        }
                        case Constants.PSENSOR_GET_CALI_DATA: {
                            psensor_cali_data left_value;
                            psensor_cali_data right_value;
                            if (result == 1)
                            {
                                byte[] cali_byte = new byte[Constants.CALIDATA_LEN];
                                StringBuffer prompt = new StringBuffer();

                                System.arraycopy(data, index , cali_byte, 0, Constants.CALIDATA_LEN);
                                left_value = processCaliData(cali_byte);
                                if (left_value == null)
                                {
                                    Toast.makeText(service, "left calidata format error.", Toast.LENGTH_SHORT).show();
                                }
                                else
                                {
                                    if (left_value.flag == Constants.NOT_EXIST)
                                    {
                                        prompt.append("left earphone not exist!");
                                    }
                                    else if (left_value.flag == Constants.CALIBRATED)
                                    {
                                        prompt.append("left earphone calibrated!");
                                        prompt.append("in ear value:");
                                        prompt.append(TextUtil.shortToHex(left_value.inear_value));
                                        prompt.append(",out ear value:");
                                        prompt.append(TextUtil.shortToHex(left_value.outear_value));
                                    }
                                    else if (left_value.flag == Constants.NOT_CALIBRATED)
                                    {
                                        prompt.append("left earphone not calibrated.");
                                    }
                                }
                                index += Constants.CALIDATA_LEN;
                                System.arraycopy(data, index , cali_byte, 0, Constants.CALIDATA_LEN);

                                right_value = processCaliData(cali_byte);
                                if (right_value == null)
                                {
                                    Toast.makeText(service, "right calidata format error.", Toast.LENGTH_SHORT).show();
                                }
                                else
                                {
                                    if (right_value.flag == Constants.NOT_EXIST)
                                    {
                                        prompt.append("right earphone not exist!");
                                    }
                                    else if (right_value.flag == Constants.CALIBRATED)
                                    {
                                        prompt.append(", right earphone calibrated!");
                                        prompt.append("in ear value:");
                                        prompt.append(TextUtil.shortToHex(right_value.inear_value));
                                        prompt.append(",out ear value:");
                                        prompt.append(TextUtil.shortToHex(right_value.outear_value));
                                    }
                                    else if (right_value.flag == Constants.NOT_CALIBRATED)
                                    {
                                        prompt.append("right earphone not calibrated.");
                                    }
                                }

                                AlertDialog dialog = new AlertDialog.Builder(getActivity())
                                        .setTitle("cali data")
                                        .setMessage(prompt)
                                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                               // Toast.makeText(getActivity(), "Cancel", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            }
                                        })
                                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                              //  Toast.makeText(getActivity(), "OK", Toast.LENGTH_SHORT).show();
                                                dialog.dismiss();
                                            }
                                        }).create();
                                dialog.show();
                            }
                            else
                            {
                                Toast.makeText(service, "get calidata failed.", Toast.LENGTH_LONG).show();
                            }

                            break;
                        }
                        case Constants.PSENSOR_GET_RAW_DATA: {
                            if (result == 1) {
                                int raw_data;

                                raw_data = data[index + 1] & 0xff;
                                raw_data <<= 8;
                                raw_data |= data[index] & 0xff;

                                String info = new String("raw data is");
                                info += raw_data;

                                Toast.makeText(service, info, Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(service, "get raw data failed.", Toast.LENGTH_LONG).show();
                            }

                            break;
                        }

                        case Constants.PSENSOR_GET_INEAR_STATUS: {
                            if (result == 1) {
                                Toast.makeText(service, "in ear", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(service, "out ear", Toast.LENGTH_LONG).show();
                            }

                            break;
                        }

                        case Constants.PSENSOR_RACE_SPP_LOG_ON: {
                            if (result == 1) {
                                Toast.makeText(service, "log on success", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(service, "log on failed.", Toast.LENGTH_LONG).show();
                            }

                            break;
                        }

                        case Constants.PSENSOR_RACE_SPP_LOG_OFF: {
                            if (result == 1) {
                                Toast.makeText(service, "log off success", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(service, "log off failed.", Toast.LENGTH_LONG).show();
                            }
                            break;
                        }

                        case Constants.PSENSOR_RACE_CAL_CT: {
                            if (result == 1) {
                                Toast.makeText(service, "out ear calibrated ok", Toast.LENGTH_SHORT).show();
                            }
                            else
                            {
                                Toast.makeText(service, "out ear calibrated failed.", Toast.LENGTH_LONG).show();
                            }

                            break;
                        }

                        case Constants.PSENSOR_RACE_CAL_G2: {
                            if (result == 1) {
                                Toast.makeText(service, "out ear calibrate cmd ok, polling calibrated status", Toast.LENGTH_SHORT).show();
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        byte[] cmd = consCommandByte(Constants.PSENSOR_QUERY_CALI_STATUS);
                                        sendBinaryData(cmd);
                                    }
                                }, 1000);
                            }
                            else
                            {
                                Toast.makeText(service, "in ear calibrated failed.", Toast.LENGTH_LONG).show();
                            }
                            break;
                        }

                        case Constants.PSENSOR_QUERY_CALI_STATUS: {
                            if (result == Constants.PSENSOR_QUERY_CALI_FAIL) {
                                Toast.makeText(service, "out ear calibrated value failed.", Toast.LENGTH_LONG).show();
                            }
                            else if (result == Constants.PSENSOR_QUERY_CALI_SUCCESS) {
                                Toast.makeText(service, "out ear calibrated value success.", Toast.LENGTH_LONG).show();
                            } else {
                                //Toast.makeText(service, "out ear calibrated value processing.", Toast.LENGTH_LONG).show();
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        byte[] cmd = consCommandByte(Constants.PSENSOR_QUERY_CALI_STATUS);
                                        sendBinaryData(cmd);
                                    }
                                }, 1000);
                            }

                            break;
                        }

                        case Constants.PSENSOR_ANC_HIGH:
                        case Constants.PSENSOR_ANC_LOW:
                        case Constants.PSENSOR_ANC_WIND:
                        case Constants.PSENSOR_CHANNEL_LEFT:
                        case Constants.PSENSOR_CHANNEL_RIGHT: {
                            final String[] ok_array =
                            {
                                  "anc high success"  ,
                                    "anc low success",
                                    "anc wind success",
                                    "switch left channel success",
                                    "switch right channel success"
                            };
                            final String[] nok_array =
                            {
                                    "anc high failed"  ,
                                    "anc low failed",
                                    "anc wind failed",
                                    "switch left channel failed",
                                    "switch right channel failed"
                            };

                            int idx = cmd - Constants.PSENSOR_ANC_HIGH;

                            if (result == 1) {
                                Toast.makeText(service, ok_array[idx], Toast.LENGTH_SHORT).show();
                            }
                            else {
                                Toast.makeText(service,  nok_array[idx], Toast.LENGTH_SHORT).show();
                            }

                            break;
                        }

                        default: {
                            throw new IllegalStateException("Unexpected value: " + cmd);
                        }
                    }
                }
            }
        }
        else
        {
            if (TextUtil.isAllAscii(data)) {
                error = false;
                String str = new String(data);
                receiveText.append(str);
            }
        }

        if (error)
        {
            Toast.makeText(service, "error", Toast.LENGTH_SHORT).show();
        }
    }


    private void receive(byte[] data) {
        if(hexEnabled) {
            receiveText.append(TextUtil.toHexString(data) + '\n');
        } else {
            String msg = new String(data);
            if(newline.equals(TextUtil.newline_crlf) && msg.length() > 0) {
                // don't show CR as ^M if directly before LF
                msg = msg.replace(TextUtil.newline_crlf, TextUtil.newline_lf);
                // special handling if CR and LF come in separate fragments
                if (pendingNewline && msg.charAt(0) == '\n') {
                    Editable edt = receiveText.getEditableText();
                    if (edt != null && edt.length() > 1)
                        edt.replace(edt.length() - 2, edt.length(), "");
                }
                pendingNewline = msg.charAt(msg.length() - 1) == '\r';
            }
            receiveText.append(TextUtil.toCaretString(msg, newline.length() != 0));
        }
    }

    private void status(String str) {
        SpannableStringBuilder spn = new SpannableStringBuilder(str + '\n');
        spn.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.colorStatusText)), 0, spn.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        receiveText.append(spn);
    }

    /*
     * SerialListener
     */
    @Override
    public void onSerialConnect() {
        status("connected");
        connected = Connected.True;
    }

    @Override
    public void onSerialConnectError(Exception e) {
        status("connection failed: " + e.getMessage());
        disconnect();
    }

    @Override
    public void onSerialRead(byte[] data) {
        receiveBinary(data);
    }

    @Override
    public void onSerialIoError(Exception e) {
        status("connection lost: " + e.getMessage());
        disconnect();
    }

    byte[] consCommandByte(byte cmd) {
        byte[] cmdArray = new byte[] { 0x05, 0x5A, 0x05, 0x00, 0x00, 0x20, 0x00, 0x0B, 0x13};

        cmdArray[Constants.EVENT_INDEX] = cmd;
        cmdArray[Constants.PARAM_INDEX] = cmd;

        return cmdArray;
    }

    psensor_cali_data  processCaliData(byte[] rsp)
    {
        psensor_cali_data data;

        if (rsp[0] == Constants.LEFT_CHANNEL || rsp[0] == Constants.RIGHT_CHANNEL)
        {
            data = new psensor_cali_data();
            data.flag = rsp[1];

            data.outear_value = rsp[3] & 0xff;
            data.outear_value <<= 8;
            data.outear_value |= rsp[2] & 0xff;

            data.inear_value = rsp[5] & 0xff;
            data.inear_value <<= 8;
            data.inear_value |= rsp[4] & 0xff;

            return data;
        }

        return null;
    }


    public String getCurrentTimestamp() {
        Calendar calendar = Calendar.getInstance(); // 如果不设置时间，则默认为当前时间
        calendar.setTime(new Date()); // 将系统当前时间赋值给 Calendar 对象
        System.out.println("现在时刻：" + calendar.getTime()); // 获取当前时间
        int year = calendar.get(Calendar.YEAR); // 获取当前年份
        System.out.println("现在是" + year + "年");
        int month = calendar.get(Calendar.MONTH) + 1; // 获取当前月份（月份从 0 开始，所以加 1）
        System.out.print(month + "月");
        int day = calendar.get(Calendar.DATE); // 获取日
        System.out.print(day + "日");
        int hour = calendar.get(Calendar.HOUR_OF_DAY); // 获取当前小时数（24 小时制）
        System.out.print(hour + "时");
        int minute = calendar.get(Calendar.MINUTE); // 获取当前分钟
        System.out.print(minute + "分");
        int second = calendar.get(Calendar.SECOND); // 获取当前秒数
        System.out.print(second + "秒");

        String s = String.format("%d-%d-%d_%d-%d-%d", year, month, day, hour, minute, second);

        Log.d("Terminal", "timestamp = " + s);

        return s;
    }


    // save the file into filesystem
    public boolean saveLogFile()
    {
        try
        {
            // Creates a trace file in the primary external storage space of the
            // current application.
            // If the file does not exists, it is created.
            String filename = "logFile_" + getCurrentTimestamp() + ".txt";
            Log.d("Terminal", "filename = " + filename);
            File traceFile = new File(((Context)getActivity()).getExternalFilesDir(null), filename);
            if (!traceFile.exists())
                traceFile.createNewFile();
            // Adds a line to the trace file
            BufferedWriter writer = new BufferedWriter(new FileWriter(traceFile, true /*append*/));
            writer.write(receiveText.getText().toString());
            writer.close();
            // Refresh the data so it can seen when the device is plugged in a
            // computer. You may have to unplug and replug the device to see the
            // latest changes. This is not necessary if the user should not modify
            // the files.
            MediaScannerConnection.scanFile((Context)(getActivity()),
                    new String[] { traceFile.toString() },
                    null,
                    null);
        }
        catch (IOException e)
        {
            Log.e("Terminal", "Unable to write to the TraceFile.txt file.");
            return false;
        }

        return true;
    }


}
