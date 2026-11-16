package net.sourceforge.vrapper.eclipse.interceptor;

import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.sourceforge.vrapper.platform.VrapperPlatformException;

import org.eclipse.ui.IEditorPart;

/**
 * This class holds information on the way top-level editors are nested.
 * <p>
 * Certain editors consist of multiple pages (e.g. the PDE Plugin editor), we need to keep this
 * information to prevent endless recursion when installing Vrapper into all the child editors. This
 * information is also necessary when switching between different child editors.
 * <p>
 * Uses weak references to {@link IEditorPart} so values stored in {@link WeakHashMap} do not pin
 * closed editors in memory.
 */
public class EditorInfo {

    protected Map<IEditorPart, EditorInfo> childEditors = new WeakHashMap<IEditorPart, EditorInfo>();
    protected EditorInfo parent;
    protected final WeakReference<IEditorPart> current;

    protected EditorInfo(IEditorPart current) {
        this.current = new WeakReference<IEditorPart>(current);
    }

    protected EditorInfo(EditorInfo parent, IEditorPart current) {
        this.parent = parent;
        this.current = new WeakReference<IEditorPart>(current);
    }

    /** Stores information about the child and returns the NestedEditorpartInfo for the child. */
    public EditorInfo createChildInfo(IEditorPart editorPart) {
        if (editorPart == null) {
            throw new VrapperPlatformException("Child editor cannot be null! Error while"
                    + " traversing " + getCurrent());
        }
        EditorInfo child = new EditorInfo(this, editorPart);
        this.childEditors.put(editorPart, child);
        return child;
    }

    public Map<IEditorPart, EditorInfo> getChildEditors() {
        return Collections.unmodifiableMap(childEditors);
    }

    public void setChildEditors(Set<IEditorPart> childEditors) {
        this.childEditors = new WeakHashMap<IEditorPart, EditorInfo>();
        for (IEditorPart editor : childEditors) {
            this.childEditors.put(editor, new EditorInfo(editor));
        }
    }

    public EditorInfo getChild(IEditorPart subPart) {
        return childEditors.get(subPart);
    }

    public boolean isTopLevelEditor() {
        return parent == null;
    }

    /** Whether this editor is a simple AbstractTextEditor without any nested parts. */
    public boolean isSimpleEditor() {
        return parent == null && childEditors.isEmpty();
    }

    public IEditorPart getTopLevelEditor() {
        if (parent == null) {
            return getCurrent();
        } else {
            return parent.getTopLevelEditor();
        }
    }

    public EditorInfo getParentInfo() {
        return parent;
    }

    public IEditorPart getCurrent() {
        return current.get();
    }

    public void dispose() {
        current.clear();
        childEditors.clear();
    }
}
