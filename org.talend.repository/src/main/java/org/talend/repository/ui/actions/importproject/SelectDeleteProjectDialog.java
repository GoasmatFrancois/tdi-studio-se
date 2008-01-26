// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.repository.ui.actions.importproject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ContainerCheckedTreeViewer;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.internal.util.SWTResourceUtil;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.commons.ui.swt.formtools.Form;
import org.talend.core.CorePlugin;
import org.talend.core.context.Context;
import org.talend.core.context.RepositoryContext;
import org.talend.core.model.general.Project;
import org.talend.core.prefs.GeneralParametersProvider;
import org.talend.core.prefs.GeneralParametersProvider.GeneralParameters;
import org.talend.repository.i18n.Messages;
import org.talend.repository.ui.ERepositoryImages;

/**
 * DOC qwei class global comment. Detailled comment
 */
public class SelectDeleteProjectDialog extends SelectionDialog {

    private Button bSelectAll;

    private Button bDeselectAll;

    private ContainerCheckedTreeViewer treeViewer;

    private List<Object> projectItemList = new ArrayList<Object>();

    private List<Object> delItemList = new ArrayList<Object>();

    private Project pro;

    private static final String DEFAULTMESAGE = Messages.getString("SelectDeleteProjectDialog.Label"); //$NON-NLS-1$

    private static final String TITILE = Messages.getString("SelectDeleteProjectDialog.Title"); //$NON-NLS-1$

    /**
     * DOC qwei SelectDeleteProjectDialog constructor comment.
     * 
     * @param parentShell
     */
    protected SelectDeleteProjectDialog(Shell parentShell) {
        super(parentShell);
        super.setShellStyle((SWT.APPLICATION_MODAL | SWT.DIALOG_TRIM | SWT.RESIZE | SWT.RESIZE | SWT.MIN | SWT.MAX));
        // TODO Auto-generated constructor stub
        setTitle(TITILE);
        setMessage(DEFAULTMESAGE);
        RepositoryContext repositoryContext = (RepositoryContext) CorePlugin.getContext().getProperty(
                Context.REPOSITORY_CONTEXT_KEY);
        this.pro = repositoryContext.getProject();

    }

    protected SelectDeleteProjectDialog(Shell parentShell, boolean login) {
        this(parentShell);
        getProjectItem(login);
    }

    private void getProjectItem(boolean login) {
        List<String> notExportProjects = Arrays.asList(GeneralParametersProvider
                .getStrings(GeneralParameters.PROJECTS_EXCLUDED_FROM_EXPORT));
        IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
        for (int i = 0; i < projects.length; i++) {
            if (projects[i].isOpen() && !notExportProjects.contains(projects[i].getName())) {
                if (!login && pro.getLabel().toLowerCase().equals(projects[i].getName().toLowerCase())) {

                } else {
                    projectItemList.add(projects[i]);
                }
            }
        }

    }

