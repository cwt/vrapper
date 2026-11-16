package net.sourceforge.vrapper.eclipse.platform;

import net.sourceforge.vrapper.eclipse.ui.CommandLineUIFactory;
import net.sourceforge.vrapper.eclipse.ui.ModeContributionItem;
import net.sourceforge.vrapper.platform.CommandLineUI;
import net.sourceforge.vrapper.platform.UserInterfaceService;
import net.sourceforge.vrapper.vim.EditorAdaptor;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;

public class EclipseUserInterfaceService implements UserInterfaceService {

    private static final String CONTRIBUTION_ITEM_NAME = "VimInputMode";

    private final CommandLineUIFactory commandLineFactory;
    private final java.lang.ref.WeakReference<IEditorPart> editorRef;
    private final PartChangeListener partChangeListener = new PartChangeListener();
    private final java.lang.ref.WeakReference<org.eclipse.ui.IPartService> partServiceRef;
    private final ModeContributionItem vimInputModeItem;

    private String lastInfoValue = "";
    private String lastErrorValue = "";

    /* When a normal-mode command is entered, the letters you type
     * are displayed in the info status bar. If your command puts
     * something in the info bar, since it is shared, we want to
     * first wipe out the command letters, and the replace it with
     * the command result. Setting infoSet to true, and storing the
     * command result in lastCommandResultValue will accomplish this.
     * See CommandBasedMode.java for the if/else logic on this. -- BRD
     */
    private boolean infoSet;
    private String lastCommandResultValue = "";

    private String currentModeName;

    public EclipseUserInterfaceService(final IEditorPart editor, final ITextViewer textViewer) {
        this.editorRef = new java.lang.ref.WeakReference<IEditorPart>(editor);
        commandLineFactory = new CommandLineUIFactory(textViewer.getTextWidget());
        vimInputModeItem = getContributionItem();
        setEditorMode(VRAPPER_DISABLED);
        final org.eclipse.ui.IWorkbenchPartSite site = editor.getSite();
        final org.eclipse.ui.IWorkbenchWindow window = site != null ? site.getWorkbenchWindow() : null;
        final org.eclipse.ui.IPartService partService = window != null ? window.getPartService() : null;
        this.partServiceRef = new java.lang.ref.WeakReference<org.eclipse.ui.IPartService>(partService);
        if (partService != null) {
            partService.addPartListener(partChangeListener);
        }
    }

    private IEditorPart getEditor() {
        return editorRef.get();
    }

    /**
     * Unregisters workbench part listeners attached for this editor UI service.
     */
    public void dispose() {
        final org.eclipse.ui.IPartService partService = partServiceRef.get();
        if (partService != null) {
            partService.removePartListener(partChangeListener);
            partServiceRef.clear();
        }
        editorRef.clear();
    }

    @Override
    public void setEditorMode(final String modeName) {
        currentModeName = modeName;
        vimInputModeItem.setText(currentModeName);
    }

    // For :ascii command
    @Override
    public void setAsciiValues(final String asciiValue, final int decValue, final String hexValue, final String octalValue) {
        final String asciiValueText = "<" + asciiValue + ">  "
                              + decValue + ",  "
                              + "Hex " + hexValue + ",  "
                              + "Octal " + octalValue;
        lastCommandResultValue = asciiValueText;
        setErrorMessage(null);
        setInfoMessage(asciiValueText);
    }

    @Override
    public String getLastCommandResultValue() {
        return lastCommandResultValue;
    }

    @Override
    public void setLastCommandResultValue(final String lastCommandResultValue) {
        this.lastCommandResultValue = lastCommandResultValue;
    }

    @Override
    public void setErrorMessage(final String content) {
        final IEditorPart editor = getEditor();
        if (editor != null) {
            editor.getEditorSite().getActionBars().getStatusLineManager().setErrorMessage(content);
        }
        lastErrorValue = content;
    }

    @Override
    public String getLastErrorValue() {
        return lastErrorValue;
    }

    @Override
    public void setInfoMessage(final String content) {
        final IEditorPart editor = getEditor();
        if (editor != null) {
            editor.getEditorSite().getActionBars().getStatusLineManager().setMessage(content);
        }
        lastInfoValue = content;
    }

    @Override
    public String getLastInfoValue() {
        return lastInfoValue;
    }

    private ModeContributionItem getContributionItem() {
        final IEditorPart editor = getEditor();
        final String name = CONTRIBUTION_ITEM_NAME + editor.getEditorSite().getId();
        final IStatusLineManager manager = editor.getEditorSite().getActionBars().getStatusLineManager();
        ModeContributionItem item = (ModeContributionItem) manager.find(name);
        if (item == null) {
            item = new ModeContributionItem(name);
            try {
                manager.insertBefore("ElementState", item);
            } catch (final IllegalArgumentException e) {
                manager.add(item);
            }
            item.setVisible(true);
            manager.update(true);
        }
        return item;
    }

    private final class PartChangeListener implements IPartListener {

        @Override
        public void partActivated(final IWorkbenchPart arg0) {
            final IEditorPart editor = getEditor();
            if (editor != null && arg0 == editor) {
                setEditorMode(currentModeName);
            }
        }

        @Override
        public void partBroughtToTop(final IWorkbenchPart arg0) { }

        @Override
        public void partClosed(final IWorkbenchPart arg0) {
            final IEditorPart editor = getEditor();
            if (editor == null || arg0 == editor) {
                dispose();
            }
        }

        @Override
        public void partDeactivated(final IWorkbenchPart arg0) { }

        @Override
        public void partOpened(final IWorkbenchPart arg0) { }
    }

    @Override
    public void setRecording(final boolean b, final String macroName) {
        setEditorMode(currentModeName);
        vimInputModeItem.setRecording(b, macroName);
    }

    @Override
    public boolean isInfoSet() {
        return infoSet;
    }

    @Override
    public void setInfoSet(final boolean infoSet) {
        this.infoSet = infoSet;
    }

    @Override
    public CommandLineUI getCommandLineUI(EditorAdaptor editorAdaptor) {
        return commandLineFactory.createCommandLineUI(editorAdaptor);
    }
}
