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

package cz.msebera.android.httpclient.impl.nio;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CharsetEncoder;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpEntityEnclosingRequest;
import cz.msebera.android.httpclient.HttpException;
import cz.msebera.android.httpclient.HttpRequest;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.HttpResponseFactory;
import cz.msebera.android.httpclient.annotation.NotThreadSafe;
import cz.msebera.android.httpclient.config.MessageConstraints;
import cz.msebera.android.httpclient.entity.ContentLengthStrategy;
import cz.msebera.android.httpclient.impl.nio.codecs.DefaultHttpRequestWriter;
import cz.msebera.android.httpclient.impl.nio.codecs.DefaultHttpRequestWriterFactory;
import cz.msebera.android.httpclient.impl.nio.codecs.DefaultHttpResponseParser;
import cz.msebera.android.httpclient.impl.nio.codecs.DefaultHttpResponseParserFactory;
import cz.msebera.android.httpclient.nio.NHttpClientEventHandler;
import cz.msebera.android.httpclient.nio.NHttpClientHandler;
import cz.msebera.android.httpclient.nio.NHttpClientIOTarget;
import cz.msebera.android.httpclient.nio.NHttpMessageParser;
import cz.msebera.android.httpclient.nio.NHttpMessageParserFactory;
import cz.msebera.android.httpclient.nio.NHttpMessageWriter;
import cz.msebera.android.httpclient.nio.NHttpMessageWriterFactory;
import cz.msebera.android.httpclient.nio.reactor.EventMask;
import cz.msebera.android.httpclient.nio.reactor.IOSession;
import cz.msebera.android.httpclient.nio.reactor.SessionInputBuffer;
import cz.msebera.android.httpclient.nio.reactor.SessionOutputBuffer;
import cz.msebera.android.httpclient.nio.util.ByteBufferAllocator;
import cz.msebera.android.httpclient.params.HttpParamConfig;
import cz.msebera.android.httpclient.params.HttpParams;
import cz.msebera.android.httpclient.util.Args;

/**
 * Default implementation of the {@link cz.msebera.android.httpclient.nio.NHttpClientConnection}
 * interface.
 *
 * @since 4.0
 */
