/*
 * Copyright (c) 2021 Oracle and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.helidon.webserver.examples.rsocket;

import io.helidon.webserver.rsocket.HelidonDuplexConnection;
import io.helidon.webserver.rsocket.RoutedRSocket;
import io.netty.buffer.ByteBuf;
import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.metadata.CompositeMetadata;
import io.rsocket.metadata.CompositeMetadata.Entry;
import io.rsocket.metadata.RoutingMetadata;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.transport.ServerTransport.ConnectionAcceptor;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import javax.websocket.CloseReason;
import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.Session;

public class RSocketEndpoint extends Endpoint {

    final ConnectionAcceptor connectionAcceptor;
    final Map<String, HelidonDuplexConnection> connections = new ConcurrentHashMap<>();

    public RSocketEndpoint() {
        this.connectionAcceptor = RSocketServer
                .create()
                .acceptor(new SocketAcceptor() {
                    @Override
                    public Mono<RSocket> accept(ConnectionSetupPayload connectionSetupPayload, RSocket rSocket) {

                        final String defaultMimeType = connectionSetupPayload.dataMimeType(); // octet / json / ...

                        Optional<String> connectionRouteOpt = extractRoute(connectionSetupPayload.metadata());
                        // http headers
                        return Mono.just(RoutedRSocket.builder()
                                .addRequestResponse("print", PrintRequestResponseHandler.class.getCanonicalName()).build());
                    }

                })
                .asConnectionAcceptor();
    }


    @Override
    public void onOpen(Session session, EndpointConfig endpointConfig) {
        final HelidonDuplexConnection connection = new HelidonDuplexConnection(session);
        connections.put(session.getId(), connection);
        connectionAcceptor.apply(connection).subscribe();
    }

    @Override
    public void onClose(Session session, CloseReason closeReason) {
        connections.get(session.getId()).onCloseSink.tryEmitEmpty();
    }

    static Optional<String> extractRoute(ByteBuf metadata) {
        final CompositeMetadata compositeMetadata = new CompositeMetadata(metadata, false);

        for (Entry compositeMetadatum : compositeMetadata) {
            final String key = compositeMetadatum.getMimeType();
            final ByteBuf payload = compositeMetadatum.getContent();


            if (WellKnownMimeType.MESSAGE_RSOCKET_ROUTING.getString().equals(key)) {
                final RoutingMetadata routes = new RoutingMetadata(payload);

                for (String route : routes) {
                    //registry.findRoute(route);
                    return Optional.of(route);
                }
            }

            return Optional.empty();
        }
        return Optional.empty();
    }
}