package com.byteshaft.sendsms.receiver;

import android.content.Context;
import android.util.Log;

import com.byteshaft.requests.HttpRequest;
import com.byteshaft.sendsms.SendSmsService;
import com.byteshaft.sendsms.utils.AppGlobals;
import com.byteshaft.sendsms.utils.Helpers;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;

import static com.byteshaft.sendsms.SendSmsService.api_key;
import static com.byteshaft.sendsms.SendSmsService.url;

/**
 * Created by s9iper1 on 11/12/16.
 */

public class CallReceiver extends Receiver {

    @Override
    protected void onIncomingCallReceived(Context ctx, String number) {
        Log.i("CallReceiver", "onIncomingCallReceived");
        Helpers.appendLog(String.format("%s Ringing -Incomming call from " + number,
                SendSmsService.getInstance().getCurrentLogDetails("")));
        callRequest(ctx, "Incoming", number);
    }


    @Override
    protected void onOutgoingCallStarted(Context ctx, String number) {
        Log.i("CallReceiver", "onOutgoingCallStarted");
        Helpers.appendLog(String.format("%s Ringing -Outgoing call to " + number,
                SendSmsService.getInstance().getCurrentLogDetails("")));
        callRequest(ctx, "Outgoing", number);
    }

    private void callRequest(Context context, final String direction, String number) {
        JSONObject data = new JSONObject();
        HttpRequest request;
        try {
            data.put("api_key", api_key);
            data.put("command", "phone_call_set_state");
            JSONObject params = new JSONObject();
            params.put("call_state", "RINGING");
            params.put("direction", direction.toUpperCase());
            params.put("phone_number", number);
            params.put("queue_name", SendSmsService.queueName);
            Log.i("TAG", "queue name " + SendSmsService.queueName);
            data.put("parameters", params);
            Log.i("TAG", data.toString());
            Helpers.appendLog(String.format("%s Sending request phone_call_set_state",
                    SendSmsService.getInstance().getCurrentLogDetails("")));
            request = new HttpRequest(context);
            Helpers.appendLog(String.format("%s Request phone_call_set_state",
                    SendSmsService.getInstance().getCurrentLogDetails("")));
            request.setOnReadyStateChangeListener(new HttpRequest.OnReadyStateChangeListener() {
                @Override
                public void onReadyStateChange(HttpRequest request, int readyState) {
                    switch (readyState) {
                        case HttpRequest.STATE_DONE:
                            Log.d(AppGlobals.getLOGTAG(getClass()), " STATE_DONE");
                            switch (request.getStatus()) {
                                case HttpURLConnection.HTTP_OK:
                                    String response = request.getResponseText();
                                    Log.i("TAG", "Response :" + response);
                                    try {
                                        JSONObject jsonObject = new JSONObject(response);
                                        if (jsonObject.getString("result").equals("TRUE")) {
                                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") + " " + direction + " call was stored to database.");
                                        }
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    break;
                                default:
                                    Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") + " NOT HTTP status for sms_server_mark_message_as_sent: " + request.getStatusText() + "\n");

                            }
                            break;
                        default:
                            Helpers.appendLog(SendSmsService.getInstance().getCurrentLogDetails("") + " ??? HTTP status for sms_server_mark_message_as_sent: xxx \n");
                    }
                }
            });
            request.open("POST", url);
            request.setTimeout(20000);
            request.setRequestHeader("Content-Type", "application/json");
            request.send(data.toString());
            Log.d("Success", "sending Call request..............");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
