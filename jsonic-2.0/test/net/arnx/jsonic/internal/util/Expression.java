package net.arnx.jsonic.internal.util;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import net.arnx.jsonic.internal.io.CharSequenceInputSource;
import net.arnx.jsonic.internal.io.InputSource;

public class Expression {
	private static final Map<String, Integer> LANK_MAP = new HashMap<String, Integer>();
	
	static {
		LANK_MAP.put("(", 130);
		LANK_MAP.put("[", 130);
		LANK_MAP.put(".", 130);
		
		LANK_MAP.put("~", 120);
		LANK_MAP.put("!", 120);
		
		LANK_MAP.put("*", 110);
		LANK_MAP.put("/", 110);
		LANK_MAP.put("%", 110);
		
		LANK_MAP.put("+", 100);
		LANK_MAP.put("-", 100);
		
		LANK_MAP.put("<<", 90);
		LANK_MAP.put(">>", 90);
		LANK_MAP.put(">>>", 90);
		
		LANK_MAP.put("<", 80);
		LANK_MAP.put(">", 80);
		LANK_MAP.put("<=", 80);
		LANK_MAP.put(">=", 80);
		LANK_MAP.put("instanceof", 80);
		
		LANK_MAP.put("==", 70);
		LANK_MAP.put("!=", 70);
		
		LANK_MAP.put("&", 60);
		
		LANK_MAP.put("^", 50);
		
		LANK_MAP.put("|", 40);
		
		LANK_MAP.put("&&", 30);
		
		LANK_MAP.put("||", 20);
		
		LANK_MAP.put("?", 10);
		LANK_MAP.put(":", 10);
	}
	
	public static Expression compile(String expr) {
		return new Expression(expr);
	}
	
	private String expr;
	private Node root;
	
	private Expression(String expr) {
		try {
			this.expr = expr;
			this.root = parse(new CharSequenceInputSource(expr));
		} catch (IOException e) {
			// no handle
		}
	}
	
	public Object evaluate() {
		Map<String, Object> map = Collections.emptyMap();
		return evaluate(map);
	}
	
