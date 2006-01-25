/*******************************************************************************
 * * Copyright (c) 2001-2005 quickfixengine.org All rights reserved. * * This
 * file is part of the QuickFIX FIX Engine * * This file may be distributed
 * under the terms of the quickfixengine.org * license as defined by
 * quickfixengine.org and appearing in the file * LICENSE included in the
 * packaging of this file. * * This file is provided AS IS with NO WARRANTY OF
 * ANY KIND, INCLUDING THE * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE. * * See http://www.quickfixengine.org/LICENSE for
 * licensing information. * * Contact ask@quickfixengine.org if any conditions
 * of this licensing are * not clear to you. *
 ******************************************************************************/

package quickfix;

import quickfix.mina.SingleThreadedEventHandlingStrategy;
import quickfix.mina.initiator.AbstractSocketInitiator;

public class SocketInitiator extends AbstractSocketInitiator {
    private volatile Boolean isStarted = Boolean.FALSE;

    public SocketInitiator(Application application, MessageStoreFactory messageStoreFactory,
                SessionSettings settings, MessageFactory messageFactory) throws ConfigError {
        super(application, messageStoreFactory, settings, new ScreenLogFactory(settings),
                messageFactory);
        // This exception is thrown for compatibility reasons
        if (settings == null) {
            throw new ConfigError("no settings");
        }
    }

    public SocketInitiator(Application application, MessageStoreFactory messageStoreFactory,
            SessionSettings settings, LogFactory logFactory, MessageFactory messageFactory)
            throws ConfigError {
        super(application, messageStoreFactory, settings, logFactory, messageFactory);
        // This exception is thrown for compatibility reasons
        if (settings == null) {
            throw new ConfigError("no settings");
        }
    }

    public SocketInitiator(SessionFactory sessionFactory, SessionSettings settings) throws ConfigError {
        super(settings, sessionFactory);
    }

    private SingleThreadedEventHandlingStrategy eventHandlingStrategy =
        new SingleThreadedEventHandlingStrategy(this);

    public void block() throws ConfigError, RuntimeError {
        initialize();
        eventHandlingStrategy.block();
    }

    public boolean poll() throws ConfigError, RuntimeError {
        initialize();
        return eventHandlingStrategy.poll();
    }
    
    public void start() throws ConfigError, RuntimeError {
        initialize();
    }
    
    public void stop() {
        stop(false);
    }

    public void stop(boolean forceDisconnect) {
        eventHandlingStrategy.stopHandlingMessages();
        logoutAllSessions(forceDisconnect);
    }

    private void initialize() throws ConfigError {
        synchronized (isStarted) {
            if (isStarted == Boolean.FALSE) {
                initiateSessions(eventHandlingStrategy);
            }
            isStarted = Boolean.TRUE;
        }
    }
}