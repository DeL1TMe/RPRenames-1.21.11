package net.minecraft.text;

import com.google.common.collect.Lists;
import com.mojang.brigadier.Message;
import com.mojang.datafixers.util.Either;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.object.TextObjectContents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkPos;
import org.jspecify.annotations.Nullable;

/**
 * A text. Can be converted to and from JSON format.
 * 
 * <p>Each text has a tree structure, embodying all its {@link
 * #getSiblings() siblings}. To iterate contents in the text and all
 * its siblings, call {@code visit} methods.
 * 
 * <p>This interface does not expose mutation operations. For mutation,
 * refer to {@link MutableText}.
 * 
 * @see MutableText
 */
public interface Text extends Message, StringVisitable {
	/**
	 * Returns the style of this text.
	 */
	Style getStyle();

	/**
	 * {@return the content of the text}
	 */
	TextContent getContent();

	@Override
	default String getString() {
		return StringVisitable.super.getString();
	}

	/**
	 * Returns the full string representation of this text, truncated beyond
	 * the supplied {@code length}.
	 * 
	 * @param length the max length allowed for the string representation of the text
	 */
	default String asTruncatedString(int length) {
		StringBuilder stringBuilder = new StringBuilder();
		this.visit(string -> {
			int j = length - stringBuilder.length();
			if (j <= 0) {
				return TERMINATE_VISIT;
			} else {
				stringBuilder.append(string.length() <= j ? string : string.substring(0, j));
				return Optional.empty();
			}
		});
		return stringBuilder.toString();
	}

	/**
	 * Returns the siblings of this text.
	 */
	List<Text> getSiblings();

	/**
	 * {@return the string of the literal text, or {@code null} if this text is not
	 * a literal}
	 * 
	 * <p>A literal text is an unstyled {@link PlainTextContent} without any siblings.
	 * Such texts are serialized as a string instead of an object.
	 */
	@Nullable
	default String getLiteralString() {
		return this.getContent() instanceof PlainTextContent plainTextContent && this.getSiblings().isEmpty() && this.getStyle().isEmpty()
			? plainTextContent.string()
			: null;
	}

	/**
	 * Copies the text's content, excluding the styles or siblings.
	 */
	default MutableText copyContentOnly() {
		return MutableText.of(this.getContent());
	}

	/**
	 * Copies the text's content, the style, and the siblings.
	 * 
	 * <p>A shallow copy is made for the siblings.
	 */
	default MutableText copy() {
		return new MutableText(this.getContent(), new ArrayList(this.getSiblings()), this.getStyle());
	}

	OrderedText asOrderedText();

	@Override
	default <T> Optional<T> visit(StringVisitable.StyledVisitor<T> styledVisitor, Style style) {
		Style style2 = this.getStyle().withParent(style);
		Optional<T> optional = this.getContent().visit(styledVisitor, style2);
		if (optional.isPresent()) {
			return optional;
		} else {
			for (Text text : this.getSiblings()) {
				Optional<T> optional2 = text.visit(styledVisitor, style2);
				if (optional2.isPresent()) {
					return optional2;
				}
			}

			return Optional.empty();
		}
	}

	@Override
	default <T> Optional<T> visit(StringVisitable.Visitor<T> visitor) {
		Optional<T> optional = this.getContent().visit(visitor);
		if (optional.isPresent()) {
			return optional;
		} else {
			for (Text text : this.getSiblings()) {
				Optional<T> optional2 = text.visit(visitor);
				if (optional2.isPresent()) {
					return optional2;
				}
			}

			return Optional.empty();
		}
	}

	default List<Text> withoutStyle() {
		return this.getWithStyle(Style.EMPTY);
	}

	default List<Text> getWithStyle(Style style) {
		List<Text> list = Lists.<Text>newArrayList();
		this.visit((styleOverride, text) -> {
			if (!text.isEmpty()) {
				list.add(literal(text).fillStyle(styleOverride));
			}

			return Optional.empty();
		}, style);
		return list;
	}

	/**
	 * {@return whether the text contains {@code text}, without considering styles}
	 */
	default boolean contains(Text text) {
		if (this.equals(text)) {
			return true;
		} else {
			List<Text> list = this.withoutStyle();
			List<Text> list2 = text.getWithStyle(this.getStyle());
			return Collections.indexOfSubList(list, list2) != -1;
		}
	}