	public Object evaluate(Map<String, Object> map) {
		Node current = root;
		LinkedList<Node> buffer = new LinkedList<Node>();
		LinkedList<Object> stack = new LinkedList<Object>();
		do {
			if (current.type == NodeType.GROUP) {
				stack.add(current);
			} else if (current.type == NodeType.OPERATOR) {
				Node node = null;
				while (!stack.isEmpty()) {
					node = (Node)stack.getLast();
					if (node.type == NodeType.OPERATOR && LANK_MAP.get(node.text) >= LANK_MAP.get(current.text)) {
						node = (Node)stack.removeLast();
						buffer.add(node);
					} else {
						break;
					}
				}
				stack.add(current);
			} else {
				buffer.add(current);
			}
			
			current = current.next;
			
			if (current == null || (current.prev != null && current.prev.parent != null && current.parent == current.prev.parent.parent)) {
				Node node = null;
				while (!stack.isEmpty()) {
					node = (Node)stack.removeLast();
					if (node.type != NodeType.GROUP) {
						buffer.add(node);
					} else if (current != null) {
						break;
					}
				}
			}
		} while (current != null);
		
		printTree();
		
		System.out.println("[reverse]");
		for (Node node : buffer) {
			System.out.println(node);
		}
		System.out.println();
		
		ListIterator<Node> i = buffer.listIterator(); 
		while (i.hasNext()) {
			Node node = i.next();
			switch (node.type) {
			case OPERATOR:
				if ("(".equals(node.text)) {
				} else if ("[".equals(node.text)) {
				} else if (".".equals(node.text)) {
				} else if ("~".equals(node.text)) {
				} else if ("!".equals(node.text)) {
					Object value = stack.removeLast();
					stack.add(evaluateNot(value));
				} else if ("*".equals(node.text)) {
					Object value2 = stack.removeLast();
					Object value1 = stack.removeLast();
					stack.add(evaluateMultiply(value1, value2));
				} else if ("/".equals(node.text)) {
					Object value2 = stack.removeLast();
					Object value1 = stack.removeLast();
					stack.add(evaluateDivide(value1, value2));
				} else if ("%".equals(node.text)) {					
					Object value2 = stack.removeLast();
					Object value1 = stack.removeLast();
					stack.add(evaluateRemainder(value1, value2));
				} else if ("+".equals(node.text)) {
					if (node.prev.type == NodeType.GROUP || node.prev.type == NodeType.OPERATOR) {
						Object value = stack.removeLast();
						stack.add(evaluatePlus(value));
					} else {
						Object value2 = stack.removeLast();
						Object value1 = stack.removeLast();
						stack.add(evaluatePlus(value1, value2));
					}
				} else if ("-".equals(node.text)) {
					if (node.prev.type == NodeType.GROUP || node.prev.type == NodeType.OPERATOR) {
						Object value = stack.removeLast();
						stack.add(evaluateMinus(value));
					} else {
						Object value2 = stack.removeLast();
						Object value1 = stack.removeLast();
						stack.add(evaluateMinus(value1, value2));
					}
				} else if ("<<".equals(node.text)) {
				} else if (">>".equals(node.text)) {
				} else if (">>>".equals(node.text)) {
				} else if ("<".equals(node.text)) {
				} else if (">".equals(node.text)) {
				} else if ("<=".equals(node.text)) {
				} else if (">=".equals(node.text)) {
				} else if ("instanceof".equals(node.text)) {
				} else if ("==".equals(node.text)) {
					Object value2 = stack.removeLast();
					Object value1 = stack.removeLast();
					stack.add(evaluateEquals(value1, value2));
				} else if ("!=".equals(node.text)) {
					Object value2 = stack.removeLast();
					Object value1 = stack.removeLast();
					stack.add(!evaluateEquals(value1, value2));
				} else if ("&".equals(node.text)) {
				} else if ("^".equals(node.text)) {
				} else if ("|".equals(node.text)) {
				} else if ("&&".equals(node.text)) {
					Object value2 = stack.removeLast();
					Object value1 = stack.removeLast();
					stack.add(evaluateShortAnd(value1, value2));
				} else if ("||".equals(node.text)) {
					Object value2 = stack.removeLast();
					Object value1 = stack.removeLast();
					stack.add(evaluateShortOr(value1, value2));
				} else if ("?".equals(node.text)) {
				} else if (":".equals(node.text)) {
				} else {
					throw new IllegalStateException();
				}
				break;
			case LITERAL:
				stack.add(node);
				break;
			case STRING:
				stack.add(node.text);
				break;
			case NUMBER:
				stack.add(new BigDecimal(node.text));
				break;
			case BOOLEAN:
				stack.add(Boolean.valueOf(node.text));
				break;
			case NULL:
				stack.add(null);
				break;
			}
		}
		
		return stack.getLast();
	}
	
