/*
 * Copyright (c) 2020 Oracle and/or its affiliates.
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

package io.helidon.faulttolerance;

import java.util.concurrent.atomic.AtomicInteger;

final record AtomicCycle(
        AtomicInteger atomicInteger,
        int maxIndex) {

    AtomicCycle(int maxIndex) {
        this(new AtomicInteger(-1), maxIndex + 1);
    }

    int get() {
        return atomicInteger.get();
    }

    void set(int n) {
        atomicInteger.set(n);
    }

    int incrementAndGet() {
        return atomicInteger.accumulateAndGet(maxIndex, (current, max) -> (current + 1) % max);
    }
}