	/**
	 * Creates a literal text with the given string as content.
	 */
	static Text of(@Nullable String string) {
		return (Text)(string != null ? literal(string) : ScreenTexts.EMPTY);
	}

	static MutableText literal(String string) {
		return MutableText.of(PlainTextContent.of(string));
	}

	static MutableText translatable(String key) {
		return MutableText.of(new TranslatableTextContent(key, null, TranslatableTextContent.EMPTY_ARGUMENTS));
	}

	/**
	 * {@return a translatable text with arguments}
	 * 
	 * <p>The arguments passed <strong>must be either numbers, booleans, strings, or another
	 * {@link Text}</strong>. Use {@link #stringifiedTranslatable} to construct texts with
	 * other objects as arguments. Alternatively, convert them using static methods here
	 * like {@link #of(Identifier)}.
	 */
	static MutableText translatable(String key, Object... args) {
		return MutableText.of(new TranslatableTextContent(key, null, args));
	}

	/**
	 * {@return a translatable text with arguments}
	 * 
	 * <p>Arguments that are not numbers, booleans, strings, or another {@link Text} are
	 * converted to strings using {@link String#valueOf(Object)}.
	 */
	static MutableText stringifiedTranslatable(String key, Object... args) {
		for (int i = 0; i < args.length; i++) {
			Object object = args[i];
			if (!TranslatableTextContent.isPrimitive(object) && !(object instanceof Text)) {
				args[i] = String.valueOf(object);
			}
		}

		return translatable(key, args);
	}

	static MutableText translatableWithFallback(String key, @Nullable String fallback) {
		return MutableText.of(new TranslatableTextContent(key, fallback, TranslatableTextContent.EMPTY_ARGUMENTS));
	}

	static MutableText translatableWithFallback(String key, @Nullable String fallback, Object... args) {
		return MutableText.of(new TranslatableTextContent(key, fallback, args));
	}

	static MutableText empty() {
		return MutableText.of(PlainTextContent.EMPTY);
	}

	static MutableText keybind(String string) {
		return MutableText.of(new KeybindTextContent(string));
	}

	static MutableText nbt(String rawPath, boolean interpret, Optional<Text> separator, NbtDataSource dataSource) {
		return MutableText.of(new NbtTextContent(rawPath, interpret, separator, dataSource));
	}

	static MutableText score(ParsedSelector selector, String objective) {
		return MutableText.of(new ScoreTextContent(Either.left(selector), objective));
	}

	static MutableText score(String name, String objective) {
		return MutableText.of(new ScoreTextContent(Either.right(name), objective));
	}

	static MutableText selector(ParsedSelector selector, Optional<Text> separator) {
		return MutableText.of(new SelectorTextContent(selector, separator));
	}

	static MutableText object(TextObjectContents object) {
		return MutableText.of(new ObjectTextContent(object));
	}

	/**
	 * {@return a {@linkplain #literal literal} text representing {@code date}}
	 * 
	 * @implNote The date is converted to a string using {@link Date#toString}.
	 * Notably, it does not localize the date format.
	 */
	static Text of(Date date) {
		return literal(date.toString());
	}

	/**
	 * {@return a text with {@code message}}
	 * 
	 * <p>If a text instance is passed, this method returns {@code message} itself;
	 * otherwise this creates a new literal text with the message content.
	 */
	static Text of(Message message) {
		return (Text)(message instanceof Text text ? text : literal(message.getString()));
	}

	/**
	 * {@return a text representing {@code uuid}}
	 * 
	 * <p>The UUID is converted to a string like {@code 12345678-90AB-CDEF-1234-567890ABCDEF}.
	 * 
	 * @see UUID#toString
	 */
	static Text of(UUID uuid) {
		return literal(uuid.toString());
	}

	/**
	 * {@return a text representing {@code id}}
	 * 
	 * <p>The returned text has the format {@code namespace:path}.
	 * Namespace is always included.
	 */
	static Text of(Identifier id) {
		return literal(id.toString());
	}

	/**
	 * {@return a text representing chunk {@code pos}}
	 * 
	 * <p>The returned text has the format {@code [X, Z]}.
	 */
	static Text of(ChunkPos pos) {
		return literal(pos.toString());
	}

	static Text of(URI uri) {
		return literal(uri.toString());
	}
}