	private Node parse(InputSource s) throws IOException {
		StringBuilder sb = new StringBuilder(1000);
		Node parent = new Node(null, null, NodeType.GROUP, "");
		Node current = parent;
		
		int n = -1;
		while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case '\r':
			case '\n':
			case ' ':
			case '\t':
			case 0xFEFF: // BOM
				continue;
			case '(':
			case '[':
				if (current.type == NodeType.LITERAL) {
					current = new Node(parent, current, NodeType.OPERATOR, Character.toString(c));
					current = new Node(parent, current, NodeType.GROUP, "");
					parent = current;
				} else if (c == '(') {
					current = new Node(parent, current, NodeType.GROUP, Character.toString(c));
					parent = current;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			case ')':
			case ']':
				if (parent.type != NodeType.GROUP) {
					throw createParseException("expression not closed.", s);
				} else if (current.type == NodeType.OPERATOR) {
					throw createParseException("unexpected char " + c, s);
				}
				parent = parent.parent;
				break;
			case '=':
				if (current.type == NodeType.GROUP || current.type == NodeType.OPERATOR) {
					throw createParseException("unexpected char " + c, s);
				}
				n = s.next();
				if (n == '=') {
					current = new Node(parent, current, NodeType.OPERATOR, "==");
				} else if (n == -1) {
					throw createParseException("expression not closed.", s);
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			case '<':
			case '>':
			case '&':
			case '|':
				if (current.type == NodeType.GROUP || current.type == NodeType.OPERATOR) {
					throw createParseException("unexpected char " + c, s);
				}
			case '!':
				n = s.next();
				String ope = Character.toString(c);
				if ((c != '&' && c != '|' && n == '=') || (c != '!' && n == c)) {
					if (c == '>' && n == '>') {
						n = s.next();
						if (n == c) {
							ope = ">>>";
						} else {
							s.back();
						}
					} else {
						ope += Character.toString((char)n);
					}
				} else {
					s.back();
				}
				current = new Node(parent, current, NodeType.OPERATOR, ope);
				break;
			case '*':
			case '%':
			case '^':
			case '?':
			case ':':
				if (current.type == NodeType.GROUP || current.type == NodeType.OPERATOR) {
					throw createParseException("unexpected char " + c, s);
				}
			case '+':
			case '-':
			case '~':
				current = new Node(parent, current, NodeType.OPERATOR, Character.toString(c));
				break;
			case '/':
				n = s.next();
				s.back();
				if (n == '/' || n == '*') {
					s.back();
					skipComment(s);
				} else {
					if (current.type == NodeType.GROUP || current.type == NodeType.OPERATOR) {
						throw createParseException("unexpected char " + c, s);
					}
					current = new Node(parent, current, NodeType.OPERATOR, Character.toString(c));
				}
				break;
			case '.':
				if (current.type != NodeType.LITERAL) {
					throw createParseException("unexpected char " + c, s);
				}
				current = new Node(parent, current, NodeType.OPERATOR, Character.toString(c));
				break;
			case '\'':
			case '"':
				s.back();
				current = new Node(parent, current, NodeType.STRING, parseString(s, sb));
				break;
			default:
				if (c >= '0' && c <= '9') {
					s.back();
					current = new Node(parent, current, NodeType.NUMBER, parseNumber(s, sb));
				} else {
					s.back();
					String literal = parseLiteral(s, sb);
					if ("null".equals(literal)) {
						current = new Node(parent, current, NodeType.NULL, literal);
					} else if ("true".equals(literal) || "false".equals(literal)) {
						current = new Node(parent, current, NodeType.BOOLEAN, literal);
					} else if ("instanceof".equals(literal)) {
						current = new Node(parent, current, NodeType.OPERATOR, literal);
					} else {
						current = new Node(parent, current, NodeType.LITERAL, literal);
					}
				}
			}
		}
		
		if (parent.parent != null || current.type == NodeType.OPERATOR) {
			throw createParseException("expression not closed.", s);
		}
		
		return parent;
	}
	
	private String parseString(InputSource s, StringBuilder sb) throws IOException {
		int point = 0; // 0 '"|'' 1 'c' ... '"|'' E
		sb.setLength(0);
		char start = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				continue;
			case '\\':
				if (point == 1) {
					s.back();
					sb.append(parseEscape(s));
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				continue;
			case '\'':
			case '"':
				if (point == 0) {
					start = c;
					point = 1;
					continue;
				} else if (point == 1) {
					if (start == c) {
						break loop;						
					} else {
						sb.append(c);
					}
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				continue;
			}
			if (point == 1) {
				sb.append(c);
				continue;
			}
			throw createParseException("unexpected char " + c, s);
		}
		
		if (n != start) {
			throw createParseException("string not closed", s);
		}
		return sb.toString();
	}
	
