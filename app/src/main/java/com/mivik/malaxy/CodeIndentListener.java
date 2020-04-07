package com.mivik.malaxy;

import com.mivik.malax.BaseMalax;
import com.mivik.malax.WrappedEditable;

import static com.mivik.malax.BaseMalax.Cursor;

public class CodeIndentListener implements WrappedEditable.EditActionListener {
	@Override
	public boolean beforeAction(WrappedEditable wrappedEditable, WrappedEditable.EditAction editAction) {
		if (editAction instanceof WrappedEditable.InsertCharAction) {
			WrappedEditable<?>.InsertCharAction action = (WrappedEditable<?>.InsertCharAction) editAction;
			BaseMalax malax = (BaseMalax) wrappedEditable.unwrap();
			if (action.ch == '\n') {
				final int line = ((Cursor) action.lef).line;
				final int len = malax.getLineManager().getTrimmed(line);
				Cursor cursor = new Cursor(line, 0);
				for (; cursor.column < len; cursor.column++) if (malax.charAt(cursor) != '\t') break;
				int tabs = cursor.column;
				if (len > 0) {
					cursor.column = len - 1;
					if (malax.charAt(cursor) == '{') ++tabs;
				}
				if (tabs == 0) return false;
				StringBuilder b = new StringBuilder(tabs + 1);
				b.append('\n');
				for (int i = 0; i < tabs; i++) b.append('\t');
				wrappedEditable.insert(action.lef, b.toString());
				return true;
			}
		}
		return false;
	}

	@Override
	public void afterAction(WrappedEditable wrappedEditable, WrappedEditable.EditAction editAction) {
	}
}