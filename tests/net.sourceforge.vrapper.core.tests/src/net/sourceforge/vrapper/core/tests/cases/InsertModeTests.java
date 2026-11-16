package net.sourceforge.vrapper.core.tests.cases;

import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.parseKeyStrokes;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.After;
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

	@After
	public void clearInsertMappings() {
		adaptor.changeModeSafely(NormalMode.NAME);
		type(parseKeyStrokes(":imapclear<CR>"));
	}

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

	@Test
	public void test_ImapPendingClearedAcrossContentAssistMode() {
		installSaneRegisterManager();
		adaptor.changeModeSafely(NormalMode.NAME);
		type(parseKeyStrokes(":inoremap jj <LT>ESC<GT><CR>"));

		setBuffer("");
		type(parseKeyStrokes("ij"));
		assertEquals("j", getBuffer());

		// Eclipse ContentAssistMode activates and inserts completion text
		adaptor.changeModeSafely(ContentAssistMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);
		content.replace(1, 0, "ava");
		adaptor.changeModeSafely(InsertMode.NAME,
				InsertMode.DONT_MOVE_CURSOR, InsertMode.RESUME_ON_MODE_ENTER);

		// Typing 'j' after autocomplete must not pair with the stale 'j' or backspace "ava"
		type(parseKeyStrokes("j"));
		assertEquals(InsertMode.NAME, adaptor.getCurrentModeName());
		assertEquals("javaj", getBuffer());

		// Continuing with 'a' breaks the pending single 'j', and a fresh 'jj' exits cleanly
		type(parseKeyStrokes("ajj"));
		assertEquals(NormalMode.NAME, adaptor.getCurrentModeName());
		assertEquals("javaja", getBuffer());
	}

	@Test
	public void test_ImapPendingClearedOnExternalTextInsertionInInsertMode() {
		installSaneRegisterManager();
		adaptor.changeModeSafely(NormalMode.NAME);
		type(parseKeyStrokes(":inoremap jj <LT>ESC<GT><CR>"));

		setBuffer("");
		type(parseKeyStrokes("ihelloj"));
		assertEquals("helloj", getBuffer());

		// LinkedMode / template / inline completion advances buffer inside InsertMode
		content.replace(6, 0, "ava");

		type(parseKeyStrokes("j"));
		assertEquals(InsertMode.NAME, adaptor.getCurrentModeName());
		assertEquals("hellojavaj", getBuffer());

		type(parseKeyStrokes("xjj"));
		assertEquals(NormalMode.NAME, adaptor.getCurrentModeName());
		assertEquals("hellojavajx", getBuffer());
	}

	@Test
	public void test_ImapPendingClearedOnArrowResetEditingSession() {
		installSaneRegisterManager();
		adaptor.changeModeSafely(NormalMode.NAME);
		type(parseKeyStrokes(":inoremap jj <LT>ESC<GT><CR>"));

		setBuffer("");
		type(parseKeyStrokes("iabj"));
		type(parseKeyStrokes("<LEFT><RIGHT>j"));
		assertEquals(InsertMode.NAME, adaptor.getCurrentModeName());
		assertEquals("abjj", getBuffer());
	}

	@Test
	public void test_ImapBacktrackingWorksInRealInsertMode() {
		installSaneRegisterManager();
		adaptor.changeModeSafely(NormalMode.NAME);
		type(parseKeyStrokes(":inoremap jjj <LT>ESC<GT><CR>"));
		type(parseKeyStrokes(":inoremap jk X<CR>"));

		setBuffer("");
		type(parseKeyStrokes("ijjxjk<ESC>"));
		assertEquals("jjxX", getBuffer());
	}
}
