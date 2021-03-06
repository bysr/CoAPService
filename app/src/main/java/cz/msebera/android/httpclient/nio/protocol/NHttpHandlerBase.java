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

package cz.msebera.android.httpclient.nio.protocol;

import java.io.IOException;

import cz.msebera.android.httpclient.ConnectionReuseStrategy;
import cz.msebera.android.httpclient.HttpRequest;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpStatus;
import cz.msebera.android.httpclient.annotation.Immutable;
import cz.msebera.android.httpclient.nio.NHttpConnection;
import cz.msebera.android.httpclient.nio.util.ByteBufferAllocator;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.protocol.HttpProcessor;
import cz.msebera.android.httpclient.util.Args;

/**
 * @since 4.0
 *
 * @deprecated (4.2) do not use
 */
@Deprecated
@Immutable // provided injected dependencies are immutable
public abstract class NHttpHandlerBase {

    protected static final String CONN_STATE = "http.nio.conn-state";

    protected final HttpProcessor httpProcessor;
    protected final ConnectionReuseStrategy connStrategy;
    protected final ByteBufferAllocator allocator;
    protected final HttpParams params;

    protected EventListener eventListener;

    public NHttpHandlerBase(
            final HttpProcessor httpProcessor,
            final ConnectionReuseStrategy connStrategy,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super();
        Args.notNull(httpProcessor, "HTTP processor");
        Args.notNull(connStrategy, "Connection reuse strategy");
        Args.notNull(allocator, "ByteBuffer allocator");
        Args.notNull(params, "HTTP parameters");
        this.httpProcessor = httpProcessor;
        this.connStrategy = connStrategy;
        this.allocator = allocator;
        this.params = params;
    }

    public HttpParams getParams() {
        return this.params;
    }

    public void setEventListener(final EventListener eventListener) {
        this.eventListener = eventListener;
    }

    protected void closeConnection(final NHttpConnection conn, final Throwable cause) {
        try {
            // Try to close it nicely
            conn.close();
        } catch (final IOException ex) {
            try {
                // Just shut the damn thing down
                conn.shutdown();
            } catch (final IOException ignore) {
            }
        }
    }

    protected void shutdownConnection(final NHttpConnection conn, final Throwable cause) {
        try {
            conn.shutdown();
        } catch (final IOException ignore) {
        }
    }

    protected void handleTimeout(final NHttpConnection conn) {
        try {
            if (conn.getStatus() == NHttpConnection.ACTIVE) {
                conn.close();
                if (conn.getStatus() == NHttpConnection.CLOSING) {
                    // Give the connection some grace time to
                    // close itself nicely
                    conn.setSocketTimeout(250);
                }
                if (this.eventListener != null) {
                    this.eventListener.connectionTimeout(conn);
                }
            } else {
                conn.shutdown();
            }
        } catch (final IOException ignore) {
        }
    }

    protected boolean canResponseHaveBody(
            final HttpRequest request, final HttpResponse response) {

        if (request != null && "HEAD".equalsIgnoreCase(request.getRequestLine().getMethod())) {
            return false;
        }

        final int status = response.getStatusLine().getStatusCode();
        return status >= HttpStatus.SC_OK
            && status != HttpStatus.SC_NO_CONTENT
            && status != HttpStatus.SC_NOT_MODIFIED
            && status != HttpStatus.SC_RESET_CONTENT;
    }

}
