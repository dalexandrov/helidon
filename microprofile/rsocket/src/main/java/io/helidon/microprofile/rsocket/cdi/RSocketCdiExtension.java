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

package io.helidon.microprofile.rsocket.cdi;

import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.config.Config;
import io.helidon.microprofile.cdi.RuntimeStart;
import io.helidon.microprofile.server.RoutingName;
import io.helidon.microprofile.server.RoutingPath;
import io.helidon.microprofile.server.ServerCdiExtension;
import io.helidon.webserver.Routing;
import io.helidon.webserver.rsocket.FireAndForgetHandler;
import io.helidon.webserver.rsocket.RSocketEndpoint;
import io.helidon.webserver.rsocket.RSocketSupport;
import io.helidon.webserver.rsocket.RequestChannelHandler;
import io.helidon.webserver.rsocket.RequestResponseHandler;
import io.helidon.webserver.rsocket.RequestStreamHandler;
import io.helidon.webserver.rsocket.server.RSocketRouting;
import io.rsocket.Payload;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.UnsatisfiedResolutionException;
import javax.enterprise.inject.spi.AnnotatedMethod;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.WithAnnotations;
import javax.websocket.server.ServerApplicationConfig;
import javax.websocket.server.ServerEndpointConfig;

import static javax.interceptor.Interceptor.Priority.PLATFORM_AFTER;

/**
 * Configure RSocket related things.
 */
public class RSocketCdiExtension implements Extension {
    private static final Logger LOGGER = Logger.getLogger(RSocketCdiExtension.class.getName());

    private static final String DEFAULT_RSOCKET_PATH = "/";

    private Config config;

    private ServerCdiExtension serverCdiExtension;

    private final RSocketApplication.Builder appBuilder = RSocketApplication.builder();

    private void prepareRuntime(@Observes @RuntimeStart Config config) {
        this.config = config;
    }

    private void startServer(@Observes @Priority(PLATFORM_AFTER + 100) @Initialized(ApplicationScoped.class) Object event,
                             BeanManager beanManager) {
        serverCdiExtension = beanManager.getExtension(ServerCdiExtension.class);
        registerRSockets();
    }

    /**
     * Collect application class extending {@code ServerApplicationConfig}.
     *
     * @param applicationClass Application class.
     */
    private void applicationClass(@Observes ProcessAnnotatedType<? extends ServerApplicationConfig> applicationClass) {
        LOGGER.finest(() -> "Application class found " + applicationClass.getAnnotatedType().getJavaClass());
        appBuilder.applicationClass(applicationClass.getAnnotatedType().getJavaClass());
    }

    /**
     * Overrides a rsocket application class.
     *
     * @param applicationClass Application class.
     */
    public void applicationClass(Class<? extends ServerApplicationConfig> applicationClass) {
        LOGGER.finest(() -> "Using manually set application class  " + applicationClass);
        appBuilder.updateApplicationClass(applicationClass);
    }

