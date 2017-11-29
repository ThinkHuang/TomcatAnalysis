/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.coyote;

import java.io.IOException;
import java.util.Locale;

import org.apache.tomcat.util.buf.ByteChunk;
import org.apache.tomcat.util.http.MimeHeaders;

/**
 * Response object.
 * 
 * @author James Duncan Davidson [duncan@eng.sun.com]
 * @author Jason Hunter [jch@eng.sun.com]
 * @author James Todd [gonzo@eng.sun.com]
 * @author Harish Prabandham
 * @author Hans Bergsten <hans@gefionsoftware.com>
 * @author Remy Maucherat
 */
public final class Response {

    // ----------------------------------------------------- Class Variables

    /**
     * Default locale as mandated by the spec.
     */
    private static Locale DEFAULT_LOCALE = Locale.getDefault();


    // ----------------------------------------------------- Instance Variables

    /**
     * Status code.
     */
    protected int status = 200;


    /**
     * Status message.
     */
    protected String message = null;


    /**
     * Response headers.
     */
    protected MimeHeaders headers = new MimeHeaders();


    /**
     * Associated output buffer.
     */
    protected OutputBuffer outputBuffer;


    /**
     * Notes.
     */
    protected Object notes[] = new Object[Constants.MAX_NOTES];


    /**
     * Committed flag.
     */
    protected boolean commited = false;


    /**
     * Action hook.
     */
    public ActionHook hook;


    /**
     * HTTP specific fields.
     */
    protected String contentType = null;
    protected String contentLanguage = null;
    protected String characterEncoding = Constants.DEFAULT_CHARACTER_ENCODING;
    protected long contentLength = -1;
    private Locale locale = DEFAULT_LOCALE;

    // General informations
    private long contentWritten = 0;

    /**
     * Holds request error exception.
     */
    protected Exception errorException = null;

    /**
     * Has the charset been explicitly set.
     */
    protected boolean charsetSet = false;

    protected Request req;

    // ------------------------------------------------------------- Properties

    public Request getRequest() {
        return req;
    }

    public void setRequest( Request req ) {
        this.req=req;
    }

    public OutputBuffer getOutputBuffer() {
        return outputBuffer;
    }


    public void setOutputBuffer(OutputBuffer outputBuffer) {
        this.outputBuffer = outputBuffer;
    }


    public MimeHeaders getMimeHeaders() {
        return headers;
    }


    public ActionHook getHook() {
        return hook;
    }


    public void setHook(ActionHook hook) {
        this.hook = hook;
    }


    // -------------------- Per-Response "notes" --------------------


    public final void setNote(int pos, Object value) {
        notes[pos] = value;
    }


    public final Object getNote(int pos) {
        return notes[pos];
    }


    // -------------------- Actions --------------------


    public void action(ActionCode actionCode, Object param) {
        if (hook != null) {
            if( param==null ) 
                hook.action(actionCode, this);
            else
                hook.action(actionCode, param);
        }
    }


    // -------------------- State --------------------


    public int getStatus() {
        return status;
    }

    
    /** 
     * Set the response status 
     */ 
    public void setStatus( int status ) {
        this.status = status;
    }


    /**
     * Get the status message.
     */
    public String getMessage() {
        return message;
    }


    /**
     * Set the status message.
     */
    public void setMessage(String message) {
        this.message = message;
    }


    public boolean isCommitted() {
        return commited;
    }


    public void setCommitted(boolean v) {
        this.commited = v;
    }


    // -----------------Error State --------------------


    /** 
     * Set the error Exception that occurred during
     * request processing.
     */
    public void setErrorException(Exception ex) {
    errorException = ex;
    }


    /** 
     * Get the Exception that occurred during request
     * processing.
     */
    public Exception getErrorException() {
        return errorException;
    }


    public boolean isExceptionPresent() {
        return ( errorException != null );
    }


