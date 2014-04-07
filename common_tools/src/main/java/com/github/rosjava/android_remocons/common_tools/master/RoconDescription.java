/*
 * Software License Agreement (BSD License)
 *
 * Copyright (c) 2013, Yujin Robot.
 *
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

package com.github.rosjava.android_remocons.common_tools.master;

import java.util.Date;
import java.util.List;

/**
 * Extends MasterDescription with concert specific attributes.
 * On concerts, apps namespace must be empty.
 *
 * @author jorge@yujinrobot.com (Jorge Santos Simon)
 */
public class RoconDescription extends MasterDescription implements java.io.Serializable {
    private static final long serialVersionUID = -4705526306056241179L;

    private String description;
    private String[] userRoles;
    private int currentRole = -1;
    private String interactionsNamespace;

    public static RoconDescription create(MasterDescription master) {
        RoconDescription cd = new RoconDescription(master.getMasterId(), master.getMasterName(),
                                                       null, null, null, new Date());
        cd.setMasterIconFormat(master.getMasterIconFormat());
        cd.setMasterIconData(master.getMasterIconData());
        return cd;
    }

    public static RoconDescription createUnknown(MasterId masterId) {
        return new RoconDescription(masterId, NAME_UNKNOWN, null, null, null, new Date());
    }

    /**
     * Empty constructor required by snake yaml parsing
     */
    public RoconDescription() {
    }

    public RoconDescription(MasterId masterId, String concertName, String description,
                            rocon_std_msgs.Icon concertIcon, String interactionsNamespace,
                            Date timeLastSeen) {
        super(masterId, concertName, "Rocon concert", concertIcon, "", timeLastSeen);

        this.description = description;
        this.interactionsNamespace = interactionsNamespace;
    }

    public void copyFrom(RoconDescription other) {
        super.copyFrom(other);

        this.userRoles = other.userRoles.clone();
        this.description = other.description;
        this.interactionsNamespace = other.interactionsNamespace;
    }

    public String getInteractionsNamespace() { return this.interactionsNamespace; }

    public String[] getUserRoles()  {
        return userRoles;
    }

    public String getCurrentRole()  {
        if (userRoles != null && currentRole >= 0 && currentRole <  userRoles.length)
            return userRoles[currentRole];
        else
            return null;
    }

    public void setInteractionsNamespace(String namespace) {
        this.interactionsNamespace = namespace;
    }

    public void setUserRoles(List<String> roles)
    {
        userRoles = new String[roles.size()];
        roles.toArray(userRoles);
    }
    public void setCurrentRole(int role) {
        currentRole = role;
    }
}