	private String parseNumber(InputSource s, StringBuilder sb) throws IOException {
		int point = 0; // 0 '0' | ('[1-9]' 1 '[0-9]*') 2 '(.)' 3 '[0-9]' 4 '[0-9]*' 5 'e|E' 6 '[+|-]' 7 '[0-9]' 8 '[0-9]*' E
		sb.setLength(0);
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF: // BOM
				break;
			case '+':
				if (point == 6) {
					sb.append(c);
					point = 7;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			case '-':
				if (point == 6) {
					sb.append(c);
					point = 7;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			case '.':
				if (point == 1 || point == 2) {
					sb.append(c);
					point = 3;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			case 'e':
			case 'E':
				if (point == 1 || point == 2 || point == 4 || point == 5) {
					sb.append(c);
					point = 6;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			default:
				if (c >= '0' && c <= '9') {
					if (point == 0) {
						sb.append(c);
						point = (c == '0') ? 2 : 1;
					} else if (point == 1 || point == 4 || point == 8) {
						sb.append(c);
					} else if (point == 3) {
						sb.append(c);
						point = 5;
					} else if (point == 6 || point == 7) {
						sb.append(c);
						point = 8;
					} else {
						throw createParseException("unexpected char " + c, s);
					}
				} else if (point == 1 || point == 2 || point == 4 || point == 5 || point == 8) {
					s.back();
					break loop;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
			}
		}
		
		return sb.toString();
	}
	
	private String parseLiteral(InputSource s, StringBuilder sb) throws IOException {
		int point = 0; // 0 'IdStart' 1 'IdPart' ... !'IdPart' E
		sb.setLength(0);

		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			if (c == 0xFEFF) continue;
			
			if (c == '\\') {
				s.back();
				c = parseEscape(s);
			}
			
			if (point == 0 && Character.isJavaIdentifierStart(c)) {
				sb.append(c);
				point = 1;
			} else if (point == 1 && (Character.isJavaIdentifierPart(c))) {
				sb.append(c);
			} else {
				s.back();
				break loop;
			}
		}
		
		return sb.toString();
	}
	
	private char parseEscape(InputSource s) throws IOException {
		int point = 0; // 0 '\' 1 'u' 2 'x' 3 'x' 4 'x' 5 'x' E
		char escape = '\0';
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			if (c == 0xFEFF) continue; // BOM
			
			if (point == 0) {
				if (c == '\\') {
					point = 1;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
			} else if (point == 1) {
				switch(c) {
				case 'b':
					escape = '\b';
					break loop;
				case 'f':
					escape = '\f';
					break loop;
				case 'n':
					escape = '\n';
					break loop;
				case 'r':
					escape = '\r';
					break loop;
				case 't':
					escape = '\t';
					break loop;
				case 'u':
					point = 2;
					break;
				default:
					escape = c;
					break loop;
				}
			} else {
				int hex = (c >= '0' && c <= '9') ? c-48 :
					(c >= 'A' && c <= 'F') ? c-65+10 :
					(c >= 'a' && c <= 'f') ? c-97+10 : -1;
				if (hex != -1) {
					escape |= (hex << ((5-point)*4));
					if (point != 5) {
						point++;
					} else {
						break loop;
					}
				} else {
					throw createParseException("illegal unicode escape " + c, s);
				}
			}
		}
		
		return escape;
	}

	private void skipComment(InputSource s) throws IOException {
		int point = 0; // 0 '/' 1 '*' 2  '*' 3 '/' E or  0 '/' 1 '/' 4  '\r|\n|\r\n' E
		
		int n = -1;
		loop:while ((n = s.next()) != -1) {
			char c = (char)n;
			switch(c) {
			case 0xFEFF:
				break;
			case '/':
				if (point == 0) {
					point = 1;
				} else if (point == 1) {
					point = 4;
				} else if (point == 3) {
					break loop;
				} else if (!(point == 2 || point == 4)) {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			case '*':
				if (point == 1) {
					point = 2;
				} else if (point == 2) {
					point = 3;
				} else if (!(point == 3 || point == 4)) {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			case '\n':
			case '\r':
				if (point == 2 || point == 3) {
					point = 2;
				} else if (point == 4) {
					break loop;
				} else {
					throw createParseException("unexpected char " + c, s);
				}
				break;
			default:
				if (point == 3) {
					point = 2;
				} else if (!(point == 2 || point == 4)) {
					throw createParseException("unexpected char " + c, s);
				}
			}
		}
	}
	
	private IllegalArgumentException createParseException(String message, InputSource s) {
		return new IllegalArgumentException("" + s.getLineNumber() + ": " + message + "\n" + s.toString() + " <- ?");
	}
	
	private boolean evaluateNot(Object a) {
		return !toBoolean(a);
	}
	
	private Object evaluateMultiply(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			return toBigDecimal(a).multiply(toBigDecimal(b));
		} else if (a == null || b == null) {
			return null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private Object evaluateDivide(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			return toBigDecimal(a).divide(toBigDecimal(b));
		} else if (a == null || b == null) {
			return null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private Object evaluateRemainder(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			return toBigDecimal(a).remainder(toBigDecimal(b));
		} else if (a == null || b == null) {
			return null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private Object evaluatePlus(Object a) {
		if (a instanceof Number) {
			return toBigDecimal(a);
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private Object evaluatePlus(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			return toBigDecimal(a).add(toBigDecimal(b));
		} else if (a instanceof Object) {
			if (b instanceof Number) {
				return toString(a) + toString(toBigDecimal(b));
			} else if (b == null) {
				return null;
			} else {
				return toString(a) + toString(b);
			}
		} else if (a == null) {
			return (b != null) ? b.toString() : null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private Object evaluateMinus(Object a) {
		if (a instanceof Number) {
			return toBigDecimal(a).negate();
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private Object evaluateMinus(Object a, Object b) {
		if (a instanceof Number && b instanceof Number) {
			return toBigDecimal(a).subtract(toBigDecimal(b));
		} else if (a == null || b == null) {
			return null;
		} else {
			throw new IllegalArgumentException();
		}
	}
	
	private boolean evaluateEquals(Object a, Object b) {
		return (a != null) ? a.equals(b) : (a == b) ;
	}
	
	private boolean evaluateShortAnd(Object a, Object b) {
		return toBoolean(a) && toBoolean(b);
	}
	
	private boolean evaluateShortOr(Object a, Object b) {
		return toBoolean(a) || toBoolean(b);
	}
	
	private boolean toBoolean(Object o) {
		if (o instanceof Boolean) {
			return (Boolean)o;
		} else {
			return o != null;
		}
	}
	
	private String toString(Object o) {
		if (o instanceof BigDecimal) {
			return ((BigDecimal)o).toPlainString();
		} else if (o == null) {
			return "";
		} else {
			return o.toString();
		}
	}
	
	private BigDecimal toBigDecimal(Object n) {
		if (n == null) {
			return null;
		} else if (n instanceof BigDecimal) {
			return (BigDecimal)n;
		} else if (n instanceof Integer) {
			return new BigDecimal((Integer)n);
		} else if (n instanceof Byte) {
			return new BigDecimal((Byte)n);
		} else if (n instanceof Short) {
			return new BigDecimal((Short)n);
		} else if (n instanceof Long) {
			return new BigDecimal((Long)n);
		} else if (n instanceof Float) {
			return new BigDecimal((Float)n);
		} else if (n instanceof Double) {
			return new BigDecimal((Double)n);
		} else if (n instanceof BigInteger) {
			return new BigDecimal((BigInteger)n);
		} else {
			throw new UnsupportedOperationException();
		}
	}
	
	private void printTree() {
		Node current = root;
		int depth = 0;
		do {
			if (current.prev != null) {
				if (current.parent == current.prev) {
					depth++; 
				} else if (current.prev.parent != null && current.parent == current.prev.parent.parent) {
					depth--;
				}
			}
			for (int j = 0; j < depth; j++) System.out.print("\t");
			System.out.println(current);
			current = current.next;
		} while (current != null);
		
		System.out.println();
	}
	
	@Override
	public String toString() {
		return expr;
	}
}

enum NodeType {
	GROUP,
	OPERATOR,
	LITERAL,
	STRING,
	NUMBER,
	BOOLEAN,
	NULL
}

class Node {
	public Node parent;
	public Node prev;
	public Node next;
	
	public NodeType type;
	
	public String text;
	public Object value;
	
	public Node(Node parent, Node prev, NodeType type, String text) {
		this.parent = parent;
		this.prev = prev;
		this.type = type;
		this.text = text;
		
		if (prev != null) prev.next = this;
	}
	
	@Override
	public String toString() {
		return "[" + type + "] " + text;
		//+ " (prev: " + ((prev != null) ? prev.text : "null") + ", next: " + ((next != null) ? next.text : "null") + ")";
	}
}