    // -------------------- Methods --------------------
    
    
    public void reset() 
        throws IllegalStateException {
        
        // Reset the headers only if this is the main request,
        // not for included
        contentType = null;
        locale = DEFAULT_LOCALE;
        contentLanguage = null;
        characterEncoding = Constants.DEFAULT_CHARACTER_ENCODING;
        contentLength = -1;
        charsetSet = false;

        status = 200;
        message = null;
        headers.clear();
        
        // Force the PrintWriter to flush its data to the output
        // stream before resetting the output stream
        //
        // Reset the stream
        if (commited) {
            //String msg = sm.getString("servletOutputStreamImpl.reset.ise"); 
            throw new IllegalStateException();
        }
        
        action(ActionCode.RESET, this);
    }


    public void finish() {
        action(ActionCode.CLOSE, this);
    }


    public void acknowledge() {
        action(ActionCode.ACK, this);
    }


    // -------------------- Headers --------------------
    /**
     * Warning: This method always returns <code>false<code> for Content-Type
     * and Content-Length.
     */
    public boolean containsHeader(String name) {
        return headers.getHeader(name) != null;
    }


    public void setHeader(String name, String value) {
        char cc=name.charAt(0);
        if( cc=='C' || cc=='c' ) {
            if( checkSpecialHeader(name, value) )
            return;
        }
        headers.setValue(name).setString( value);
    }


    public void addHeader(String name, String value) {
        char cc=name.charAt(0);
        if( cc=='C' || cc=='c' ) {
            if( checkSpecialHeader(name, value) )
            return;
        }
        headers.addValue(name).setString( value );
    }

    
    /** 
     * Set internal fields for special header names. 
     * Called from set/addHeader.
     * Return true if the header is special, no need to set the header.
     */
    private boolean checkSpecialHeader( String name, String value) {
        // XXX Eliminate redundant fields !!!
        // ( both header and in special fields )
        if( name.equalsIgnoreCase( "Content-Type" ) ) {
            setContentType( value );
            return true;
        }
        if( name.equalsIgnoreCase( "Content-Length" ) ) {
            try {
                long cL=Long.parseLong( value );
                setContentLength( cL );
                return true;
            } catch( NumberFormatException ex ) {
                // Do nothing - the spec doesn't have any "throws" 
                // and the user might know what he's doing
                return false;
            }
        }
        if( name.equalsIgnoreCase( "Content-Language" ) ) {
            // XXX XXX Need to construct Locale or something else
        }
        return false;
    }


    /** Signal that we're done with the headers, and body will follow.
     *  Any implementation needs to notify ContextManager, to allow
     *  interceptors to fix headers.
     */
    public void sendHeaders() {
        action(ActionCode.COMMIT, this);
        commited = true;
    }


    // -------------------- I18N --------------------


    public Locale getLocale() {
        return locale;
    }

    /**
     * Called explicitly by user to set the Content-Language and
     * the default encoding
     */
    public void setLocale(Locale locale) {

        if (locale == null) {
            return;  // throw an exception?
        }

        // Save the locale for use by getLocale()
        this.locale = locale;

        // Set the contentLanguage for header output
        contentLanguage = locale.getLanguage();
        if ((contentLanguage != null) && (contentLanguage.length() > 0)) {
            String country = locale.getCountry();
            StringBuilder value = new StringBuilder(contentLanguage);
            if ((country != null) && (country.length() > 0)) {
                value.append('-');
                value.append(country);
            }
            contentLanguage = value.toString();
        }

    }

    /**
     * Return the content language.
     */
    public String getContentLanguage() {
        return contentLanguage;
    }

    /*
     * Overrides the name of the character encoding used in the body
     * of the response. This method must be called prior to writing output
     * using getWriter().
     *
     * @param charset String containing the name of the character encoding.
     */
    public void setCharacterEncoding(String charset) {

        if (isCommitted())
            return;
        if (charset == null)
            return;

        characterEncoding = charset;
        charsetSet=true;
    }

