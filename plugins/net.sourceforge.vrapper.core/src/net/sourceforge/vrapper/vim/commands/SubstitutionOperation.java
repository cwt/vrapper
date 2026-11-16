package net.sourceforge.vrapper.vim.commands;

import net.sourceforge.vrapper.platform.Configuration;
import net.sourceforge.vrapper.platform.CursorService;
import net.sourceforge.vrapper.platform.SearchAndReplaceService;
import net.sourceforge.vrapper.platform.TextContent;
import net.sourceforge.vrapper.utils.ContentType;
import net.sourceforge.vrapper.utils.LineInformation;
import net.sourceforge.vrapper.utils.LineRange;
import net.sourceforge.vrapper.utils.Position;
import net.sourceforge.vrapper.utils.SimpleLineRange;
import net.sourceforge.vrapper.utils.SubstitutionDefinition;
import net.sourceforge.vrapper.vim.EditorAdaptor;
import net.sourceforge.vrapper.vim.commands.BlockWiseSelection.TextBlock;

/**
 * Perform a substitution on a range of lines.  Can be current line,
 * all lines, or any range in between.
 * For example, :s/foo/blah/g or :%s/foo/blah/g or :2,5s/foo/blah/g
 */
public class SubstitutionOperation extends AbstractLinewiseOperation {

	private SubstitutionDefinition subDef;

	public SubstitutionOperation(SubstitutionDefinition substitution) {
		this.subDef = substitution;
	}

	public SubstitutionDefinition getDefinition() {
		return subDef;
	}

    @Override
    public LineRange getDefaultRange(EditorAdaptor editorAdaptor, int count, Position currentPos)
            throws CommandExecutionException {
        return SimpleLineRange.singleLine(editorAdaptor, currentPos);
    }

    @Override
    public void execute(EditorAdaptor editorAdaptor, LineRange range) throws CommandExecutionException {
        TextContent model = editorAdaptor.getModelContent();
		int numReplaces = 0;
		int lineReplaceCount = 0;
		VisualScope visualScope = subDef.find.contains("\\%V")
				? VisualScope.create(editorAdaptor, model)
				: null;
		if (range.getStartLine() == range.getEndLine()) {
			LineInformation currentLine = model.getLineInformation(range.getStartLine());
			//begin and end compound change so a single 'u' undoes all replaces
			editorAdaptor.getHistory().beginCompoundChange();
			numReplaces = performReplace(currentLine, subDef.find, subDef.replace, subDef.flags, editorAdaptor, visualScope);
			editorAdaptor.getHistory().endCompoundChange();
		}
		else {
			LineInformation line;
			int lineChanges = 0;

			int endLine = range.getEndLine();
			int totalLines = model.getNumberOfLines();
			int lineDiff;
			//perform search individually on each line in the range
			//(so :%s without 'g' flag runs once on each line)
			editorAdaptor.getHistory().beginCompoundChange();
			for(int i=range.getStartLine(); i <= endLine; i++) {
				line = model.getLineInformation(i);
				lineChanges = performReplace(line, subDef.find, subDef.replace, subDef.flags, editorAdaptor, visualScope);
				if(lineChanges > 0) {
					lineReplaceCount++;
				}
				numReplaces += lineChanges;

				lineDiff = model.getNumberOfLines() - totalLines;
				if(lineDiff > 0) {
				    //lines were introduced as a result of this replacement
				    //skip over those introduced lines and move on to the next intended line
				    i += lineDiff;
				    endLine += lineDiff;
				    if (visualScope != null) {
				        visualScope.onLinesInserted(lineDiff);
				    }
				    totalLines += lineDiff;
				}
			}
			editorAdaptor.getHistory().endCompoundChange();
		}
		
		if (numReplaces == 0) {
			editorAdaptor.getUserInterfaceService().setErrorMessage("'"+subDef.find+"' not found");
		} else {
			String message = numReplaces + " ";
			message += subDef.flags.contains("n") ? "matches" : "substitutions";
			if (lineReplaceCount > 1) {
				message += " on " + lineReplaceCount + " lines";
			} else {
				message += " on 1 line";
			}
			editorAdaptor.getUserInterfaceService().setInfoMessage(message);
		}
		
		//enable '&', 'g&', and ':s' features
		// [TODO] Move to substitution parser
		editorAdaptor.getRegisterManager().setLastSubstitution(this);
	}
    
