package net.sourceforge.vrapper.core.tests.cases;

import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.parseKeyStrokes;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import net.sourceforge.vrapper.platform.CursorService;
import net.sourceforge.vrapper.testutil.VimTestCase;
import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.vim.modes.ContentAssistMode;
import net.sourceforge.vrapper.vim.modes.InsertMode;
import net.sourceforge.vrapper.vim.modes.NormalMode;

public class InsertModeTests extends VimTestCase {

	@Test
	public void test_CtrlC_exits() {
		adaptor.changeModeSafely(InsertMode.NAME);
		assertEquals(InsertMode.NAME, adaptor.getCurrentModeName());
		type(parseKeyStrokes("<C-c>"));
		assertEquals(NormalMode.NAME, adaptor.getCurrentModeName());
	}

	@Test
	public void test_ResumeOnModeEnter_preservesCount() {
		installSaneRegisterManager();
		adaptor.changeModeSafely(NormalMode.NAME);

		setBuffer("");
		type(parseKeyStrokes("3ia"));
		adaptor.changeModeSafely(ContentAssistMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);
		adaptor.changeModeSafely(InsertMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);
		type(parseKeyStrokes("b<ESC>"));
		assertEquals("ababab", getBuffer());
	}

	@Test
	public void test_ResumeOnModeEnter_preservesRepetitionCommand() {
		installSaneRegisterManager();
		adaptor.changeModeSafely(NormalMode.NAME);

		setBuffer("one\nfour");
		type(parseKeyStrokes("otwo"));
		adaptor.changeModeSafely(ContentAssistMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);
		adaptor.changeModeSafely(InsertMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);
		type(parseKeyStrokes("<ESC>."));
		assertEquals("one\ntwo\ntwo\nfour", getBuffer());
	}

	@Test
	public void test_ResumeOnModeEnter_preservesBlockInsertOnLeaveHintAndUnlocksHistory() {
		installSaneRegisterManager();
		adaptor.changeModeSafely(NormalMode.NAME);

		// TestCursorAndSelection doesn't store marks; track LAST_CHANGE_START on the spy
		// so BlockwiseRepeatInsertCommand can reposition cleanly on <Esc>.
		final Position[] lastChangeStart = new Position[1];
		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) {
				lastChangeStart[0] = (Position) invocation.getArguments()[1];
				return null;
			}
		}).when(cursorAndSelection).setMark(eq(CursorService.LAST_CHANGE_START), any(Position.class));
		when(cursorAndSelection.getMark(CursorService.LAST_CHANGE_START))
				.thenAnswer(new Answer<Position>() {
					@Override
					public Position answer(InvocationOnMock invocation) {
						return lastChangeStart[0];
					}
				});

		setBuffer("one\ntwo\nthree");
		type(parseKeyStrokes("jj<C-v>kkIfoo"));
		adaptor.changeModeSafely(ContentAssistMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);
		adaptor.changeModeSafely(InsertMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);
		type(parseKeyStrokes("bar<ESC>"));

		assertEquals("foobarone\nfoobartwo\nfoobarthree", getBuffer());
		verify(historyService).lock("block-action");
		verify(historyService).unlock("block-action");
	}
}
