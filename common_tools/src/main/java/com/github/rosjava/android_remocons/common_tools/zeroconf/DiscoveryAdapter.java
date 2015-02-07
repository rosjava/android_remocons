/*
 * Copyright (C) 2013 Yujin Robot.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.github.rosjava.android_remocons.common_tools.zeroconf;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Checkable;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.github.rosjava.android_remocons.common_tools.R;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.DiscoveredService;

import java.util.ArrayList;


public class DiscoveryAdapter extends ArrayAdapter<DiscoveredService> {

    /**
     * This class is necessary to work a checkbox well
     */

    public class CustomCheckBox extends LinearLayout implements Checkable {
        private CheckedTextView checkbox;

        public CustomCheckBox(Context context) {
            super(context);

            View view = ((LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE))
                                                 .inflate(R.layout.zeroconf_master_item, this, false);
            checkbox = (CheckedTextView) view.findViewById(R.id.service_detail);
            addView(view);
        }


        @Override
        public void setChecked(boolean checked) {
            checkbox.setChecked(checked);

        }

        @Override
        public boolean isChecked() {
            return checkbox.isChecked();
        }

        @Override
        public void toggle() {
            setChecked(!isChecked());

        }
    }


    private final Context context;
    private ArrayList<DiscoveredService> discoveredServices;
    private String targetServiceName;
    private int targetServiceDrawable;
    private int otherServicesDrawable;

    public DiscoveryAdapter(Context context, ArrayList<DiscoveredService> discoveredServices,
                            String targetServiceName, int targetServiceDrawable, int otherServicesDrawable) {
        super(context, R.layout.zeroconf_master_item, discoveredServices); // pass the list to the super
        this.context = context;
        this.discoveredServices = discoveredServices;  // keep a pointer locally so we can play with it
        this.targetServiceName     = targetServiceName;
        this.targetServiceDrawable = targetServiceDrawable;
        this.otherServicesDrawable = otherServicesDrawable;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = convertView;
        if (v == null) {
            v = new CustomCheckBox(getContext());
        }

        DiscoveredService discovered_service = discoveredServices.get(position);
        if (discovered_service != null) {
            TextView tt = (TextView) v.findViewById(R.id.service_name);
            TextView bt = (TextView) v.findViewById(R.id.service_detail);
            if (tt != null) {
                tt.setText(discovered_service.name);
            }
            if (bt != null) {
                String result = "";
                for (String ipv4_address : discovered_service.ipv4_addresses) {
                    if (result.equals("")) {
                        result += ipv4_address + ":" + discovered_service.port;
                    } else {
                        result += "\n" + ipv4_address + ":" + discovered_service.port;
                    }
                }
                for (String ipv6_address : discovered_service.ipv6_addresses) {
                    if (result.equals("")) {
                        result += ipv6_address + ":" + discovered_service.port;
                    } else {
                        result += "\n" + ipv6_address + ":" + discovered_service.port;
                    }
                }
                bt.setText(result);
            }
            ImageView im = (ImageView) v.findViewById(R.id.icon);
            if (im != null) {
                if (discovered_service.type.indexOf("_" + targetServiceName + "._tcp") != -1 ||
                    discovered_service.type.indexOf("_" + targetServiceName + "._udp") != -1) {
                    im.setImageDrawable(context.getResources().getDrawable(targetServiceDrawable));
                } else {
                    im.setImageDrawable(context.getResources().getDrawable(otherServicesDrawable));
                }
            }
        }
        return v;
    }
}
