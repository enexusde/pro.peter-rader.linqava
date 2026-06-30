/*
 *  _    ___ _  _  ___https://www.e-nexus.de./
 * | |  |_ _| \| |/ _ \ __ ___ ____ _ 
 * | |__ | || .` | (_) / _` \ V / _` |
 * |____|___|_|\_|\__\_\__,_|\_/\__,_|
 * Queries may not be Strings! (c) 2026
 */
package linqava;

/**
 * Reference of the Unicode glyphs linqava uses to express SQL operators as Java method names.
 *
 * <p>SQL operators live in Unicode categories ({@code Sm}/{@code So}/{@code Sk}) that are <b>not</b>
 * legal Java identifiers, so each is mapped to a visually similar character that <b>is</b> a legal
 * identifier part ({@link Character#isJavaIdentifierPart(int)} {@code == true}). The connector for
 * multi-word keywords ("left join" &rarr; {@code LEFT‿JOIN}) is a connector-punctuation glyph.</p>
 *
 * <table border="1">
 *   <caption>SQL token &rarr; Java glyph</caption>
 *   <tr><th>SQL</th><th>Glyph</th><th>Code point</th><th>Unicode name</th><th>Category</th></tr>
 *   <tr><td>=</td>  <td>ᆖ</td><td>U+4E8C</td><td>CJK IDEOGRAPH "TWO" (two strokes)</td><td>Lo</td></tr>
 *   <tr><td>&lt;</td><td>ᐸ</td><td>U+1438</td><td>CANADIAN SYLLABICS PA</td><td>Lo</td></tr>
 *   <tr><td>&gt;</td><td>ᐳ</td><td>U+1433</td><td>CANADIAN SYLLABICS PO</td><td>Lo</td></tr>
 *   <tr><td>&lt;=</td><td>ᐸᆖ</td><td>U+1438 U+4E8C</td><td>(&lt; followed by =)</td><td>Lo</td></tr>
 *   <tr><td>&gt;=</td><td>ᐳᆖ</td><td>U+1433 U+4E8C</td><td>(&gt; followed by =)</td><td>Lo</td></tr>
 *   <tr><td>&lt;&gt;</td><td>ᐸᐳ</td><td>U+1438 U+1433</td><td>(&lt; followed by &gt;)</td><td>Lo</td></tr>
 *   <tr><td>+</td>  <td>ᐩ</td><td>U+1429</td><td>CANADIAN SYLLABICS FINAL PLUS</td><td>Lo</td></tr>
 *   <tr><td>-</td>  <td>ｰ</td><td>U+FF70</td><td>HALFWIDTH KATAKANA-HIRAGANA PROLONGED SOUND MARK</td><td>Lm</td></tr>
 *   <tr><td>*</td>  <td>ᚷ</td><td>U+16B7</td><td>RUNIC LETTER GEBO GYFU</td><td>Lo</td></tr>
 *   <tr><td>/</td>  <td>ノ</td><td>U+30CE</td><td>KATAKANA LETTER NO</td><td>Lo</td></tr>
 *   <tr><td>(space in "left join")</td><td>‿</td><td>U+203F</td><td>UNDERTIE</td><td>Pc</td></tr>
 * </table>
 *
 * <p>Note: U+A78A "MODIFIER LETTER SHORT EQUALS SIGN" looks like the perfect "=" but is category
 * {@code Sk} and therefore rejected by Java; hence {@code ᆖ} (U+4E8C) is used for "=".</p>
 */
public final class Glyphs {

	private Glyphs() {
	}

	/** Glyph for {@code =} (U+4E8C). */
	public static final String EQ  = "ᆖ";
	/** Glyph for {@code <} (U+1438). */
	public static final String LT  = "ᐸ";
	/** Glyph for {@code >} (U+1433). */
	public static final String GT  = "ᐳ";
	/** Glyph for {@code <=} (U+1438 U+4E8C). */
	public static final String LE  = "ᐸᆖ";
	/** Glyph for {@code >=} (U+1433 U+4E8C). */
	public static final String GE  = "ᐳᆖ";
	/** Glyph for {@code <>} (U+1438 U+1433). */
	public static final String NE  = "ᐸᐳ";
	/** Glyph for {@code +} (U+1429). */
	public static final String ADD = "ᐩ";
	/** Glyph for {@code -} (U+FF70). */
	public static final String SUB = "ｰ";
	/** Glyph for {@code *} (U+16B7). */
	public static final String MUL = "ᚷ";
	/** Glyph for {@code /} (U+30CE). */
	public static final String DIV = "ノ";

	/** Connector replacing the space inside multi-word keywords, e.g. "left join" -> LEFT‿JOIN. */
	public static final String JOIN_CONNECTOR = "‿"; // ‿
}
