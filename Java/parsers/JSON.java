package parsers;

import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JSON {

    public static Object parse(Reader r) throws IOException {
        return parse(new JSON(r));
    }

    private static final String S_NULL = "null";
    private static final String S_TRUE = "true";
    private static final String S_FALSE = "false";
    private static final String S_COMMA = ",";
    private static final String S_COLON = ":";
    private static final String S_ARRAY_START = "[";
    private static final String S_ARRAY_END = "]";
    private static final String S_OBJECT_START = "{";
    private static final String S_OBJECT_END = "}";

    private static Object parse(JSON jr) throws IOException {
        String token = jr.readToken();
        if (token == null)
            return null;
        if (token.equals(S_NULL))
            return null;
        if (token.equals(S_TRUE))
            return Boolean.TRUE;
        if (token.equals(S_FALSE))
            return Boolean.FALSE;
        if (token.equals(S_ARRAY_START)) {
            List<Object> array = new ArrayList<>();
            do {
                array.add(parse(jr));
                token = jr.readToken();
            } while (token.equals(S_COMMA));
            if (!token.equals(S_ARRAY_END))
                throw new IOException("Unexpected " + token + " in array"); // !!
            return array;
        }
        if (token.equals(S_OBJECT_START)) {
            Map<String, Object> object = new LinkedHashMap<>();
            do {
                String key = dequote(jr.readToken());
                token = jr.readToken();
                if (!token.equals(S_COLON))
                    throw new IOException("Unexpected " + token + " in object");
                object.put(key, parse(jr));
                token = jr.readToken();
            } while (token.equals(S_COMMA));
            if (!token.equals(S_OBJECT_END))
                throw new IOException("Unexpected " + token + " in object"); // !!
            return object;
        }
        if (CHARTOKEN.indexOf(token) != -1)
            throw new IOException("Unexpected " + token); // ??
        if (quoted(token))
            return dequote(token);
        return Double.valueOf(token);
    }

    public static boolean quoted(String s) {
        return s.length() > 1 && s.charAt(0) == '\"' && s.charAt(s.length() - 1) == '\"';
    }

    public static String dequote(String quoted) {
        if (quoted(quoted))
            return quoted.substring(1, quoted.length() - 1);
        throw new RuntimeException(quoted + " is not a quoted string.");
    }

    public static void mapObject(Object json, Object target, Map<String, String> resolver) throws Exception {
        if (!(json instanceof Map))
            throw new Exception("Map expected, got " + json.getClass());
        Map<?, ?> mjson = (Map<?, ?>) json;
        Field fs[] = target.getClass().getFields();
        for (int i = 0; i < fs.length; i++) {
            Field f = fs[i];
            String n = f.getName();
            if (resolver != null && resolver.containsKey(n))
                n = resolver.get(n);
            Object v = mjson.get(n);
            if (v != null) {
                if (v instanceof Boolean)
                    f.setBoolean(target, (Boolean) v);
                else if (v instanceof Double)
                    f.setDouble(target, (Double) v);
                else if (v instanceof String)
                    f.set(target, v);
                else if (v instanceof List)
                    mapList(v, f.get(target),
                            (Class<?>) ((ParameterizedType) f.getGenericType()).getActualTypeArguments()[0], resolver);
                else if (v instanceof Map)
                    mapObject(v, f.get(target), resolver);
                else
                    throw new Exception(v.getClass() + " encountered");
            }
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static void mapList(Object json, Object target, Class type, Map<String, String> resolver) throws Exception {
        if (!(json instanceof List) || !(target instanceof List))
            throw new Exception("Lists expected, got " + json.getClass() + " and " + target.getClass());
        List ljson = (List) json;
        List ltarget = (List) target;
        for (int i = 0; i < ljson.size(); i++) {
            Object v = ljson.get(i);
            if (ltarget.size() <= i)
                ltarget.add(null);
            if (v != null) {
                if (type.isInstance(v))
                    ltarget.set(i, type.cast(v));
                else {
                    Object o = type.newInstance();
                    mapObject(v, o, resolver);
                    ltarget.set(i, o);
                }
            }
        }
    }

    private static final String WHITESPACE = " \t\r\n";
    private static final String CHARTOKEN = "[]{},:";
    private static final char STRINGCHAR = '\"';
    private static final char ESCAPECHAR = '\\';
    private static final String ESCAPED = "\"\\/bfnrt";
    private static final String UNESCAPED = "\"\\/\b\f\n\r\t";
    private static final char UNICODECHAR = 'u';

    private final Reader r;
    private int c;

    private JSON(Reader r) throws IOException {
        this.r = r;
        c = r.read();
    }

    private String readToken() throws IOException {
        while (WHITESPACE.indexOf(c) != -1) {
            c = r.read();
        }

        if (c == -1)
            return null;

        StringBuilder sb = new StringBuilder();
        sb.append((char) c);

        if (CHARTOKEN.indexOf(c) != -1) {
            c = r.read();
            return sb.toString();
        }

        if (c == STRINGCHAR) {
            do {
                c = r.read();
                if (c == -1) {
                    throw new IOException("Unexpected end of stream " + sb.toString());
                } else if (c != ESCAPECHAR) {
                    sb.append((char) c);
                } else {
                    int e = r.read();
                    if (e == -1)
                        throw new IOException("Unexpected end of stream " + sb.toString());
                    if (e == UNICODECHAR) {
                        sb.append((char) Integer.parseInt("" + r.read() + r.read() + r.read() + r.read(), 16));
                    } else {
                        int pos = ESCAPED.indexOf(e);
                        if (pos == -1)
                            throw new IOException("Illegal escape sequence: " + sb.toString() + "\\" + (char) e);
                        sb.append(UNESCAPED.charAt(pos));
                    }
                }
            } while (c != STRINGCHAR);
            c = r.read();
            return sb.toString();
        }

        while ((c = r.read()) != -1 && WHITESPACE.indexOf(c) == -1 && CHARTOKEN.indexOf(c) == -1) {
            sb.append((char) c);
        }
        return sb.toString();
    }
}
