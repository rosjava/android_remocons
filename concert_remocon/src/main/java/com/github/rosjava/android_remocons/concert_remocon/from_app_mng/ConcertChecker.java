/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
 * Copyright (c) 2013, OSRF.
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials provided
 *    with the distribution.
 *  * Neither the name of Willow Garage, Inc. nor the names of its
 *    contributors may be used to endorse or promote products derived
 *    from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
 * LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN
 * ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.rosjava.android_remocons.concert_remocon.from_app_mng;

import android.util.Log;

import org.ros.address.InetAddressFactory;
import org.ros.android.NodeMainExecutorService;
import org.ros.internal.node.client.ParameterClient;
import org.ros.internal.node.server.NodeIdentifier;
import org.ros.namespace.GraphName;
import org.ros.node.NodeConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

import com.github.rosjava.android_apps.application_management.ConcertId;
import com.github.rosjava.android_apps.application_management.ConcertDescription;

/**
 * Threaded ROS-concert checker. Runs a thread which checks for a valid ROS
 * concert and sends back a {@link ConcertDescription} (with concert name and type)
 * on success or a failure reason on failure.
 *
 * @author hersh@willowgarage.com
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class ConcertChecker {
    public interface ConcertDescriptionReceiver {
        /**
         * Called on success with a description of the concert that got checked.
         */
        void receive(ConcertDescription concertDescription);
    }

    public interface FailureHandler {
        /**
         * Called on failure with a short description of why it failed, like
         * "exception" or "timeout".
         */
        void handleFailure(String reason);
    }

    private CheckerThread checkerThread;
    private ConcertDescriptionReceiver foundConcertCallback;
    private FailureHandler failureCallback;

    /**
     * Constructor. Should not take any time.
     */
    public ConcertChecker(ConcertDescriptionReceiver foundConcertCallback, FailureHandler failureCallback) {
        this.foundConcertCallback = foundConcertCallback;
        this.failureCallback = failureCallback;
    }

    /**
     * Start the checker thread with the given concertId. If the thread is
     * already running, kill it first and then start anew. Returns immediately.
     */
    public void beginChecking(ConcertId concertId) {
        stopChecking();
        if (concertId.getMasterUri() == null) {
            failureCallback.handleFailure("empty concert URI");
            return;
        }
        URI uri;
        try {
            uri = new URI(concertId.getMasterUri());
        } catch (URISyntaxException e) {
            failureCallback.handleFailure("invalid concert URI");
            return;
        }
        checkerThread = new CheckerThread(concertId, uri);
        checkerThread.start();
    }

    /**
     * Stop the checker thread.
     */
    public void stopChecking() {
        if (checkerThread != null && checkerThread.isAlive()) {
            checkerThread.interrupt();
        }
    }

    private class CheckerThread extends Thread {
        private URI concertUri;
        private ConcertId concertId;

        public CheckerThread(ConcertId concertId, URI concertUri) {
            this.concertUri = concertUri;
            this.concertId = concertId;
            setDaemon(true);
            // don't require callers to explicitly kill all the old checker threads.
            setUncaughtExceptionHandler(new UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable ex) {
                    failureCallback.handleFailure("exception: " + ex.getMessage());
                }
            });
        }

        @Override
        public void run() {
            try {
                // Check if the concert exists by looking for /concert/name parameter
                // getParam throws when it can't find the parameter.
                ParameterClient paramClient = new ParameterClient(
                        NodeIdentifier.forNameAndUri("/concert_checker", concertUri.toString()), concertUri);
                String name = (String) paramClient.getParam(GraphName.of("/concert/name")).getResult();
                Log.i("ConcertRemocon", "Concert " + name + " found; retrieve additional information");

                NodeMainExecutorService nodeMainExecutorService = new NodeMainExecutorService();
                NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                        InetAddressFactory.newNonLoopback().getHostAddress(), concertUri);

                // Check for the concert information topic (/concert/info)
                ListenerNode<concert_msgs.ConcertInfo> readInfoTopic =
                        new ListenerNode("/concert/info", concert_msgs.ConcertInfo._TYPE);
                nodeMainExecutorService.execute(readInfoTopic, nodeConfiguration.setNodeName("read_info_node"));
                readInfoTopic.waitForResponse();


                concert_msgs.ConcertInfo concertInfo = readInfoTopic.getLastMessage();

                String              concertName = concertInfo.getName();
                String              concertDesc = concertInfo.getDescription();
                rocon_std_msgs.Icon concertIcon = concertInfo.getIcon();

                if (name.equals(concertName) == false)
                    Log.w("ConcertRemocon", "Concert names from parameter and topic differs; we use the later");

                // Check for the concert roles topic (/concert/roles)
                ListenerNode<concert_msgs.Roles> readRolesTopic =
                        new ListenerNode("/concert/interactions/roles", concert_msgs.Roles._TYPE);
                nodeMainExecutorService.execute(readRolesTopic, nodeConfiguration.setNodeName("concert_roles_node"));
                readRolesTopic.waitForResponse();

                nodeMainExecutorService.shutdownNodeMain(readInfoTopic);
                nodeMainExecutorService.shutdownNodeMain(readRolesTopic);

                // configure concert description
                Date timeLastSeen = new Date();
                ConcertDescription description = new ConcertDescription(concertId, concertName, concertDesc, concertIcon, timeLastSeen);
                Log.i("ConcertRemocon", "Concert is available");
                description.setConnectionStatus(ConcertDescription.OK);
                description.setUserRoles(readRolesTopic.getLastMessage());
                foundConcertCallback.receive(description);
                return;
            } catch ( RuntimeException e) {
                // thrown if concert could not be found in the getParam call (from java.net.ConnectException)
                Log.w("ConcertRemocon", "could not find concert [" + concertUri + "][" + e.toString() + "]");
                failureCallback.handleFailure(e.toString());
            } catch (Throwable e) {
                Log.w("ConcertRemocon", "exception while creating node in concert checker for URI " + concertUri, e);
                failureCallback.handleFailure(e.toString());
            }
        }
    }
}