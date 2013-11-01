package com.github.rosjava.android_remocons.concert_remocon.from_app_mng;

import android.util.Log;

import org.ros.master.client.MasterStateClient;
import org.ros.master.client.SystemState;
import org.ros.master.client.TopicSystemState;
import org.ros.namespace.GraphName;
import org.ros.namespace.NameResolver;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;

public class ConcertNameResolver extends AbstractNodeMain {

	private ConcertDescription currentConcert;
	private NameResolver applicationNamespaceResolver;
	private NameResolver concertNameResolver;
	private GraphName name;
	private GraphName applicationNamespace;
	private ConnectedNode connectedNode;
    private boolean resolved = false;

	public ConcertNameResolver() {
	}

	public void setConcert(ConcertDescription currentConcert) {
		this.currentConcert = currentConcert;
	}

	@Override
	public GraphName getDefaultNodeName() {
		return null;
	}

	public void setConcertName(String name) {
		this.name = GraphName.of(name);
	}
	
	public void resetConcertName(String name) {
		concertNameResolver = connectedNode.getResolver().newChild(name);
	}


	public NameResolver getAppNameSpace() {
		return applicationNamespaceResolver;
	}

    public NameResolver getConcertNameSpace() {
		return concertNameResolver;
	}

    /**
     * Call this to block until the resolver finishes its job.
     * i.e. after an execute is called to run the onStart method
     * below.
     *
     * Note - BLOCKING call!
     */
    public void waitForResolver() {
        while (!resolved) {
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                Log.w("ConcertRemocon", "concert name waitForResolver caught an arbitrary exception");
            }
        }
    }

	@Override
    /**
     * Resolves the namespace under which concert apps can be started
     * and stopped. Sometimes this will already have been provided
     * via setConcert() by managing applications (e.g. remocons) which
     * use the MasterChecker.
     *
     * In other cases, such as when the
     * application directly connects, we do a simple parameter
     * lookup, falling back to a default if provided.
     */
	public void onStart(final ConnectedNode connectedNode) {
		this.connectedNode = connectedNode;
        if (currentConcert != null) {
            name = GraphName.of(currentConcert.getConcertName());
        } else {
            // This is duplicated in PlatformInfoServiceClient and could be better stored somewhere, but it's not much.
            MasterStateClient masterClient = new MasterStateClient(this.connectedNode, this.connectedNode.getMasterUri());
            SystemState systemState = masterClient.getSystemState();
            for (TopicSystemState topic : systemState.getTopics()) {
                String name = topic.getTopicName();
                GraphName graph_name = GraphName.of(name);
                if ( graph_name.getBasename().toString().equals("app_list") ) {
                    this.name = graph_name.getParent().toRelative();
                    Log.i("ApplicationManagement", "configuring a concert namespace resolver [" + this.name + "]");
                    break;
                }
            }
        }
        applicationNamespace = name.join(GraphName.of("application"));  // hard coded, might we need to change this?
        applicationNamespaceResolver = connectedNode.getResolver().newChild(applicationNamespace);
        concertNameResolver = connectedNode.getResolver().newChild(name);
        resolved = true;
	}
}
