/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2011, Willow Garage, Inc.
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

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;

/**
 * Threaded control checker. Checks to see if the software is running and in a valid state.
 *
 * @author pratkanis@willowgarage.com
 */
public class ControlChecker {
  public interface SuccessHandler {
    /** Called on success with a description of the concert that got checked. */
    void handleSuccess();
  }
  public interface FailureHandler {
    /**
     * Called on failure with a short description of why it failed, like
     * "exception" or "timeout".
     */
    void handleFailure(String reason);
  }
  public interface EvictionHandler {
    /** Called to prompt the user to evict another user */
    boolean doEviction(String user);
  }
  public interface StartHandler {
    /** Called when starting the concert so that the user can be informed of possible delays */
    void handleStarting();
  }
  private CheckerThread checkerThread;
  private SuccessHandler concertReadyCallback;
  private FailureHandler failureCallback;
  private EvictionHandler evictionCallback;
  private StartHandler startCallback;
  private boolean doStart;
  /** Constructor. Should not take any time. Never starts or evicts. */
  public ControlChecker(SuccessHandler concertReadyCallback, FailureHandler failureCallback) {
    this.concertReadyCallback = concertReadyCallback;
    this.failureCallback = failureCallback;
    this.evictionCallback = new EvictionHandler() {
        public boolean doEviction(String user) { 
          return false; 
        }};
    this.startCallback = null;
    this.doStart = false;
  }
  /** Constructor. Should not take any time. */
  public ControlChecker(SuccessHandler concertReadyCallback, FailureHandler failureCallback, EvictionHandler evictionCallback) {
    this.concertReadyCallback = concertReadyCallback;
    this.failureCallback = failureCallback;
    this.evictionCallback = evictionCallback;
    this.startCallback = null;
    this.doStart = true;
  }
  /** Constructor. Should not take any time. */
  public ControlChecker(SuccessHandler concertReadyCallback, FailureHandler failureCallback, EvictionHandler evictionCallback, StartHandler startCallback) {
    this.concertReadyCallback = concertReadyCallback;
    this.failureCallback = failureCallback;
    this.evictionCallback = evictionCallback;
    this.startCallback = startCallback;
    this.doStart = true;
  }
  /**
   * Start the checker thread with the given concertId. If the thread is
   * already running, kill it first and then start anew. Returns immediately.
   */
  public void beginChecking(ConcertId concertId) {
    stopChecking();
    //If there's no wifi tag in the concert id, skip this step
    if (concertId.getControlUri() == null) {
      concertReadyCallback.handleSuccess();
      return;
    }
    checkerThread = new CheckerThread(concertId);
    checkerThread.start();
  }
  /** Stop the checker thread. */
  public void stopChecking() {
    if (checkerThread != null && checkerThread.isAlive()) {
      checkerThread.interrupt();
    }
  }
  private class CheckerThread extends Thread {
    private ConcertId concertId;
    public CheckerThread(ConcertId concertId) {
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
    private String getPage(String uri) {
      try {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet();
        request.setURI(new URI(uri));
        HttpResponse response = client.execute(request);
        BufferedReader in = new BufferedReader
          (new InputStreamReader(response.getEntity().getContent()));
        StringBuffer sb = new StringBuffer("");
        String line = "";
        String NL = System.getProperty("line.separator");
        while ((line = in.readLine()) != null) {
          sb.append(line + NL);
        }
        in.close();
        String page = sb.toString();
        return page;
      } catch (java.io.IOException ex) {
        Log.e("ControlChecker", "IOError: " + uri, ex);
      } catch (java.net.URISyntaxException ex) {
        Log.e("ControlChecker", "URI Invalid: " + uri, ex);
      }
      return null;
    }
    final String USER_TAG = "ACTIVE_USER:";
    final String VALID_USER = "applications";
    final String NO_USER = "None";
    private String getActiveUser() {
        String page = getPage(concertId.getControlUri() + "?action=GET_STATE");
        if (page == null) {
          return null;
        }
        String[] pageLines = page.split("\n");
        String activeUser = NO_USER;
     
        for (String i : pageLines) {
          if (i.trim().indexOf(USER_TAG) >= 0) {
            activeUser = i.trim().substring(USER_TAG.length() + 1).trim();
          }
        }
        return activeUser;
    }
    @Override
    public void run() {
      try {
        String activeUser = getActiveUser();
        Log.d("ControlChecker", "Active user: " + activeUser);
        if (activeUser == null) {
          failureCallback.handleFailure("Could not connect to the control page");
          return;
        }
        boolean goodState = false;
        boolean badUser = false;
        if (activeUser.equals(VALID_USER)) {
          goodState = true;
        } else if (!activeUser.equals(NO_USER)) {
          badUser = true;
        }
        if (goodState) {
          concertReadyCallback.handleSuccess();
        } else {
          if (badUser) {
            if (evictionCallback.doEviction(activeUser)) { //Prompt
              Log.d("ControlChecker", "Stopping concert");
              getPage(concertId.getControlUri() + "?action=STOP_CONCERT");
            } else {
              failureCallback.handleFailure("Need to evict current user inorder to connect");
              return;
            }
          }
          if (doStart) {
            if (startCallback != null) {
              startCallback.handleStarting();
            }
            Log.d("ControlChecker", "Starting concert");
            getPage(concertId.getControlUri() + "?action=START_CONCERT");
           
            int i = 0;
           
            while (i < 30 && !VALID_USER.equals(getActiveUser())) {
              i++;
              Thread.sleep(1000L);
            }
           
            if (VALID_USER.equals(getActiveUser())) {
              concertReadyCallback.handleSuccess();
            } else {
              failureCallback.handleFailure("Re-started the concert, but it is still not working");
            }
          } else {
            //Non-started concert
            failureCallback.handleFailure("Concert not started");
          }
        }
      } catch (Throwable ex) {
        Log.e("ControlChecker", "Exception while checking control URI "
              + concertId.getControlUri(), ex);
        failureCallback.handleFailure(ex.toString());
      }
    }
  }
}