package net.sourceforge.vrapper.vim.commands;

import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.utils.TextRange;
import net.sourceforge.vrapper.vim.EditorAdaptor;
import net.sourceforge.vrapper.vim.Options;
import net.sourceforge.vrapper.vim.commands.motions.StickyColumnPolicy;
import net.sourceforge.vrapper.vim.modes.AbstractVisualMode;
import net.sourceforge.vrapper.vim.modes.BlockwiseVisualMode;
import net.sourceforge.vrapper.vim.modes.LinewiseVisualMode;
import net.sourceforge.vrapper.vim.modes.VisualMode;

public class SelectTextObjectCommand extends CountAwareCommand {

    private final TextObject textObject;

    public SelectTextObjectCommand(final TextObject textObject) {
        this.textObject = textObject;
    }

    @Override
    public void execute(final EditorAdaptor editorAdaptor, int count) throws CommandExecutionException {
        Selection oldSelection = editorAdaptor.getSelection();
        TextRange region = textObject.getRegion(editorAdaptor, count);
        //if selection was calculated to be the same as before
        //it probably means we're chaining i{i{, keep increasing count
        //until we expand into next the region
        int orgCount = count;
        int oldRightOffset = oldSelection.getRightBound().getModelOffset();
        if (oldSelection instanceof BlockWiseSelection && oldSelection.getModelLength() > 0) {
            // BlockWiseSelection stores an exclusive range [from, to], so its selected right column
            // ends one offset past getRightBound().
            oldRightOffset++;
        }
        while(textObject instanceof DelimitedTextObject
                && oldSelection.getLeftBound().getModelOffset() <= region.getLeftBound().getModelOffset()
                && oldRightOffset >= region.getRightBound().getModelOffset()) {
            // TextObject might not use count, abort after a while to prevent an endless loop.
            // FIXME: See if we can do this chaining functionality without looping over count. 
            if (count > orgCount + 10) {
                break;
            }
            count++;
            //this will throw a CommandExecutionException and break out of the
            //while loop if no delimiter can be found larger than the current
            //selection
            region = textObject.getRegion(editorAdaptor, count);
        }

        Selection selection;
        String newMode;
        switch (textObject.getContentType(editorAdaptor.getConfiguration())) {
        case TEXT_RECTANGLE: 
            selection = new BlockWiseSelection(editorAdaptor, region.getStart(), region.getEnd());
            newMode = BlockwiseVisualMode.NAME;
            break;
        case LINES:
            /**
             * TODO: ugly casting. The problem is, that if textObject already
             * returns a LineWiseSelection, then by creating new instance of
             * LineWiseSelection from the old one we extend the selected range
             * by one line. This is due implementation of LineWiseSelection,
             * whose getEnd() points to the line AFTER the actual selection. It
             * might be solved by refactoring TextRange with introduction of
             * linewise range (by line numbers)
             */
            if (region instanceof LineWiseSelection) {
                selection = (LineWiseSelection) region;
            }
            else {
                selection = new LineWiseSelection(editorAdaptor, region.getStart(), region.getEnd());
            }
            newMode = LinewiseVisualMode.NAME;
            break;
        case TEXT:
            String selectionOption = editorAdaptor.getConfiguration().get(Options.SELECTION);
            boolean isBlockwise = BlockwiseVisualMode.NAME.equals(editorAdaptor.getCurrentModeName());
            boolean isInclusive = isBlockwise || Selection.INCLUSIVE.equals(selectionOption);
            SimpleSelection textSelection = new SimpleSelection(
                    editorAdaptor.getCursorService(), isInclusive, region);
            if (isBlockwise) {
                selection = new BlockWiseSelection(
                        editorAdaptor, textSelection.getFrom(), textSelection.getTo());
                newMode = BlockwiseVisualMode.NAME;
            } else {
                selection = textSelection;
                newMode = VisualMode.NAME;
            }
            break;
        default: throw new CommandExecutionException("WTF");
        }
        // Open a closed folds if we happen to select a text object hidden by a fold.
        Position caretPos = selection.getTo();
        if (caretPos.getViewOffset() < 0) {
            editorAdaptor.getViewportService().exposeModelPosition(caretPos);
        }
        //Make sure to update the sticky column.
        editorAdaptor.setPosition(caretPos, StickyColumnPolicy.ON_CHANGE);
        editorAdaptor.setSelection(selection);
        editorAdaptor.changeMode(newMode, AbstractVisualMode.FIX_SELECTION_HINT);
    }

    @Override
    public CountAwareCommand repetition() {
        return null;
    }

}
