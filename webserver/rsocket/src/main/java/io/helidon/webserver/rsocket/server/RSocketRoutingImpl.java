package io.helidon.webserver.rsocket.server;

import io.helidon.webserver.rsocket.FireAndForgetHandler;
import io.helidon.webserver.rsocket.RequestChannelHandler;
import io.helidon.webserver.rsocket.RequestResponseHandler;
import io.helidon.webserver.rsocket.RequestStreamHandler;

import java.util.Map;

public class RSocketRoutingImpl implements RSocketRouting {

    private final Map<String, RequestResponseHandler> requestResponseRoutes;
    private final Map<String, FireAndForgetHandler> fireAndForgetRoutes;
    private final Map<String, RequestStreamHandler> requestStreamRoutes;
    private final Map<String, RequestChannelHandler> requestChannelRoutes;


    public RSocketRoutingImpl(Map<String, RequestResponseHandler> requestResponseRoutes,
                              Map<String, FireAndForgetHandler> fireAndForgetRoutes,
                              Map<String, RequestStreamHandler> requestStreamRoutes,
                              Map<String, RequestChannelHandler> requestChannelRoutes) {
        this.requestResponseRoutes = requestResponseRoutes;
        this.fireAndForgetRoutes = fireAndForgetRoutes;
        this.requestStreamRoutes = requestStreamRoutes;
        this.requestChannelRoutes = requestChannelRoutes;
    }

    @Override
    public Map<String, RequestResponseHandler> getRequestResponseRoutes() {
        return requestResponseRoutes;
    }

    @Override
    public Map<String, FireAndForgetHandler> getFireAndForgetRoutes() {
        return fireAndForgetRoutes;
    }

    @Override
    public Map<String, RequestStreamHandler> getRequestStreamRoutes() {
        return requestStreamRoutes;
    }

    @Override
    public Map<String, RequestChannelHandler> getRequestChannelRoutes() {
        return requestChannelRoutes;
    }

    @Override
    public String toString() {
        return "RSocketRoutingImpl{" +
                "requestResponseRoutes=" + requestResponseRoutes +
                ", fireAndForgetRoutes=" + fireAndForgetRoutes +
                ", requestStreamRoutes=" + requestStreamRoutes +
                ", requestChannelRoutes=" + requestChannelRoutes +
                '}';
    }
}