    public String getCharacterEncoding() {
        return characterEncoding;
    }

    /**
     * Sets the content type.
     *
     * This method must preserve any response charset that may already have 
     * been set via a call to response.setContentType(), response.setLocale(),
     * or response.setCharacterEncoding().
     *
     * @param type the content type
     */
    @SuppressWarnings("deprecation")
    public void setContentType(String type) {

        int semicolonIndex = -1;

        if (type == null) {
            this.contentType = null;
            return;
        }

        /*
         * Remove the charset param (if any) from the Content-Type, and use it
         * to set the response encoding.
         * The most recent response encoding setting will be appended to the
         * response's Content-Type (as its charset param) by getContentType();
         */
        boolean hasCharset = false;
        int len = type.length();
        int index = type.indexOf(';');
        while (index != -1) {
            semicolonIndex = index;
            index++;
            // Yes, isSpace() is deprecated but it does exactly what we need
            while (index < len && Character.isSpace(type.charAt(index))) {
                index++;
            }
            if (index+8 < len
                    && type.charAt(index) == 'c'
                    && type.charAt(index+1) == 'h'
                    && type.charAt(index+2) == 'a'
                    && type.charAt(index+3) == 'r'
                    && type.charAt(index+4) == 's'
                    && type.charAt(index+5) == 'e'
                    && type.charAt(index+6) == 't'
                    && type.charAt(index+7) == '=') {
                hasCharset = true;
                break;
            }
            index = type.indexOf(';', index);
        }

        if (!hasCharset) {
            this.contentType = type;
            return;
        }

        this.contentType = type.substring(0, semicolonIndex);
        String tail = type.substring(index+8);
        int nextParam = tail.indexOf(';');
        String charsetValue = null;
        if (nextParam != -1) {
            this.contentType += tail.substring(nextParam);
            charsetValue = tail.substring(0, nextParam);
        } else {
            charsetValue = tail;
        }

        // The charset value may be quoted, but must not contain any quotes.
        if (charsetValue != null && charsetValue.length() > 0) {
            charsetSet=true;
            charsetValue = charsetValue.replace('"', ' ');
            this.characterEncoding = charsetValue.trim();
        }
    }

    public String getContentType() {

        String ret = contentType;

        if (ret != null 
            && characterEncoding != null
            && charsetSet) {
            ret = ret + ";charset=" + characterEncoding;
        }

        return ret;
    }
    
    public void setContentLength(int contentLength) {
        this.contentLength = contentLength;
    }

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public int getContentLength() {
        long length = getContentLengthLong();
        
        if (length < Integer.MAX_VALUE) {
            return (int) length;
        }
        return -1;
    }
    
    public long getContentLengthLong() {
        return contentLength;
    }


    /** 
     * Write a chunk of bytes.
     */
    public void doWrite(ByteChunk chunk/*byte buffer[], int pos, int count*/)
        throws IOException
    {
        outputBuffer.doWrite(chunk, this);
        contentWritten+=chunk.getLength();
    }

    // --------------------
    
    public void recycle() {
        
        contentType = null;
        contentLanguage = null;
        locale = DEFAULT_LOCALE;
        characterEncoding = Constants.DEFAULT_CHARACTER_ENCODING;
        charsetSet = false;
        contentLength = -1;
        status = 200;
        message = null;
        commited = false;
        errorException = null;
        headers.clear();

        // update counters
        contentWritten=0;
    }

    /**
     * Bytes written by application - i.e. before compression, chunking, etc.
     */
    public long getContentWritten() {
        return contentWritten;
    }
    
    /**
     * Bytes written to socket - i.e. after compression, chunking, etc.
     */
    public long getBytesWritten(boolean flush) {
        if (flush) {
            action(ActionCode.CLIENT_FLUSH, this);
        }
        return outputBuffer.getBytesWritten();
    }
}