    @Override
    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        ((GridData) composite.getLayoutData()).widthHint = 390;
        composite.setFont(parent.getFont());
        createMessageArea(composite);
        Group group = Form.createGroup(composite, 10, null, 300);
        Composite inner = new Composite(group, SWT.NONE);
        inner.setFont(composite.getFont());
        inner.setLayoutData(new GridData(GridData.FILL_BOTH));
        GridLayout layout = new GridLayout();
        layout.numColumns = 2;
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.horizontalSpacing = 10;
        layout.makeColumnsEqualWidth = false;
        inner.setLayout(layout);
        createTreeViewer(inner);
        createButtons(inner);
        return composite;
    }

    private void createTreeViewer(Composite parent) {
        treeViewer = new ContainerCheckedTreeViewer(parent);
        treeViewer.setContentProvider(getResourceProvider(IResource.FOLDER | IResource.PROJECT));
        // treeViewer.setLabelProvider(WorkbenchLabelProvider.getDecoratingWorkbenchLabelProvider());
        treeViewer.setLabelProvider(new LabelProvider() {

            public final String getText(Object element) {
                // query the element for its label
                String label = "";
                if (element instanceof IProject) {
                    IProject project = (IProject) element;
                    label = project.getName();
                } else if (element instanceof IFolder) {
                    IFolder folder = (IFolder) element;
                    label = folder.getName();
                }
                return label;
            }

            public final Image getImage(Object element) {
                // obtain the base image by querying the element

                if (element instanceof IProject) {
                    return ImageProvider.getImage(ERepositoryImages.PROJECT_ICON);
                } else if (element instanceof IFolder) {
                    return ImageProvider.getImage(ERepositoryImages.FOLDER_ICON);
                }
                return null;
            }
        });
        treeViewer.setInput(projectItemList);
        treeViewer.getTree().setLayoutData(new GridData(GridData.FILL_BOTH));
        addTreeListener();

    }

    private void createButtons(Composite parent) {

        Composite buttons = new Composite(parent, SWT.NONE);
        buttons.setFont(parent.getFont());
        GridData data = new GridData(GridData.FILL_VERTICAL);
        buttons.setLayoutData(data);

        GridLayout layout = new GridLayout();
        layout.marginHeight = 10;
        layout.marginWidth = 0;
        layout.marginRight = 0;

        buttons.setLayout(layout);
        bSelectAll = new Button(buttons, SWT.PUSH);
        bSelectAll.setText(Messages.getString("SelectDeleteProjectDialog.SelectAll")); //$NON-NLS-1$
        bSelectAll.setFont(parent.getFont());
        setButtonLayoutData(bSelectAll);
        bSelectAll.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                treeViewer.setAllChecked(true);
                delItemList = projectItemList;
            }
        });

        bDeselectAll = new Button(buttons, SWT.PUSH);
        bDeselectAll.setText(Messages.getString("SelectDeleteProjectDialog.DeselectAll")); //$NON-NLS-1$
        bDeselectAll.setFont(parent.getFont());
        setButtonLayoutData(bDeselectAll);
        bDeselectAll.addSelectionListener(new SelectionAdapter() {

            public void widgetSelected(SelectionEvent e) {
                treeViewer.setAllChecked(false);
                if (!delItemList.isEmpty()) {
                    delItemList.removeAll(projectItemList);
                }
            }
        });
    }

    private ITreeContentProvider getResourceProvider(final int resourceType) {
        return new WorkbenchContentProvider() {

            public Object[] getChildren(Object o) {
                if (o instanceof IContainer) {
                    IResource[] members = null;
                    try {
                        members = ((IContainer) o).members();
                    } catch (CoreException e) {
                        // just return an empty set of children
                        return new Object[0];
                    }

                    // filter out the desired resource types
                    ArrayList results = new ArrayList();
                    for (int i = 0; i < members.length; i++) {
                        // And the test bits with the resource types to see if they are what we want
                        if ((members[i].getType() & resourceType) > 0) {
                            results.add(members[i]);
                        }
                    }
                    return results.toArray();
                }
                // input element case
                if (o instanceof ArrayList) {
                    return ((ArrayList) o).toArray();
                }
                return new Object[0];
            }
        };
    }

    private void addTreeListener() {
        treeViewer.addCheckStateListener(new ICheckStateListener() {

            public void checkStateChanged(CheckStateChangedEvent event) {
                Object obj = event.getElement();
                delItemList.add(obj);
            }

        });
    }

    private void delProjectItem() {
        try {

            if (delItemList.size() != 0) {
                for (Object obj : delItemList) {
                    if (obj instanceof IProject) {
                        IProject project = (IProject) obj;
                        project.delete(true, null);
                        // IResourceChangeDescriptionFactory factory =
                        // ResourceChangeValidator.getValidator().createDeltaFactory();
                        // factory.delete(project);

                    } else if (obj instanceof IFolder) {
                        IFolder folder = (IFolder) obj;
                        folder.delete(false, null);
                    }

                }
            }
        } catch (CoreException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Override
    protected void okPressed() {
        delProjectItem();
        super.okPressed();

    }
}