@SuppressWarnings("deprecation")
@NotThreadSafe
public class DefaultNHttpClientConnection
    extends NHttpConnectionBase implements NHttpClientIOTarget {

    protected final NHttpMessageParser<HttpResponse> responseParser;
    protected final NHttpMessageWriter<HttpRequest> requestWriter;

    /**
     * Creates a new instance of this class given the underlying I/O session.
     *
     * @param session the underlying I/O session.
     * @param responseFactory HTTP response factory.
     * @param allocator byte buffer allocator.
     * @param params HTTP parameters.
     *
     * @deprecated (4.3) use {@link DefaultNHttpClientConnection#DefaultNHttpClientConnection(
     *   IOSession, int, int, ByteBufferAllocator, CharsetDecoder, CharsetEncoder,
     *   MessageConstraints, ContentLengthStrategy, ContentLengthStrategy,
     *   NHttpMessageWriterFactory, NHttpMessageParserFactory)}
     */
    @Deprecated
    public DefaultNHttpClientConnection(
            final IOSession session,
            final HttpResponseFactory responseFactory,
            final ByteBufferAllocator allocator,
            final HttpParams params) {
        super(session, allocator, params);
        Args.notNull(responseFactory, "Response factory");
        this.responseParser = createResponseParser(this.inbuf, responseFactory, params);
        this.requestWriter = createRequestWriter(this.outbuf, params);
        this.hasBufferedInput = false;
        this.hasBufferedOutput = false;
        this.session.setBufferStatus(this);
    }

    /**
     * Creates new instance DefaultNHttpClientConnection given the underlying I/O session.
     *
     * @param session the underlying I/O session.
     * @param buffersize buffer size. Must be a positive number.
     * @param fragmentSizeHint fragment size hint.
     * @param allocator memory allocator.
     *   If <code>null</code> {@link cz.msebera.android.httpclient.nio.util.HeapByteBufferAllocator#INSTANCE}
     *   will be used.
     * @param chardecoder decoder to be used for decoding HTTP protocol elements.
     *   If <code>null</code> simple type cast will be used for byte to char conversion.
     * @param charencoder encoder to be used for encoding HTTP protocol elements.
     *   If <code>null</code> simple type cast will be used for char to byte conversion.
     * @param constraints Message constraints. If <code>null</code>
     *   {@link MessageConstraints#DEFAULT} will be used.
     * @param incomingContentStrategy incoming content length strategy. If <code>null</code>
     *   {@link org.apache.http.impl.entity.LaxContentLengthStrategy#INSTANCE} will be used.
     * @param outgoingContentStrategy outgoing content length strategy. If <code>null</code>
     *   {@link org.apache.http.impl.entity.StrictContentLengthStrategy#INSTANCE} will be used.
     *
     * @since 4.3
     */
    public DefaultNHttpClientConnection(
            final IOSession session,
            final int buffersize,
            final int fragmentSizeHint,
            final ByteBufferAllocator allocator,
            final CharsetDecoder chardecoder,
            final CharsetEncoder charencoder,
            final MessageConstraints constraints,
            final ContentLengthStrategy incomingContentStrategy,
            final ContentLengthStrategy outgoingContentStrategy,
            final NHttpMessageWriterFactory<HttpRequest> requestWriterFactory,
            final NHttpMessageParserFactory<HttpResponse> responseParserFactory) {
        super(session, buffersize, fragmentSizeHint,  allocator, chardecoder, charencoder,
                incomingContentStrategy, outgoingContentStrategy);
        this.requestWriter = (requestWriterFactory != null ? requestWriterFactory :
            DefaultHttpRequestWriterFactory.INSTANCE).create(this.outbuf);
        this.responseParser = (responseParserFactory != null ? responseParserFactory :
            DefaultHttpResponseParserFactory.INSTANCE).create(this.inbuf, constraints);
    }

    /**
     * @since 4.3
     */
    public DefaultNHttpClientConnection(
            final IOSession session,
            final int buffersize,
            final CharsetDecoder chardecoder,
            final CharsetEncoder charencoder,
            final MessageConstraints constraints) {
        this(session, buffersize, buffersize, null, chardecoder, charencoder, constraints,
                null, null, null, null);
    }

    /**
     * @since 4.3
     */
    public DefaultNHttpClientConnection(final IOSession session, final int buffersize) {
        this(session, buffersize, buffersize, null, null, null, null, null, null, null, null);
    }

    /**
     * Creates an instance of {@link NHttpMessageParser} to be used
     * by this connection for parsing incoming {@link HttpResponse} messages.
     * <p>
     * This method can be overridden in a super class in order to provide
     * a different implementation of the {@link NHttpMessageParser} interface.
     *
     * @return HTTP response parser.
     *
     * @deprecated (4.3) use constructor.
     */
    @Deprecated
    protected NHttpMessageParser<HttpResponse> createResponseParser(
            final SessionInputBuffer buffer,
            final HttpResponseFactory responseFactory,
            final HttpParams params) {
        // override in derived class to specify a line parser
        final MessageConstraints constraints = HttpParamConfig.getMessageConstraints(params);
        return new DefaultHttpResponseParser(buffer, null, responseFactory, constraints);
    }

    /**
     * Creates an instance of {@link NHttpMessageWriter} to be used
     * by this connection for writing out outgoing {@link HttpRequest} messages.
     * <p>
     * This method can be overridden by a super class in order to provide
     * a different implementation of the {@link NHttpMessageWriter} interface.
     *
     * @return HTTP response parser.
     *
     * @deprecated (4.3) use constructor.
     */
    @Deprecated
    protected NHttpMessageWriter<HttpRequest> createRequestWriter(
            final SessionOutputBuffer buffer,
            final HttpParams params) {
        // override in derived class to specify a line formatter
        return new DefaultHttpRequestWriter(buffer, null);
    }

    /**
     * @since 4.2
     */
    protected void onResponseReceived(final HttpResponse response) {
    }

    /**
     * @since 4.2
     */
    protected void onRequestSubmitted(final HttpRequest request) {
    }

    public void resetInput() {
        this.response = null;
        this.contentDecoder = null;
        this.responseParser.reset();
    }

    public void resetOutput() {
        this.request = null;
        this.contentEncoder = null;
        this.requestWriter.reset();
    }

    public void consumeInput(final NHttpClientEventHandler handler) {
        if (this.status != ACTIVE) {
            this.session.clearEvent(EventMask.READ);
            return;
        }
        try {
            if (this.response == null) {
                int bytesRead;
                do {
                    bytesRead = this.responseParser.fillBuffer(this.session.channel());
                    if (bytesRead > 0) {
                        this.inTransportMetrics.incrementBytesTransferred(bytesRead);
                    }
                    this.response = this.responseParser.parse();
                } while (bytesRead > 0 && this.response == null);
                if (this.response != null) {
                    if (this.response.getStatusLine().getStatusCode() >= 200) {
                        final HttpEntity entity = prepareDecoder(this.response);
                        this.response.setEntity(entity);
                        this.connMetrics.incrementResponseCount();
                    }
                    onResponseReceived(this.response);
                    handler.responseReceived(this);
                    if (this.contentDecoder == null) {
                        resetInput();
                    }
                }
                if (bytesRead == -1) {
                    handler.endOfInput(this);
                }
            }
            if (this.contentDecoder != null && (this.session.getEventMask() & SelectionKey.OP_READ) > 0) {
                handler.inputReady(this, this.contentDecoder);
                if (this.contentDecoder.isCompleted()) {
                    // Response entity received
                    // Ready to receive a new response
                    resetInput();
                }
            }
        } catch (final HttpException ex) {
            resetInput();
            handler.exception(this, ex);
        } catch (final Exception ex) {
            handler.exception(this, ex);
        } finally {
            // Finally set buffered input flag
            this.hasBufferedInput = this.inbuf.hasData();
        }
    }

    public void produceOutput(final NHttpClientEventHandler handler) {
        try {
            if (this.status == ACTIVE) {
                if (this.contentEncoder == null) {
                    handler.requestReady(this);
                }
                if (this.contentEncoder != null) {
                    handler.outputReady(this, this.contentEncoder);
                    if (this.contentEncoder.isCompleted()) {
                        resetOutput();
                    }
                }
            }
            if (this.outbuf.hasData()) {
                final int bytesWritten = this.outbuf.flush(this.session.channel());
                if (bytesWritten > 0) {
                    this.outTransportMetrics.incrementBytesTransferred(bytesWritten);
                }
            }
            if (!this.outbuf.hasData()) {
                if (this.status == CLOSING) {
                    this.session.close();
                    this.status = CLOSED;
                    resetOutput();
                }
                if (this.contentEncoder == null && this.status != CLOSED) {
                    this.session.clearEvent(EventMask.WRITE);
                }
            }
        } catch (final Exception ex) {
            handler.exception(this, ex);
        } finally {
            // Finally set the buffered output flag
            this.hasBufferedOutput = this.outbuf.hasData();
        }
    }

    public void submitRequest(final HttpRequest request) throws IOException, HttpException {
        Args.notNull(request, "HTTP request");
        assertNotClosed();
        if (this.request != null) {
            throw new HttpException("Request already submitted");
        }
        onRequestSubmitted(request);
        this.requestWriter.write(request);
        this.hasBufferedOutput = this.outbuf.hasData();

        if (request instanceof HttpEntityEnclosingRequest
                && ((HttpEntityEnclosingRequest) request).getEntity() != null) {
            prepareEncoder(request);
            this.request = request;
        }
        this.connMetrics.incrementRequestCount();
        this.session.setEvent(EventMask.WRITE);
    }

    public boolean isRequestSubmitted() {
        return this.request != null;
    }

    public void consumeInput(final NHttpClientHandler handler) {
        consumeInput(new NHttpClientEventHandlerAdaptor(handler));
    }

    public void produceOutput(final NHttpClientHandler handler) {
        produceOutput(new NHttpClientEventHandlerAdaptor(handler));
    }

}