    /**
     * Collect annotated endpoints.
     *
     * @param endpoint The endpoint.
     */
    private void endpointClasses(@Observes @WithAnnotations(RSocket.class) ProcessAnnotatedType<?> endpoint) {
        LOGGER.info(() -> "Annotated endpoint found " + endpoint.getAnnotatedType().getJavaClass());

        LOGGER.info("Methods:");
        List<Method> methods = endpoint.getAnnotatedType().getMethods().stream().map(AnnotatedMethod::getJavaMember).collect(Collectors.toList());

        RSocketRouting.Builder rSocketRoutingBuilder = RSocketRouting.builder();


        for (Method method : methods) {
            LOGGER.info("Method: " + method.getName());
            LOGGER.info("Has the following annotations");
            for (Annotation annotation : method.getAnnotations()) {
                LOGGER.info(" - " + annotation.toString());

                if (annotation.annotationType().equals(FireAndForget.class)) {
                    rSocketRoutingBuilder.fireAndForget(
                            payload -> {
                                try {
                                    return (Single<Void>) method.invoke(payload);
                                } catch (IllegalAccessException e) {
                                    e.printStackTrace();
                                } catch (InvocationTargetException e) {
                                    e.printStackTrace();
                                }
                                return Single.empty();
                            });
                } else if (annotation.annotationType().equals(RequestChannel.class)) {
                    rSocketRoutingBuilder.requestChannel(payloads -> {
                        try {
                            return (Multi<Payload>) method.invoke(payloads);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        return Multi.empty();
                    });
                } else if (annotation.annotationType().equals(RequestResponse.class)) {
                    rSocketRoutingBuilder.requestResponse(payload -> {
                        try {
                            return (Single<Payload>) method.invoke(payload);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        return Single.empty();
                    });
                } else if (annotation.annotationType().equals(RequestStream.class)){
                    rSocketRoutingBuilder.requestStream(payload -> {
                        try {
                            return (Multi<Payload>) method.invoke(payload);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                        return Multi.empty();
                    });
                }
            }
        }

        RSocketRouting rSocketRouting = rSocketRoutingBuilder.build();

        LOGGER.info("ROUTING: "+rSocketRouting.toString());

        ServerCdiExtension serverCdiExtension = CDI.current().getBeanManager().getExtension(ServerCdiExtension.class);
        serverCdiExtension.serverRoutingBuilder().register("/rsocket",
                RSocketSupport.builder()
                        .register(RSocketEndpoint.create(rSocketRouting, "/board")
                                .getEndPoint()
                        ).build());

        appBuilder.annotatedEndpoint(endpoint.getAnnotatedType().getJavaClass());
    }

    /**
     * Provides access to RSocket application.
     *
     * @return Application.
     */
    RSocketApplication toRSocketApplication() {
        return appBuilder.build();
    }

    private void registerRSockets() {
        try {
            RSocketApplication app = toRSocketApplication();

            // If application present call its methods
            RSocketSupport.Builder builder = RSocketSupport.builder();
            Optional<Class<? extends ServerApplicationConfig>> appClass = app.applicationClass();

            Optional<String> contextRoot = appClass.flatMap(c -> findContextRoot(config, c));
            Optional<String> namedRouting = appClass.flatMap(c -> findNamedRouting(config, c));
            boolean routingNameRequired = appClass.map(c -> isNamedRoutingRequired(config, c)).orElse(false);

            Routing.Builder routing;
            if (appClass.isPresent()) {
                Class<? extends ServerApplicationConfig> c = appClass.get();

                // Attempt to instantiate via CDI
                ServerApplicationConfig instance = null;
                try {
                    instance = CDI.current().select(c).get();
                } catch (UnsatisfiedResolutionException e) {
                    // falls through
                }

                // Otherwise, we create instance directly
                if (instance == null) {
                    try {
                        instance = c.getDeclaredConstructor().newInstance();
                    } catch (Exception e) {
                        throw new RuntimeException("Unable to instantiate rsocket application " + c, e);
                    }
                }

                // Call methods in application class
                Set<ServerEndpointConfig> endpointConfigs = instance.getEndpointConfigs(app.programmaticEndpoints());
                Set<Class<?>> endpointClasses = instance.getAnnotatedEndpointClasses(app.annotatedEndpoints());

                // Register classes and configs
                endpointClasses.forEach(builder::register);
                endpointConfigs.forEach(builder::register);

                // Create routing builder
                routing = serverCdiExtension.routingBuilder(namedRouting, routingNameRequired, c.getName());
            } else {
                // Direct registration without calling application class
                app.annotatedEndpoints().forEach(builder::register);
                app.programmaticEndpoints().forEach(builder::register);

                // Create routing builder
                routing = serverCdiExtension.serverRoutingBuilder();
            }

            // Finally register RSockets in Helidon routing
            String rootPath = contextRoot.orElse(DEFAULT_RSOCKET_PATH);
            LOGGER.info("Registering RSocket application at " + rootPath);
            routing.register(rootPath, new RSocketSupportMp(builder.build()));
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Unable to load RSocket extension", e);
        }
    }

    private Optional<String> findContextRoot(Config config,
                                             Class<? extends ServerApplicationConfig> applicationClass) {
        return config.get(applicationClass.getName() + "." + RoutingPath.CONFIG_KEY_PATH)
                .asString()
                .or(() -> Optional.ofNullable(applicationClass.getAnnotation(RoutingPath.class))
                        .map(RoutingPath::value))
                .map(path -> path.startsWith("/") ? path : ("/" + path));
    }

    private Optional<String> findNamedRouting(Config config,
                                              Class<? extends ServerApplicationConfig> applicationClass) {
        return config.get(applicationClass.getName() + "." + RoutingName.CONFIG_KEY_NAME)
                .asString()
                .or(() -> Optional.ofNullable(applicationClass.getAnnotation(RoutingName.class))
                        .map(RoutingName::value))
                .flatMap(name -> RoutingName.DEFAULT_NAME.equals(name) ? Optional.empty() : Optional.of(name));
    }

    private boolean isNamedRoutingRequired(Config config,
                                           Class<? extends ServerApplicationConfig> applicationClass) {
        return config.get(applicationClass.getName() + "." + RoutingName.CONFIG_KEY_REQUIRED)
                .asBoolean()
                .or(() -> Optional.ofNullable(applicationClass.getAnnotation(RoutingName.class))
                        .map(RoutingName::required))
                .orElse(false);
    }

}