    private int performReplace(LineInformation line, String find,
    		String replace, String flags, EditorAdaptor editorAdaptor, VisualScope visualScope) {
    	//Eclipse regex doesn't handle '^' and '$' like Vim does.
    	//Time for some special cases!
		if(find.equals("^") || find.equals("$")) {
			replace = convertToPlatformNewline(editorAdaptor, replace);
			// remove any escape characters since we're inserting this string literally (not using regex)
            replace = replace.replaceAll("((^|[^\\\\])(\\\\\\\\)*)(\\\\)", "$1");
			// insert the text at the beginning (or end) of the line
			editorAdaptor.getModelContent().replace(find.equals("^") ? line.getBeginOffset() : line.getEndOffset(), 0, replace);
			return 1;
		}
		else {
		    int start = line.getBeginOffset();
		    int end = line.getEndOffset();
		    if (visualScope != null) {
		        find = find.replace("\\%V", "");
		        int[] bounds = visualScope.resolveLineBounds(line, editorAdaptor);
		        if (bounds == null) {
		            return 0;
		        }
		        start = bounds[0];
		        end = bounds[1];
		    }
			//let Eclipse handle the regex
			SearchAndReplaceService searchAndReplace = editorAdaptor.getSearchAndReplaceService();
			return searchAndReplace.replace(start, end, find, replace, flags);
		}
    }

    /**
     * Captures the visual selection geometry before substitution begins so per-line
     * replacement stays bounded even on multi-line visual/block selections and after
     * prior lines change length.
     */
    private static final class VisualScope {
        private final ContentType contentType;
        private final int startLine;
        private int endLine;
        private final int startVisualOffset;
        private final int endVisualOffset;
        private final boolean stickToEOL;
        private final int firstLineColOffset;
        private final int lastLineColOffset;
        private final boolean empty;

        private VisualScope(ContentType contentType, int startLine, int endLine,
                int startVisualOffset, int endVisualOffset, boolean stickToEOL,
                int firstLineColOffset, int lastLineColOffset, boolean empty) {
            this.contentType = contentType;
            this.startLine = startLine;
            this.endLine = endLine;
            this.startVisualOffset = startVisualOffset;
            this.endVisualOffset = endVisualOffset;
            this.stickToEOL = stickToEOL;
            this.firstLineColOffset = firstLineColOffset;
            this.lastLineColOffset = lastLineColOffset;
            this.empty = empty;
        }

        static VisualScope create(EditorAdaptor editorAdaptor, TextContent model) {
            Selection selection = resolveVisualSelection(editorAdaptor);
            if (selection == null) {
                return new VisualScope(null, 0, -1, 0, 0, false, 0, 0, true);
            }
            Configuration config = editorAdaptor.getConfiguration();
            ContentType type = selection.getContentType(config);
            int docLen = model.getTextLength();
            CursorService cs = editorAdaptor.getCursorService();

            if (ContentType.TEXT_RECTANGLE.equals(type)) {
                Position from = clampPosition(selection.getFrom(), docLen);
                Position to = clampPosition(selection.getTo(), docLen);
                TextBlock block = BlockWiseSelection.getTextBlock(from, to, model, cs);
                return new VisualScope(type, block.startLine, block.endLine,
                        block.startVisualOffset, block.endVisualOffset,
                        cs.shouldStickToEOL(), 0, 0, false);
            }
            if (ContentType.LINES.equals(type)) {
                Position from = clampPosition(selection.getFrom(), docLen);
                Position to = clampPosition(selection.getTo(), docLen);
                int fromLine = model.getLineInformationOfOffset(from.getModelOffset()).getNumber();
                int toLine = model.getLineInformationOfOffset(to.getModelOffset()).getNumber();
                return new VisualScope(type, Math.min(fromLine, toLine), Math.max(fromLine, toLine),
                        0, 0, false, 0, 0, false);
            }

            int leftOffset = Math.min(selection.getLeftBound().getModelOffset(), docLen);
            int rightOffset = Math.min(selection.getRightBound().getModelOffset(), docLen);
            LineInformation firstLine = model.getLineInformationOfOffset(leftOffset);
            LineInformation lastLine = model.getLineInformationOfOffset(rightOffset);
            return new VisualScope(ContentType.TEXT, firstLine.getNumber(), lastLine.getNumber(),
                    0, 0, false,
                    leftOffset - firstLine.getBeginOffset(),
                    rightOffset - lastLine.getBeginOffset(),
                    false);
        }

