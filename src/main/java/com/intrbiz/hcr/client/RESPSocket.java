package com.intrbiz.hcr.client;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

/**
 * Low level Redis Serialisation Protocol client
 */
public class RESPSocket implements Closeable
{
    /**
     * Different kinds of tokens in RESP
     */
    public static final class KIND
    {
        public static final char ARRAY = '*';
        
        public static final char INTEGER = ':';
        
        public static final char SIMPLE_STRING = '+';
        
        public static final char BULK_STRING = '$';
        
        public static final char ERROR = '-';
        
        public static final char EOF = '\0';
    }
    
    private static final char[] EOL = {'\r', '\n'};
    
    private static final char[] NULL_BULK_STRING = {'$', '-', '1', '\r', '\n'};
    
    private static final String[] NO_ARGUMENTS = { };
    
    /**
     * Simple commands that HCR supports
     */
    public static final class COMMANDS
    {
        public static final String COMMAND = "COMMAND";
        
        public static final String DEL = "DEL";
        
        public static final String GET = "GET";
        
        public static final String INFO = "INFO";
        
        public static final String KEYS = "KEYS";
        
        public static final String PING = "PING";
        
        public static final String QUIT = "QUIT";
        
        public static final String SET = "SET";
    }
    
    private final Socket socket;
    
    private final BufferedWriter output;
    
    private final BufferedReader input;
    
    private int length = -1;
    
    private long integerValue = -1;
    
    private String stringValue = null;
    
    public RESPSocket(String host, int port) throws IOException
    {
        this.socket = new Socket(host, port);
        this.socket.setSoTimeout(5_000);
        this.output = new BufferedWriter(new OutputStreamWriter(this.socket.getOutputStream()), 1024);
        this.input = new BufferedReader(new InputStreamReader(this.socket.getInputStream()), 1024);
    }
    
    /**
     * Write the length of an array
     */
    public RESPSocket writeArrayLength(int length) throws IOException
    {
        this.output.write(KIND.ARRAY);
        this.output.write(String.valueOf(length));
        this.output.write(EOL);
        return this;
    }
    
    /**
     * Write an integer
     */
    public RESPSocket writeInteger(long integer) throws IOException
    {
        this.output.write(KIND.INTEGER);
        this.output.write(String.valueOf(integer));
        this.output.write(EOL);
        return this;
    }
    
    /**
     * Write a simple string, which can not be null, zero or more than one line
     */
    public RESPSocket writeSimpleString(String string) throws IOException
    {
        this.output.write(KIND.SIMPLE_STRING);
        this.output.write(string);
        this.output.write(EOL);
        return this;
    }
    
    /**
     * Write an error, the string must match the same rules as a simple string
     */
    public RESPSocket writeError(String string) throws IOException
    {
        this.output.write(KIND.ERROR);
        this.output.write(string);
        this.output.write(EOL);
        return this;
    }
    
    /**
     * Write an arbitrary length string, which can be null or zero length
     */
    public RESPSocket writeBulkString(String string) throws IOException
    {
        if (string == null)
        {
            this.output.write(NULL_BULK_STRING);
        }
        else
        {
            this.output.write(KIND.BULK_STRING);
            this.output.write(String.valueOf(string.length()));
            this.output.write(EOL);
            this.output.write(string);
            this.output.write(EOL);
        }
        return this;
    }
    
    /**
     * Write a list of bulk strings
     */
    public RESPSocket writeBulkStrings(String... strings) throws IOException
    {
        for (String string : strings)
        {
            this.writeBulkString(string);
        }
        return this;
    }
    
    /**
     * Flush any buffered content to the destination
     */
    public RESPSocket flush() throws IOException
    {
        this.output.flush();
        return this;
    }
    
    /**
     * Close this socket
     */
    public void close() throws IOException
    {
        this.socket.close();
    }
    
    /**
     * Write a command
     */
    public RESPSocket writeCommand(String command) throws IOException
    {
        this.writeCommand(command, NO_ARGUMENTS);
        return this;
    }
    
    /**
     * Write a command
     */
    public RESPSocket writeCommand(String command, String... arguments) throws IOException
    {
        this.writeArrayLength(arguments.length + 1);
        this.writeBulkString(command);
        for (String argument : arguments)
        {
            this.writeBulkString(argument);
        }
        this.output.flush();
        return this;
    }
    
    /**
     * Read a token from the remote system, returning the token kind
     * @return the KIND of token
     */
    public char read() throws IOException
    {
        // reset our state
        this.length = -1;
        this.integerValue = -1;
        this.stringValue = null;
        // read some input
        String line = this.input.readLine();
        if (line == null || line.length() <= 0) return KIND.EOF;
        char kind = line.charAt(0);
        switch (kind)
        {
            case KIND.ARRAY:
                this.length = Integer.valueOf(line.substring(1));
                break;
            case KIND.INTEGER:
                this.integerValue = Integer.valueOf(line.substring(1));
                break;
            case KIND.ERROR:
            case KIND.SIMPLE_STRING:
                this.stringValue = line.substring(1);
                break;
            case KIND.BULK_STRING:
                this.stringValue = this.readBulkString(Integer.valueOf(line.substring(1)));
                break;
        }
        return kind;
    }
    
    /**
     * Read a bulk string from the wire
     */
    private String readBulkString(int length) throws IOException
    {
        if (length == -1) return null;
        StringBuilder string = new StringBuilder();
        int read = 0;
        char[] buffer = new char[1024];
        while (length > 0)
        {
            read = this.input.read(buffer, 0, length < buffer.length ? length : buffer.length);
            length -= read;
            string.append(buffer, 0, read);
        }
        this.input.readLine();
        return string.toString();
    }
    
