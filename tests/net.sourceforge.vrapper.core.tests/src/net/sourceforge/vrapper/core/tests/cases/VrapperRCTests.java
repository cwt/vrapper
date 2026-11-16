package net.sourceforge.vrapper.core.tests.cases;

import static net.sourceforge.vrapper.keymap.vim.ConstructorWrappers.key;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;

import org.junit.Test;

import net.sourceforge.vrapper.keymap.KeyMap;
import net.sourceforge.vrapper.keymap.KeyStroke;
import net.sourceforge.vrapper.keymap.Remapping;
import net.sourceforge.vrapper.testutil.VimTestCase;
import net.sourceforge.vrapper.vim.commands.Command;
import net.sourceforge.vrapper.vim.modes.NormalMode;
import net.sourceforge.vrapper.vim.modes.commandline.CommandLineMode;
import net.sourceforge.vrapper.vim.modes.commandline.CommandLineParser;

public class VrapperRCTests extends VimTestCase {

    @Test
    public void testVrapperRCParsing() throws Exception {
        File config = new File("test-resources/vrapperrc/sample.vrapperrc");
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(config, StandardCharsets.UTF_8));
            String line;
            CommandLineMode commandLineMode = new CommandLineMode(adaptor);
            CommandLineParser parser = commandLineMode.createParser();
            while((line = reader.readLine()) != null) {
                Command c = parser.parseAndExecute(null, line.trim());
                if (c != null) {
                    c.execute(adaptor);
                }
            }
        } finally {
            if(reader != null) {
                    reader.close();
            }
        }
        KeyMap map = keyMapProvider.getKeyMap(NormalMode.KEYMAP_NAME);
        assertMappingEquals(map.press(key('"')).getValue(), key('1'));
        assertMappingEquals(map.press(key('\u00e9')).getValue(), key('d'), key('d'));
        assertMappingEquals(map.press(key('\u00e4')).getValue(), key('z'), key('z'));
    }

    @Test
    public void testNestedUnsupportedBlocksInSourceConfigurationFile() throws Exception {
        String content = ""
                + "if has('gui_running')\n"
                + "  if exists('+colorcolumn')\n"
                + "    \" endif\n"
                + "    nnoremap A dd\n"
                + "  endif\n"
                + "  nnoremap K dd\n"
                + "  try\n"
                + "    try\n"
                + "      nnoremap T dd\n"
                + "    endtry\n"
                + "  catch\n"
                + "  endtry\n"
                + "endif\n"
                + ":if(1)\n"
                + "  func! OuterFunc()\n"
                + "    function InnerFunc()\n"
                + "      nnoremap F dd\n"
                + "    endfunction\n"
                + "    nnoremap M dd\n"
                + "  endf\n"
                + ":endif\n"
                + "nnoremap Q yy\n";

        Path tempRc = Files.createTempFile("vrapperrc-nested-", ".rc");
        try {
            Files.write(tempRc, content.getBytes(StandardCharsets.UTF_8));
            adaptor.changeModeSafely(NormalMode.NAME);
            assertTrue(adaptor.sourceConfigurationFile(tempRc.toAbsolutePath().toString()));
        } finally {
            Files.deleteIfExists(tempRc);
        }

        KeyMap map = keyMapProvider.getKeyMap(NormalMode.KEYMAP_NAME);
        assertNull("Inner if mapping should be skipped", map.press(key('A')));
        assertNull("Outer if mapping after inner endif should still be skipped", map.press(key('K')));
        assertNull("Nested try mapping should be skipped", map.press(key('T')));
        assertNull("Nested func mapping should be skipped", map.press(key('F')));
        assertNull("Outer func mapping after inner endfunction should still be skipped", map.press(key('M')));
        assertMappingEquals(map.press(key('Q')).getValue(), key('y'), key('y'));
    }

    private void assertMappingEquals(Remapping re, KeyStroke... strokes) {
        Iterator<KeyStroke> it = re.getKeyStrokes().iterator();
        for (KeyStroke s : strokes) {
            assertTrue(it.hasNext());
            assertEquals(s, it.next());
        }
        assertFalse(it.hasNext());
    }

}
