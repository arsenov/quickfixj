package quickfix.test.acceptance;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Iterator;

import junit.framework.Assert;

import org.apache.mina.common.CloseFuture;
import org.apache.mina.common.ConnectFuture;
import org.apache.mina.common.IoConnector;
import org.apache.mina.common.IoHandlerAdapter;
import org.apache.mina.common.IoSession;
import org.apache.mina.common.TransportType;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.transport.socket.nio.SocketConnector;
import org.apache.mina.transport.vmpipe.VmPipeAddress;
import org.apache.mina.transport.vmpipe.VmPipeConnector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import quickfix.mina.message.FIXProtocolCodecFactory;
import edu.emory.mathcs.backport.java.util.concurrent.BlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.CountDownLatch;
import edu.emory.mathcs.backport.java.util.concurrent.LinkedBlockingQueue;
import edu.emory.mathcs.backport.java.util.concurrent.TimeUnit;

public class TestConnection {
    private Logger log = LoggerFactory.getLogger(getClass());
    private HashMap ioHandlers = new HashMap();

    public void sendMessage(int clientId, String message) throws IOException {
        TestIoHandler handler = getIoHandler(clientId);
        handler.getSession().write(message);
    }

    private TestIoHandler getIoHandler(int clientId) {
        return (TestIoHandler) ioHandlers.get(new Integer(clientId));
    }

    public void tearDown() {
        Iterator handlerItr = ioHandlers.values().iterator();
        while (handlerItr.hasNext()) {
            CloseFuture closeFuture = ((TestIoHandler) handlerItr.next()).getSession().close();
            closeFuture.join();
        }
        ioHandlers.clear();
    }

    public CharSequence readMessage(int clientId, long timeout) throws InterruptedException {
        return getIoHandler(clientId).getNextMessage(timeout);
    }

    public void waitForClientDisconnect(int clientId) throws IOException, InterruptedException {
        getIoHandler(clientId).waitForDisconnect();
    }

    public void connect(int clientId, TransportType transportType, int port)
            throws UnknownHostException, IOException {
        IoConnector connector;
        SocketAddress address;
        if (transportType == TransportType.SOCKET) {
            connector = new SocketConnector();
            address = new InetSocketAddress("localhost", port);
        } else if (transportType == TransportType.VM_PIPE) {
            connector = new VmPipeConnector();
            address = new VmPipeAddress(port);
        } else {
            throw new RuntimeException("Unsupported transport type: " + transportType);
        }
        TestIoHandler testIoHandler = new TestIoHandler();
        ioHandlers.put(new Integer(clientId), testIoHandler);
        ConnectFuture future = connector.connect(address, testIoHandler);
        future.join();
        Assert.assertTrue("connection to server failed", future.isConnected());
    }

    private class TestIoHandler extends IoHandlerAdapter {
        private IoSession session;
        private BlockingQueue messages = new LinkedBlockingQueue();
        private CountDownLatch disconnectLatch = new CountDownLatch(1);

        public void sessionCreated(IoSession session) throws Exception {
            super.sessionCreated(session);
            this.session = session;
            session.getFilterChain().addLast("codec",
                    new ProtocolCodecFilter(new FIXProtocolCodecFactory()));
        }

        
        public void exceptionCaught(IoSession session, Throwable cause) throws Exception {
            super.exceptionCaught(session, cause);
            log.error(cause.getMessage(), cause);
        }

        public void sessionClosed(IoSession session) throws Exception {
            super.sessionClosed(session);
            disconnectLatch.countDown();
        }

        public void messageReceived(IoSession session, Object message) throws Exception {
            messages.add(message);
        }

        public IoSession getSession() {
            return session;
        }

        public String getNextMessage(long timeout) throws InterruptedException {
            return (String) messages.poll(timeout, TimeUnit.MILLISECONDS);
        }

        public void waitForDisconnect() throws InterruptedException {
            if (!disconnectLatch.await(500000L, TimeUnit.MILLISECONDS)) {
                Assert.fail("client not disconnected");
            }
        }
    }
}