/*
 * Copyright (c) 2010-2019 Belledonne Communications SARL.
 *
 * This file is part of linphone-android
 * (see https://www.linphone.org).
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.vokka.fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import com.google.gson.JsonElement;
import org.linphone.core.Address;
import org.linphone.core.Content;
import org.linphone.core.Core;
import org.linphone.core.CoreListenerStub;
import org.linphone.core.Event;
import org.linphone.core.ProxyConfig;
import org.linphone.core.RegistrationState;
import org.linphone.core.tools.Log;
import org.vokka.LinphoneContext;
import org.vokka.LinphoneManager;
import org.vokka.R;
import org.vokka.client.ApiClient;
import org.vokka.service.LinphoneService;
import org.vokka.settings.LinphonePreferences;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StatusBarFragment extends Fragment {
    private TextView mStatusText, mVoicemailCount, mBalanceText;
    private ImageView mStatusLed;
    private ImageView mVoicemail;
    private CoreListenerStub mListener;
    private MenuClikedListener mMenuListener;
    private RelativeLayout side_menu_quit;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.status_bar, container, false);

        mStatusText = view.findViewById(R.id.status_text);
        mStatusLed = view.findViewById(R.id.status_led);
        ImageView menu = view.findViewById(R.id.side_menu_button);
        mVoicemail = view.findViewById(R.id.voicemail);
        mVoicemailCount = view.findViewById(R.id.voicemail_count);
        mBalanceText = view.findViewById(R.id.balance_text);
        side_menu_quit = view.findViewById(R.id.side_menu_quit);
        mMenuListener = null;
        side_menu_quit.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        quit();
                    }
                });
        menu.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mMenuListener != null) {
                            mMenuListener.onMenuCliked();
                        }
                    }
                });

        // We create it once to not delay the first display
        populateSliderContent();

        mListener =
                new CoreListenerStub() {
                    @Override
                    public void onRegistrationStateChanged(
                            final Core core,
                            final ProxyConfig proxy,
                            final RegistrationState state,
                            String smessage) {
                        if (core.getProxyConfigList() == null) {
                            showNoAccountConfigured();
                            return;
                        }

                        if ((core.getDefaultProxyConfig() != null
                                        && core.getDefaultProxyConfig().equals(proxy))
                                || core.getDefaultProxyConfig() == null) {
                            mStatusLed.setImageResource(getStatusIconResource(state));
                            mStatusText.setText(getStatusIconText(state));

                            Address address = core.getProxyConfigList()[0].getIdentityAddress();
                            ApiClient.getInstance(ApiClient.BASE_URL_2)
                                    .getclientbalance(address.getUsername())
                                    .enqueue(
                                            new Callback<JsonElement>() {
                                                @Override
                                                public void onResponse(
                                                        Call<JsonElement> call,
                                                        Response<JsonElement> response) {
                                                    if (response.isSuccessful()) {
                                                        mBalanceText.setText(
                                                                response.body().toString());
                                                    }
                                                }

                                                @Override
                                                public void onFailure(
                                                        Call<JsonElement> call, Throwable t) {
                                                    t.printStackTrace();
                                                }
                                            });
                        }

                        try {
                            mStatusText.setOnClickListener(
                                    new OnClickListener() {
                                        @Override
                                        public void onClick(View v) {
                                            Core core = LinphoneManager.getCore();
                                            if (core != null) {
                                                core.refreshRegisters();
                                            }
                                        }
                                    });
                        } catch (IllegalStateException ise) {
                            Log.e(ise);
                        }
                    }

                    @Override
                    public void onNotifyReceived(
                            Core core, Event ev, String eventName, Content content) {

                        if (!content.getType().equals("application")) return;
                        if (!content.getSubtype().equals("simple-message-summary")) return;

                        if (content.getSize() == 0) return;

                        int unreadCount = 0;
                        String data = content.getStringBuffer().toLowerCase();
                        String[] voiceMail = data.split("voice-message: ");
                        if (voiceMail.length >= 2) {
                            final String[] intToParse = voiceMail[1].split("/", 0);
                            try {
                                unreadCount = Integer.parseInt(intToParse[0]);
                            } catch (NumberFormatException nfe) {
                                Log.e("[Status Fragment] " + nfe);
                            }
                            if (unreadCount > 0) {
                                mVoicemailCount.setText(String.valueOf(unreadCount));
                                mVoicemail.setVisibility(View.VISIBLE);
                                mVoicemailCount.setVisibility(View.VISIBLE);
                            } else {
                                mVoicemail.setVisibility(View.GONE);
                                mVoicemailCount.setVisibility(View.GONE);
                            }
                        }
                    }
                };

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        Core core = LinphoneManager.getCore();
        if (core != null) {
            core.addListener(mListener);
            ProxyConfig lpc = core.getDefaultProxyConfig();
            if (lpc != null) {
                mListener.onRegistrationStateChanged(core, lpc, lpc.getState(), null);
            } else {
                showNoAccountConfigured();
            }
        } else {
            mStatusText.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (LinphoneContext.isReady()) {
            Core core = LinphoneManager.getCore();
            if (core != null) {
                core.removeListener(mListener);
            }
        }
    }

    public void setMenuListener(MenuClikedListener listener) {
        mMenuListener = listener;
    }

    private void populateSliderContent() {
        Core core = LinphoneManager.getCore();
        if (core != null) {
            mVoicemailCount.setVisibility(View.VISIBLE);

            if (core.getProxyConfigList().length == 0) {
                showNoAccountConfigured();
            }
        }
    }

    private void showNoAccountConfigured() {
        mStatusLed.setImageResource(R.drawable.led_disconnected);
        mStatusText.setText(getString(R.string.no_account));
    }

    private int getStatusIconResource(RegistrationState state) {
        try {
            if (state == RegistrationState.Ok) {
                return R.drawable.led_connected;
            } else if (state == RegistrationState.Progress) {
                return R.drawable.led_inprogress;
            } else if (state == RegistrationState.Failed) {
                return R.drawable.led_error;
            } else {
                return R.drawable.led_disconnected;
            }
        } catch (Exception e) {
            Log.e(e);
        }

        return R.drawable.led_disconnected;
    }

    private String getStatusIconText(RegistrationState state) {
        Context context = getActivity();
        try {
            if (state == RegistrationState.Ok) {
                return context.getString(R.string.status_connected);
            } else if (state == RegistrationState.Progress) {
                return context.getString(R.string.status_in_progress);
            } else if (state == RegistrationState.Failed) {
                return context.getString(R.string.status_error);
            } else {
                return context.getString(R.string.status_not_connected);
            }
        } catch (Exception e) {
            Log.e(e);
        }

        return context.getString(R.string.status_not_connected);
    }

    public interface MenuClikedListener {
        void onMenuCliked();
    }

    private void goHomeAndClearStack() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
        try {
            startActivity(intent);
        } catch (IllegalStateException ise) {
            Log.e("[Main Activity] Can't start home activity: ", ise);
        }
    }

    private void quit() {
        goHomeAndClearStack();
        Core core = LinphoneManager.getCore();
        core.clearAllAuthInfo();
        if (LinphoneService.isReady()
                && LinphonePreferences.instance().getServiceNotificationVisibility()) {
            LinphoneService.instance().stopSelf();
        }
    }
}
