package net.arnx.jsonic;

import static org.junit.jupiter.api.Assertions.*;
import java.io.*;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Random;
import net.arnx.jsonic.io.ReaderInputSource;
import net.arnx.jsonic.parse.JSONParser;
import org.junit.jupiter.api.Test;

class ReaderBooleanDirectTest {
    private static String outcome(JSON json, Reader reader) {
        try { return Arrays.toString(json.parse(reader, boolean[].class)); }
        catch (JSONException e) {
            StringBuilder text = new StringBuilder(e.getErrorCode()+":"+e.getMessage()+":"
                +e.getLineNumber()+":"+e.getColumnNumber()+":"+e.getErrorOffset());
            for (Throwable c=e.getCause(); c!=null; c=c.getCause()) text.append(':').append(c.getClass().getName()).append(':').append(c.getMessage());
            return text.toString();
        } catch (IOException e) { return e.getClass().getName()+":"+e.getMessage(); }
    }
    private static Reader reader(String text, int chunk) {
        return new StringReader(text) {
            @Override public int read(char[] chars,int offset,int length) throws IOException {
                return super.read(chars,offset,Math.min(chunk,length));
            }
        };
    }
    @Test void mixedValuesFailuresDepthAndShortReadsRetainLegacyResults() {
        String prefix="[null,true,false"+",null,true,false".repeat(400);
        for (int depth:new int[]{1,2,3,32,65}) for(int chunk:new int[]{1,2,3,7,64,1004,4096}) {
            for(String input:new String[]{"[]","[true]","[null]","[true,false,null]",prefix+"]",prefix+",1]",
                prefix+",\"true\"]",prefix+",\"bad\"]",prefix+",{}]",prefix+",[]]",prefix+",[true]]",
                prefix+",falseX]",prefix+",nulL]",prefix+",]",prefix+"]?",prefix+",true",
                "\uFEFF [true, \tfalse,\r\nnull] \r\n", "[true,0.0,0,-1,\"false\"]"}) {
                assertEquals(outcome(new JSON(depth){},reader(input,chunk)),outcome(new JSON(depth),reader(input,chunk)),
                    "depth="+depth+" chunk="+chunk+" input="+input.substring(0,Math.min(40,input.length())));
            }
        }
        Random random=new Random(551);
        String[] tokens={"true","false","null","0","1","0.0","\"true\"","\"bad\"","[]","{}"};
        for(int trial=0;trial<100;trial++) {
            StringBuilder text=new StringBuilder("[");
            int count=random.nextInt(400);
            for(int i=0;i<count;i++) { if(i>0)text.append(','); text.append(tokens[random.nextInt(tokens.length)]); }
            String input=text.append(']').toString();
            for(int chunk:new int[]{3,1004}) assertEquals(outcome(new JSON(){},reader(input,chunk)),outcome(new JSON(),reader(input,chunk)));
        }
        String valid="[null,true,false,null]";
        for(int i=0;i<=valid.length();i++) {
            String input=valid.substring(0,i);
            assertEquals(outcome(new JSON(){},reader(input,1004)),outcome(new JSON(),reader(input,1004)),input);
        }
    }
    private static String refillTrace(String text,int chunk,boolean legacy) throws Exception {
        ReaderInputSource[] source={null}; JSONParser[] parser={null}; StringBuilder reads=new StringBuilder();
        Reader reader=new StringReader(text) {
            @Override public int read(char[] chars,int offset,int length) throws IOException {
                reads.append(offset).append('/').append(length).append(':').append(source[0].getOffset()).append(':')
                    .append(source[0].getLineNumber()).append(':').append(source[0].getColumnNumber()).append(':')
                    .append(parser[0].getDepth()).append(':').append(parser[0].getValue()).append(';');
                return super.read(chars,offset,Math.min(chunk,length));
            }
        };
        source[0]=legacy?new ReaderInputSource(reader){}:new ReaderInputSource(reader);
        JSON json=new JSON(); JSONReader jsonReader=new JSONReader(json.new Context(),source[0],false,true);
        Field field=JSONReader.class.getDeclaredField("parser");field.setAccessible(true);parser[0]=(JSONParser)field.get(jsonReader);
        String value;
        try {value=Arrays.toString((boolean[])jsonReader.readTyped(boolean[].class));}
        catch(JSONException e) {value=e.getErrorCode()+":"+e.getMessage()+":"+e.getErrorOffset();}
        return value+"|"+reads;
    }
    @Test void refillsSeeTheSamePositionsParserValuesAndRequests() throws Exception {
        String prefix="[null,true,false"+",null,true,false".repeat(400);
        for(int chunk:new int[]{1,2,3,7,64,1004,4096}) {
            for(String input:new String[]{prefix+"]",prefix+",falseX]",prefix+",\"bad\"]",prefix+",[]]",
                " \r\n"+prefix+", \r\ntrue] \t",prefix+",true"})
                assertEquals(refillTrace(input,chunk,true),refillTrace(input,chunk,false),"chunk="+chunk);
        }
    }
    @Test void failedBatchProbesAndNullBitsRetainMarkAndCursor() throws Exception {
        ReaderInputSource source=new ReaderInputSource(new StringReader("true,null,false,X"));
        source.next();source.back();source.mark();
        boolean[] values=new boolean[8];long[] nulls=new long[1];
        assertEquals(3,source.readBooleanValues(values,nulls,0,false));
        assertTrue(values[0]);assertFalse(values[1]);assertFalse(values[2]);assertEquals(2L,nulls[0]);
        assertEquals("true,null,false",source.copy(15));assertEquals(15,source.getOffset());assertEquals(15,source.getColumnNumber());
        assertEquals(3,source.readBooleanValues(values,nulls,3,true));assertEquals(15,source.getOffset());
        assertEquals(',',source.next());assertEquals('X',source.next());
    }
    @Test void streamEncodingsAndIoFailuresRetainTheOriginalBehavior() throws Exception {
        String text="[null,true,false"+",null,true,false".repeat(400)+"]";
        for(String encoding:new String[]{"UTF-8","UTF-16LE","UTF-16BE","UTF-32LE","UTF-32BE"}) {
            byte[] bytes=("\uFEFF"+text).getBytes(encoding);
            assertArrayEquals(new JSON(){}.parse(new ByteArrayInputStream(bytes),boolean[].class),
                new JSON().parse(new ByteArrayInputStream(bytes),boolean[].class),encoding);
        }
        for(boolean legacy:new boolean[]{true,false}) {
            Reader input=new StringReader(text) {
                private int calls;
                @Override public int read(char[] chars,int off,int len) throws IOException {
                    if(++calls==3)throw new IOException("read failure");return super.read(chars,off,len);
                }
            };
            assertEquals(IOException.class.getName()+":read failure",outcome(legacy?new JSON(){}:new JSON(),input));
        }
    }
    @Test void largeMixedFallbackRetainsNullBitsBeyondTheInitialTokenCapacity() {
        String prefix="[null,true,false"+",null,true,false".repeat(1500);
        for(String suffix:new String[]{",1]",",\"true\"]",",\"bad\"]",",[]]",",{}]"}) {
            String input=prefix+suffix;
            for(int chunk:new int[]{7,1004}) assertEquals(outcome(new JSON(){},reader(input,chunk)),
                outcome(new JSON(),reader(input,chunk)),suffix);
        }
    }
    @Test void inputAndParserSubclassesRetainTheirOriginalScanner() throws Exception {
        ReaderInputSource source=new ReaderInputSource(new StringReader("[null,true,false,null]")) {
            @Override public boolean hasBooleanValue(boolean afterValue) {throw new AssertionError("Use subclass scanner");}
            @Override public int readBooleanValues(boolean[] values,long[] nulls,int count,boolean afterValue) {
                throw new AssertionError("Use subclass scanner");
            }
        };
        JSON json=new JSON();
        JSONReader reader=new JSONReader(json.new Context(),source,false,true);
        assertArrayEquals(new boolean[]{false,true,false,false},(boolean[])reader.readTyped(boolean[].class));
        JSONParser parser=new JSONParser(new ReaderInputSource(new StringReader("[true,false]")),32,false,true,
            json.new Context().getLocalCache()){};
        assertEquals(JSONEventType.START_ARRAY,parser.next());
        assertFalse(parser.canReadBufferedBooleans());
        assertFalse(parser.hasBufferedBooleanValue());
        assertEquals(0,parser.readBufferedBooleans(new boolean[2],new long[1],0));
        assertEquals(JSONEventType.BOOLEAN,parser.next());assertEquals(Boolean.TRUE,parser.getValue());
    }
}