        private static Selection resolveVisualSelection(EditorAdaptor editorAdaptor) {
            Configuration config = editorAdaptor.getConfiguration();
            Selection active = editorAdaptor.getSelection();
            if (isNonEmptyVisualSelection(active, config)) {
                return active;
            }
            Selection last = editorAdaptor.getLastActiveSelection();
            if (isNonEmptyVisualSelection(last, config)) {
                return last;
            }
            return null;
        }

        private static boolean isNonEmptyVisualSelection(Selection selection, Configuration config) {
            if (selection == null) {
                return false;
            }
            ContentType type = selection.getContentType(config);
            return selection.getModelLength() > 0
                    || ContentType.TEXT_RECTANGLE.equals(type)
                    || ContentType.LINES.equals(type);
        }

        private static Position clampPosition(Position pos, int maxOffset) {
            if (pos.getModelOffset() > maxOffset) {
                return pos.setModelOffset(maxOffset);
            }
            return pos;
        }

        void onLinesInserted(int lineDiff) {
            endLine += lineDiff;
        }

        int[] resolveLineBounds(LineInformation line, EditorAdaptor editorAdaptor) {
            if (empty) {
                return null;
            }
            int lineNo = line.getNumber();
            if (lineNo < startLine || lineNo > endLine) {
                return null;
            }
            if (ContentType.TEXT_RECTANGLE.equals(contentType)) {
                if (line.getLength() == 0) {
                    return null;
                }
                CursorService cs = editorAdaptor.getCursorService();
                Position colStart = cs.getPositionByVisualOffset(lineNo, startVisualOffset);
                if (colStart == null || colStart.getModelOffset() >= line.getEndOffset()) {
                    return null;
                }
                int start = Math.max(line.getBeginOffset(), colStart.getModelOffset());
                int end;
                if (stickToEOL) {
                    end = line.getEndOffset();
                } else {
                    Position colEnd = cs.getPositionByVisualOffset(lineNo, endVisualOffset);
                    if (colEnd == null || colEnd.getModelOffset() >= line.getEndOffset()) {
                        end = line.getEndOffset();
                    } else {
                        end = Math.min(colEnd.addModelOffset(1).getModelOffset(), line.getEndOffset());
                    }
                }
                return start < end ? new int[] { start, end } : null;
            }
            if (ContentType.LINES.equals(contentType)) {
                return new int[] { line.getBeginOffset(), line.getEndOffset() };
            }
            int start = (lineNo == startLine)
                    ? Math.min(line.getBeginOffset() + firstLineColOffset, line.getEndOffset())
                    : line.getBeginOffset();
            int end = (lineNo == endLine)
                    ? Math.min(line.getBeginOffset() + lastLineColOffset, line.getEndOffset())
                    : line.getEndOffset();
            return start < end ? new int[] { start, end } : null;
        }
    }

	/**
	 * Replace Eclipse's platform independent newline ("\R") with a platform
	 * specific newline.
	 * 
	 * This is useful when a string needs to be used by non-Eclipse code.
	 */
	private String convertToPlatformNewline(EditorAdaptor editorAdaptor, String inputString) {
		String platformNewline = editorAdaptor.getConfiguration().getNewLine();
		/*
		 * Change "\R" to platform newline only if it is preceded by zero or an even number of
		 * backslashes.
		 */
		String result = inputString.replaceAll("((^|[^\\\\])(\\\\\\\\)*)(\\\\R)", "$1" + platformNewline);
		return result;
	}

	public TextOperation repetition() {
		return this;
	}

}
