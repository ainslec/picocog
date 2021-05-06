/*
 * Copyright 2017 - 2021, Chris Ainsley
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ainslec.picocog;

import java.util.ArrayList;
import java.util.List;

/**
 * A simple writer class that allows for easy indentation, designed to be used for generating source code.
 *
 * @author Chris Ainsley
 */
@SuppressWarnings({"unused", "UnusedReturnValue"})
public class PicoWriter implements Item {
	private static final String SEP = "\n";
	private static final String TAB = "    ";

	private int indents = 0;
	private int lines = 0;
	private boolean generateIfEmpty = true;
	private boolean generate = true;
	private boolean normalize = false;

	private boolean dirty = false;
	private final List<String[]> rows = new ArrayList<>();
	private final List<Item> content = new ArrayList<>();
	private final StringBuilder sb = new StringBuilder();
	private String indent = TAB;

	public PicoWriter() {}

	public PicoWriter(String indentText) {
		indent = indentText == null ? TAB : indentText;
	}

	private PicoWriter(int initialIndent, String indentText) {
		indents = Math.max(initialIndent, 0);
		indent = indentText == null ? TAB : indentText;
	}

	public void indentRight() {
		flushRows();
		indents++;
	}

	public void indentLeft() {
		flushRows();
		indents--;
		if (indents < 0)
			throw new RuntimeException("Local indent cannot be less than zero");
	}

	public final PicoWriter createDeferredWriter() {
		if (sb.length() > 0) {
			flush();
			lines++;
		}
		final PicoWriter inner = new PicoWriter(indents, indent);
		content.add(inner);
		lines++;
		return inner;
	}

	public final PicoWriter writeln(PicoWriter inner) {
		if (sb.length() > 0) {
			flush();
			lines++;
		}
		adjustIndents(inner, this.indents, this.indent);
		content.add(inner);
		lines++;
		return this;
	}

	private void adjustIndents(PicoWriter inner, int indents, String indent) {
		if (inner != null) {
			for (Item item : inner.content)
				if (item instanceof PicoWriter)
					adjustIndents((PicoWriter) item, indents, indent);
				else if (item instanceof Line) {
					final Line line = (Line) item;
					line.indent = line.indent + indents;
				}
			inner.indent = indent;
		}
	}

	public PicoWriter writeln_r(String string) {
		writeln(string);
		indentRight();
		return this;
	}

	public PicoWriter writeln_l(String string) {
		flushRows();
		indentLeft();
		writeln(string);
		return this;
	}

	public PicoWriter writeln_lr(String string) {
		flushRows();
		indentLeft();
		writeln(string);
		indentRight();
		return this;
	}

	public PicoWriter writeln(String string) {
		lines++;
		sb.append(string);
		flush();
		return this;
	}

	/**
	 * Writes multiple strings.
	 * 
	 * @param strings string array representing columns at the current indentation level
	 * @return the current instance of the {@link PicoWriter} object
	 */
	public PicoWriter writeln(String... strings) {
		rows.add(strings);
		dirty = true;
		lines++;
		return this;
	}

	/**
	 * Writes a line, indents, then creates a new deferred writer 
	 * before returning to the previous indent level.
	 * 
	 * @param start line before indentation
	 * @param end line after indentation
	 * @return a deferred writer that has been indented
	 */
	public PicoWriter indentDeferredWriter(String start, String end) {
		writeln(start);
		indentRight();
		PicoWriter inner = createDeferredWriter();
		indentLeft();
		writeln(end);
		dirty = true;
		lines += 2;
		return inner;
	}

	public boolean isEmpty() {
		return lines == 0;
	}

	public void write(String string)  {
		lines++;
		dirty = true;
		sb.append(string);
	}

	private static void writeIndentedLine(StringBuilder sb, int indentBase, String indentText, String line) {
		for (int indentIndex = 0; indentIndex < indentBase; indentIndex++)
			sb.append(indentText);
		sb.append(line);
		sb.append(SEP);
	}

	private boolean render(StringBuilder sb, int base, boolean normalize, boolean blank) {
		if (dirty) flush();
		if (!isGenerate() || (!isGenerateIfEmpty() && isMethodBodyEmpty()))
			return blank;

		// TODO :: Will make this configurable
		for (Item item : content) {
			if (item instanceof Line) {
				final Line line = (Line) item;
				final String text = line.text;
				final int level = base + line.indent;
				final boolean currentBlank = text.length() == 0;

				if (!normalize && !blank && !currentBlank)
					writeIndentedLine(sb, level, indent, text);
				blank = currentBlank;
			}
			else if (item instanceof PicoWriter)
				blank = ((PicoWriter) item).render(
						sb, base, 
						normalize, blank
				);
			else
				sb.append(item.toString());
		}
		return blank;
	}

	public boolean isMethodBodyEmpty() {
		return content.size() == 0 && sb.length() == 0;
	}

	public boolean isGenerateIfEmpty() {
		return generateIfEmpty;
	}

	public void setGenerateIfEmpty(boolean generateIfEmpty) {
		this.generateIfEmpty  = generateIfEmpty;
	}

	public boolean isGenerate() {
		return generate;
	}

	public void setGenerate(boolean generate) {
		this.generate = generate;
	}

	private void flush() {
		flushRows();
		content.add(new Line(sb.toString(), indents));
		sb.setLength(0);
		dirty = false;
	}

	private void flushRows() {
		if (rows.size() > 0) {
			final ArrayList<Integer> maxes = new ArrayList<>();
			for (String[] columns : rows)
				for (int i = 0; i < columns.length; i++) {
					final String value = columns[i];
					final int length = value == null ? 0 : value.length();
					if (maxes.size() < i + 1)
						maxes.add(length);
					else if (maxes.get(i) < length)
						maxes.set(i, length);
				}

			final StringBuilder sb = new StringBuilder();

			for (String[] columns : rows) {
				for (int i=0; i < columns.length; i++) {
					final String value = columns[i];
					final int width = value == null ? 0 : value.length();
					final int max = maxes.get(i);
					sb.append(value == null ? "" : value);

					if (width < max)
						for (int j = width; j < max; j++)
							sb.append(" "); // right pad
				}
				content.add(new Line(sb.toString(), indents));
				sb.setLength(0);
			}
			rows.clear();
		}
	}

	public void setNormalize(boolean normalize) {
		this.normalize = normalize;
	}

	public String toString(int indentBase) {
		final StringBuilder sb = new StringBuilder();
		render(sb, indentBase, normalize, false);
		return sb.toString();
	}

	public String toString() {
		return toString(0);
	}

	private static class Line implements Item {
		private final String text;
		private int indent;

		public Line(String text, int indent) {
			this.text = text;
			this.indent = indent;
		}

		@Override
		public String toString() {
			return indent + ":" + text;
		}
	}
}
