package com.github.rosjava.android_remocons.common_tools;

import org.ros.android.MessageCallable;
import org.ros.exception.RosException;
import org.ros.exception.RosRuntimeException;
import org.ros.message.MessageListener;
import org.ros.namespace.GraphName;
import org.ros.node.AbstractNodeMain;
import org.ros.node.ConnectedNode;
import org.ros.node.topic.Subscriber;


/**
 * Created by jorge on 10/30/13.
 */
public class ListenerNode<T> extends AbstractNodeMain
{
    private String topicName;
    private String messageType;
    private T      lastMessage;
    private MessageCallable<String, T> callable;

    public ListenerNode(String topic, String type)
    {
        topicName = topic;
        messageType = type;
    }

    public T getLastMessage()
    {
        return lastMessage;
    }

    public void setTopicName(String topicName)
    {
        this.topicName = topicName;
    }

    public void setMessageType(String messageType)
    {
        this.messageType = messageType;
    }

    public void setMessageToStringCallable(MessageCallable<String, T> callable)
    {
        this.callable = callable;
    }

    @Override
    public GraphName getDefaultNodeName() {
        return GraphName.of("get_" + topicName + "_node");
    }

    @Override
    public void onStart(ConnectedNode connectedNode) {
        Subscriber<T> subscriber = connectedNode.newSubscriber(topicName, messageType);
        subscriber.addMessageListener(new MessageListener<T>() {
            @Override
            public void onNewMessage(final T message) {
                lastMessage = message;
                if (callable != null) {
                    callable.call(message);
                }
            }
        });
    }

    /**
     * Utility function to block until subscriber receives the first message.
     *
     * @throws org.ros.exception.RosException : when it times out waiting for the service.
     */
    public void waitForResponse() throws RosException {
        int count = 0;
        while ( lastMessage == null ) {
            try {
                Thread.sleep(200);
            } catch (Exception e) {
                throw new RosRuntimeException(e);
            }
            if ( count == 20 ) {  // timeout.
                throw new RosException("timed out waiting for topic messages");
            }
            count = count + 1;
        }
    }
}
