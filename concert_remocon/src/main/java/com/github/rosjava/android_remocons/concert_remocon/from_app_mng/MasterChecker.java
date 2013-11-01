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
import org.ros.exception.ServiceNotFoundException;
import org.ros.node.NodeConfiguration;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;

/**
 * Threaded ROS-master checker. Runs a thread which checks for a valid ROS
 * master and sends back a {@link ConcertDescription} (with concert name and type)
 * on success or a failure reason on failure.
 *
 * @author hersh@willowgarage.com
 * @author murase@jsk.imi.i.u-tokyo.ac.jp (Kazuto Murase)
 */
public class MasterChecker {
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
    private ConcertDescriptionReceiver foundMasterCallback;
    private FailureHandler failureCallback;

    /**
     * Constructor. Should not take any time.
     */
    public MasterChecker(ConcertDescriptionReceiver foundMasterCallback, FailureHandler failureCallback) {
        this.foundMasterCallback = foundMasterCallback;
        this.failureCallback = failureCallback;
    }

    /**
     * Start the checker thread with the given concertId. If the thread is
     * already running, kill it first and then start anew. Returns immediately.
     */
    public void beginChecking(ConcertId concertId) {
        stopChecking();
        if (concertId.getMasterUri() == null) {
            failureCallback.handleFailure("empty master URI");
            return;
        }
        URI uri;
        try {
            uri = new URI(concertId.getMasterUri());
        } catch (URISyntaxException e) {
            failureCallback.handleFailure("invalid master URI");
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
        private URI masterUri;
        private ConcertId concertId;

        public CheckerThread(ConcertId concertId, URI masterUri) {
            this.masterUri = masterUri;
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
                // Check if the master exists - no really good way in rosjava except by checking a standard parameter.
//                ParameterClient paramClient = new ParameterClient(
//                        NodeIdentifier.forNameAndUri("/master_checker", masterUri.toString()), masterUri);
                // getParam throws when it can't find the parameter.
//                String unused_rosversion = (String) paramClient.getParam(GraphName.of("rosversion")).getResult();

                // Check for the platform information - be sure to check that master exists first otherwise you'll
                // start a thread which perpetually crashes and triest to re-register in .execute()
                NodeMainExecutorService nodeMainExecutorService = new NodeMainExecutorService();
                NodeConfiguration nodeConfiguration = NodeConfiguration.newPublic(
                        InetAddressFactory.newNonLoopback().getHostAddress(),
                        masterUri);

                PlatformInfoServiceClient srvClient = new PlatformInfoServiceClient();
                nodeMainExecutorService.execute(srvClient, nodeConfiguration.setNodeName("platform_info_client_node"));
                srvClient.waitForResponse();
                String concertName = srvClient.getUniqueName();
                String concertType = srvClient.getConcertType();
                rocon_std_msgs.Icon concertIcon = srvClient.getConcertIcon();

                ListenerNode<concert_msgs.Roles> readTopic =
                        new ListenerNode(srvClient.getConcertNamespace() + "/roles", concert_msgs.Roles._TYPE);
                nodeMainExecutorService.execute(readTopic, nodeConfiguration.setNodeName("concert_roles_node"));
                readTopic.waitForResponse();

                nodeMainExecutorService.shutdownNodeMain(srvClient);
                nodeMainExecutorService.shutdownNodeMain(readTopic);

                // configure concert description
                Date timeLastSeen = new Date();
                ConcertDescription concertDescription = new ConcertDescription(concertId, concertName, concertType, concertIcon, timeLastSeen);
                Log.i("ConcertRemocon", "rapp manager is available");
                concertDescription.setConnectionStatus(ConcertDescription.OK);
                concertDescription.setUserRoles(readTopic.getLastMessage());
                foundMasterCallback.receive(concertDescription);
                return;
            } catch ( RuntimeException e) {
                // thrown if master could not be found in the getParam call (from java.net.ConnectException)
                Log.w("ConcertRemocon", "could not find the master [" + masterUri + "][" + e.toString() + "]");
                failureCallback.handleFailure(e.toString());
            } catch (ServiceNotFoundException e) {
                // thrown by client.waitForResponse() if it times out
                Log.w("ConcertRemocon", e.getMessage()); // e.getMessage() is a little less verbose (no org.ros.exception.ServiceNotFoundException prefix)
                failureCallback.handleFailure(e.getMessage());  // don't need the master uri, it's already shown above in the concert description from input method.
            } catch (Throwable e) {
                Log.w("ConcertRemocon", "exception while creating node in masterchecker for master URI "
                        + masterUri, e);
                failureCallback.handleFailure(e.toString());
            }
        }
    }
}