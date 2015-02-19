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
import android.widget.ListView;

import com.github.rosjava.zeroconf_jmdns_suite.jmdns.DiscoveredService;
import com.github.rosjava.zeroconf_jmdns_suite.jmdns.Zeroconf;

import java.io.IOException;
import java.util.ArrayList;


public class MasterSearcher {

    private Zeroconf zeroconf;
    private ArrayList<DiscoveredService> discoveredMasters;
    private DiscoveryAdapter discoveryAdapter;
    private DiscoveryHandler discoveryHandler;
    private Logger logger;

    public MasterSearcher(Context context, final ListView listView,
                          String targetServiceName, int targetServiceDrawable, int otherServicesDrawable) {

        discoveredMasters = new ArrayList<DiscoveredService>();

        discoveryAdapter = new DiscoveryAdapter(context, discoveredMasters,
                                 targetServiceName, targetServiceDrawable, otherServicesDrawable);
        listView.setAdapter(discoveryAdapter);
        listView.setItemsCanFocus(false);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);

        logger = new Logger();
        zeroconf = new Zeroconf(logger);
        discoveryHandler = new DiscoveryHandler(discoveryAdapter, discoveredMasters);
        zeroconf.setDefaultDiscoveryCallback(discoveryHandler);

        new DiscoverySetup(context).execute(zeroconf);
    }

    public void shutdown() {
        try {
            zeroconf.shutdown();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
