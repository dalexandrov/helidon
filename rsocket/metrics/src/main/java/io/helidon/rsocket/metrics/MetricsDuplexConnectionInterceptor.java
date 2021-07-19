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

package io.helidon.rsocket.metrics;

import io.helidon.metrics.RegistryFactory;
import io.rsocket.DuplexConnection;
import io.rsocket.plugins.DuplexConnectionInterceptor;
import java.util.Objects;

import org.eclipse.microprofile.metrics.MetricRegistry;

public final class MetricsDuplexConnectionInterceptor implements DuplexConnectionInterceptor {

  private final MetricRegistry metricRegistry;

  public MetricsDuplexConnectionInterceptor() {
    metricRegistry = RegistryFactory.getInstance().getRegistry(MetricRegistry.Type.APPLICATION);
  }

  @Override
  public DuplexConnection apply(Type type, DuplexConnection duplexConnection) {
    Objects.requireNonNull(DuplexConnectionInterceptor.Type.SERVER, "ConnectionType must not be null");
    Objects.requireNonNull(duplexConnection, "Delegate must not be null");

    return new MetricsDuplexConnection(DuplexConnectionInterceptor.Type.SERVER, duplexConnection, metricRegistry);
  }
}
