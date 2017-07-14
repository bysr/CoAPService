/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package cz.msebera.android.httpclient.impl.nio.pool;

import java.io.IOException;

import javax.net.ssl.SSLContext;

import cz.msebera.android.httpclient.HttpHost;
import cz.msebera.android.httpclient.HttpRequest;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpResponseFactory;
import cz.msebera.android.httpclient.annotation.Immutable;
import cz.msebera.android.httpclient.config.ConnectionConfig;
import cz.msebera.android.httpclient.impl.DefaultHttpResponseFactory;
import cz.msebera.android.httpclient.impl.nio.DefaultNHttpClientConnectionFactory;
import cz.msebera.android.httpclient.impl.nio.SSLNHttpClientConnectionFactory;
import cz.msebera.android.httpclient.nio.NHttpClientConnection;
import cz.msebera.android.httpclient.nio.NHttpConnectionFactory;
import cz.msebera.android.httpclient.nio.NHttpMessageParserFactory;
import cz.msebera.android.httpclient.nio.NHttpMessageWriterFactory;
import cz.msebera.android.httpclient.nio.pool.NIOConnFactory;
import cz.msebera.android.httpclient.nio.reactor.IOEventDispatch;
import cz.msebera.android.httpclient.nio.reactor.IOSession;
import cz.msebera.android.httpclient.nio.reactor.ssl.SSLSetupHandler;
import cz.msebera.android.httpclient.nio.util.ByteBufferAllocator;
import cz.msebera.android.httpclient.nio.util.HeapByteBufferAllocator;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.util.Args;

/**
 * A basic {@link NIOConnFactory} implementation that creates
 * {@link NHttpClientConnection} instances given a {@link HttpHost} instance.
 *
 * @since 4.2
 */
@SuppressWarnings("deprecation")
@Immutable
public class BasicNIOConnFactory implements NIOConnFactory<HttpHost, NHttpClientConnection> {

    private final NHttpConnectionFactory<? extends NHttpClientConnection> plainFactory;
    private final NHttpConnectionFactory<? extends NHttpClientConnection> sslFactory;

    public BasicNIOConnFactory(
            final NHttpConnectionFactory<? extends NHttpClientConnection> plainFactory,
            final NHttpConnectionFactory<? extends NHttpClientConnection> sslFactory) {
        super();
        Args.notNull(plainFactory, "Plain HTTP client connection factory");
        this.plainFactory = plainFactory;
        this.sslFactory = sslFactory;
    }

    public BasicNIOConnFactory(
            final NHttpConnectionFactory<? extends NHttpClientConnection> plainFactory) {
        this(plainFactory, null);
    }

    /**
     * @deprecated (4.3) use {@link BasicNIOConnFactory#BasicNIOConnFactory(SSLContext,
     *   SSLSetupHandler, NHttpMessageParserFactory, NHttpMessageWriterFactory,
     *   ByteBufferAllocator, ConnectionConfig)}
     */
    @Deprecated
    public BasicNIOConnFactory(
            final SSLContext sslcontext,
            final SSLSetupHandler sslHandler,
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        this(new DefaultNHttpClientConnectionFactory(
                responseFactory, allocator, params),
                new SSLNHttpClientConnectionFactory(
                        sslcontext, sslHandler, responseFactory, allocator, params));
    }

    /**
     * @deprecated (4.3) use {@link BasicNIOConnFactory#BasicNIOConnFactory(SSLContext,
     *   SSLSetupHandler, ConnectionConfig)}
     */
    @Deprecated
    public BasicNIOConnFactory(
            final SSLContext sslcontext,
            final SSLSetupHandler sslHandler,
            final HttpParams params) {
        this(sslcontext, sslHandler,
                DefaultHttpResponseFactory.INSTANCE, HeapByteBufferAllocator.INSTANCE, params);
    }

    /**
     * @deprecated (4.3) use {@link BasicNIOConnFactory#BasicNIOConnFactory(ConnectionConfig)}
     */
    @Deprecated
    public BasicNIOConnFactory(final HttpParams params) {
        this(null, null, params);
    }

    /**
     * @since 4.3
     */
    public BasicNIOConnFactory(
            final SSLContext sslcontext,
            final SSLSetupHandler sslHandler,
            final NHttpMessageParserFactory<HttpResponse> responseParserFactory,
            final NHttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final ByteBufferAllocator allocator,
            final ConnectionConfig config) {
        this(new DefaultNHttpClientConnectionFactory(
                    responseParserFactory, requestWriterFactory, allocator, config),
                new SSLNHttpClientConnectionFactory(
                        sslcontext, sslHandler, responseParserFactory, requestWriterFactory,
                        allocator, config));
    }

    /**
     * @since 4.3
     */
    public BasicNIOConnFactory(
            final SSLContext sslcontext,
            final SSLSetupHandler sslHandler,
            final ConnectionConfig config) {
        this(sslcontext, sslHandler, null, null, null, config);
    }

    /**
     * @since 4.3
     */
    public BasicNIOConnFactory(final ConnectionConfig config) {
        this(new DefaultNHttpClientConnectionFactory(config), null);
    }

    public NHttpClientConnection create(final HttpHost route, final IOSession session) throws IOException {
        final NHttpClientConnection conn;
        if (route.getSchemeName().equalsIgnoreCase("https")) {
            if (this.sslFactory == null) {
                throw new IOException("SSL not supported");
            }
            conn = this.sslFactory.createConnection(session);
        } else {
            conn = this.plainFactory.createConnection(session);
        }
        session.setAttribute(IOEventDispatch.CONNECTION_KEY, conn);
        return conn;
    }

}
