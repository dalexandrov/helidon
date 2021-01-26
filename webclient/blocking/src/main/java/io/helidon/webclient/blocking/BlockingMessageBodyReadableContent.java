/*
 * Copyright (c) 2021 Oracle and/or its affiliates. All rights reserved.
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
package io.helidon.webclient.blocking;

import io.helidon.common.GenericType;
import io.helidon.common.http.DataChunk;
import io.helidon.common.reactive.Multi;
import io.helidon.common.reactive.Single;
import io.helidon.media.common.MessageBodyContent;
import io.helidon.media.common.MessageBodyFilter;
import io.helidon.media.common.MessageBodyFilters;
import io.helidon.media.common.MessageBodyReadableContent;
import io.helidon.media.common.MessageBodyReader;
import io.helidon.media.common.MessageBodyReaderContext;
import io.helidon.media.common.MessageBodyReaders;
import io.helidon.media.common.MessageBodyStreamReader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Readable {@link MessageBodyContent}.
 */
@SuppressWarnings("deprecation")
public final class BlockingMessageBodyReadableContent {

    private final MessageBodyReadableContent content;

    public BlockingMessageBodyReadableContent(MessageBodyReadableContent content) {
        this.content = content;
    }

    /**
     * Create a new readable content backed by the specified publisher.
     * @param publisher content publisher
     * @param context reader context
     */
    BlockingMessageBodyReadableContent(Publisher<DataChunk> publisher, MessageBodyReaderContext context) {
        content = MessageBodyReadableContent.create(publisher,context);
    }


    /**
     * Get the reader context used to unmarshall data.
     *
     * @return MessageBodyReaderContext
     */
    public MessageBodyReaderContext readerContext() {
        return content.readerContext();
    }

    public BlockingMessageBodyReadableContent registerFilter(MessageBodyFilter filter) {
        content.registerFilter(filter);
        return this;
    }

    public BlockingMessageBodyReadableContent registerReader(MessageBodyReader<?> reader) {
        content.registerReader(reader);
        return this;
    }


    public BlockingMessageBodyReadableContent registerReader(MessageBodyStreamReader<?> reader) {
        content.registerReader(reader);
        return this;
    }

    @Deprecated
    public void registerFilter(Function<Publisher<DataChunk>, Publisher<DataChunk>> function) {
        content.registerFilter(function);
    }

    @Deprecated
    public <T> void registerReader(Class<T> type, io.helidon.common.http.Reader<T> reader) {
        content.registerReader(type, reader);
    }

    @Deprecated
    public <T> void registerReader(Predicate<Class<?>> predicate, io.helidon.common.http.Reader<T> reader) {
        content.registerReader(predicate, reader);
    }


    public void subscribe(Subscriber<? super DataChunk> subscriber) {
        content.subscribe(subscriber);
    }

    public <T> T as(final Class<T> type) {
        return content.as(type).await();
    }

    /**
     * Consumes and converts the content payload into a completion stage of the
     * requested type.
     *
     * @param type the requested type class
     * @param <T> the requested type
     * @return a completion stage of the requested type
     */
    public <T> T as(final GenericType<T> type) {
        return content.as(type).await();
    }

    /**
     * Consumes and converts the content payload into a stream of entities of
     * the requested type.
     *
     * @param type the requested type class
     * @param <T> the requested type
     * @return a stream of entities
     */
    public <T> Stream<T> asStream(Class<T> type) {
        return asStream(GenericType.create(type));
    }

    /**
     * Consumes and converts the content payload into a stream of entities of
     * the requested type.
     *
     * @param type the requested type class
     * @param <T> the requested type
     * @return a stream of entities
     */
    public <T> Stream<T> asStream(GenericType<T> type) {
        return content.asStream(type).collectList().await().stream();
    }

    /**
     * Create a new readable content backed by the given publisher and context.
     * @param publisher content publisher
     * @param context reader context
     * @return MessageBodyReadableContent
     */
    public static BlockingMessageBodyReadableContent create(Publisher<DataChunk> publisher, MessageBodyReaderContext context) {
        return new BlockingMessageBodyReadableContent(publisher, context);
    }

    public static BlockingMessageBodyReadableContent create(MessageBodyReadableContent content){
        return new BlockingMessageBodyReadableContent(content);
    }
}