    /**
     * Get the length of the current array, note this will change if you recursively read arrays
     */
    public int getLength()
    {
        return this.length;
    }
    
    /**
     * Get the value of an integer token
     */
    public long getInteger()
    {
        return this.integerValue;
    }
    
    /**
     * Get the value of a simple string, bulk string or error message
     */
    public String getString()
    {
        return this.stringValue;
    }
    
    // read validators
    
    private final void checkReadEOF(char kind) throws IOException
    {
        if (kind == KIND.EOF)
            throw new IOException("Unexpected end of stream, connection closed by remote host");
    }
    
    private final void checkReadError(char kind) throws IOException
    {
        this.checkReadEOF(kind);
        if (kind == KIND.ERROR)
            throw new IOException("Remote error: " + this.stringValue);   
    }
    
    private final void checkKind(char kind, char expected) throws IOException
    {
        
        if (kind != expected)
            throw new IOException("Expected " + expected + ", got: " + kind);
    }
    
    private final void checkKind(char kind, char expected1, char expected2) throws IOException
    {
        if (!(kind == expected1 || kind == expected2))
            throw new IOException("Expected " + expected1 + " or " + expected2 + ", got: " + kind);
    }
    
    private final void checkRead(char kind, char expected) throws IOException
    {
        this.checkReadError(kind);
        this.checkKind(kind, expected);
    }
    
    private final void checkRead(char kind, char expected1, char expected2) throws IOException
    {
        this.checkReadError(kind);
        this.checkKind(kind, expected1, expected2);
    }
    
    /**
     * Expect to read an error message, throwing an error if the token is not an error
     */
    public String readError() throws IOException
    {
        char kind = this.read();
        this.checkReadEOF(kind);
        this.checkKind(kind, KIND.ERROR);
        return this.stringValue;
    }
    
    /**
     * Expect to read a string (either simple or bulk), throwing an error if the token is an error or not a string
     */
    public String readString() throws IOException
    {
        char kind = this.read();
        this.checkRead(kind, KIND.BULK_STRING, KIND.SIMPLE_STRING);
        return this.stringValue;
    }
    
    /**
     * Expect to read an integer, throwing an error if the token is an error or not an integer
     */
    public long readInteger() throws IOException
    {
        char kind = this.read();
        this.checkRead(kind, KIND.INTEGER);
        return this.integerValue;
    }
    
    /**
     * Expect to read an array, throwing an error if the token is an error or not an array
     */
    public int readArrayLength() throws IOException
    {
        char kind = this.read();
        this.checkRead(kind, KIND.ARRAY);
        return this.length;
    }
    
    /**
     * Read the token, mapping it to an object, or throwing the remote error
     */
    public Object readObject() throws IOException
    {
        char kind = this.read();
        this.checkReadError(kind);
        switch (kind)
        {
            case KIND.INTEGER:
                return new Long(this.integerValue);
            case KIND.SIMPLE_STRING:
            case KIND.BULK_STRING:
                return this.stringValue;
            case KIND.ARRAY:
                return this.readArrayValues();
        }
        return null;
    }
    
    @SuppressWarnings({ "rawtypes", "unchecked" })
    private List<?> readArrayValues() throws IOException
    {
        List values = new LinkedList();
        for (int i = this.length; i > 0; i--)
        {
            values.add(this.readObject());
        }
        return values;
    }
    
    /**
     * Expect to fully read an array, throwing an error if the token is an error or not an array
     */
    public List<?> readArray() throws IOException
    {
        this.readArrayLength();
        return this.readArrayValues();
    }
    
    //
    
    /**
     * List the commands supported by the remote server
     */
    public List<?> command() throws IOException
    {
        this.writeCommand(COMMANDS.COMMAND);
        return this.readArray();
    }
    
    /**
     * Delete the given keys on the remote server
     * @param keys the keys to delete
     * @return the number of keys deleted
     */
    public long del(String... keys) throws IOException
    {
        this.writeCommand(COMMANDS.DEL, keys);
        return this.readInteger();
    }
    
    /**
     * Get the value of the given key from the remote server
     * @param key the key to get the value of
     * @return the value
     */
    public String get(String key) throws IOException
    {
        this.writeCommand(COMMANDS.GET, key);
        return this.readString();
    }
    
    /**
     * Get information about the remote server
     */
    public String info() throws IOException
    {
        this.writeCommand(COMMANDS.INFO);
        return this.readString();
    }
    
    /**
     * Get a list of all keys on the remote server
     * @return the list of keys
     */
    public List<?> keys() throws IOException
    {
        this.writeCommand(COMMANDS.KEYS);
        return this.readArray();
    }
    
    /**
     * Ping the server
     * @return PONG
     */
    public String ping() throws IOException
    {
        this.writeCommand(COMMANDS.PING);
        return this.readString();
    }
    
    /**
     * Quit this connection
     * @return OK then the server will close the connection
     */
    public String quit() throws IOException
    {
        this.writeCommand(COMMANDS.QUIT);
        return this.readString();
    }
    
    /**
     * Set the given key to the given value on the remote server
     * @param key the key to set
     * @param value the value of the key
     * @return OK
     */
    public String set(String key, String value) throws IOException
    {
        this.writeCommand(COMMANDS.SET, key, value);
        return this.readString();
    }
}
