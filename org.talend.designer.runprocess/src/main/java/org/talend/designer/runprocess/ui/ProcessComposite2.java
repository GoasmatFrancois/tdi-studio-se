// ============================================================================
//
// Talend Community Edition
//
// Copyright (C) 2006 Talend - www.talend.com
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
//
// ============================================================================
package org.talend.designer.runprocess.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;
import org.talend.commons.ui.image.ImageProvider;
import org.talend.core.model.process.IContext;
import org.talend.designer.runprocess.IProcessor;
import org.talend.designer.runprocess.ProcessMessage;
import org.talend.designer.runprocess.Processor;
import org.talend.designer.runprocess.ProcessorException;
import org.talend.designer.runprocess.ProcessorUtilities;
import org.talend.designer.runprocess.RunProcessContext;
import org.talend.designer.runprocess.RunProcessPlugin;
import org.talend.designer.runprocess.ProcessMessage.MsgType;
import org.talend.designer.runprocess.i18n.Messages;
import org.talend.designer.runprocess.ui.actions.ClearPerformanceAction;
import org.talend.designer.runprocess.ui.actions.ClearTraceAction;

/**
 * DOC chuger class global comment. Detailled comment <br/>
 * 
 * $Id$
 * 
 */
public class ProcessComposite2 extends Composite {

    private static final int BUTTON_SIZE = 120;

    private static final int H_WEIGHT = 5;

    private static final int MINIMUM_HEIGHT = 65;

    private static final int MINIMUM_WIDTH = 530;

    private RunProcessContext processContext;

    /** Context composite. */
    private ProcessContextComposite contextComposite;

    /** Performance button. */
    private Button perfBtn;

    /** Trace button. */
    private Button traceBtn;

    /** Clear trace & performance button. */
    private Button clearTracePerfBtn;

    /** Clear log button. */
    // private Button clearLogBtn;
    private Button clearBeforeExec;

    /** Show time button. */
    private Button watchBtn;

    /** Exec button. */
    private Button execBtn;

    /** Kill button. */
    private Button killBtn;

    /** Debug button. */
    private Button debugBtn;

    /** Execution console. */
    private StyledText consoleText;

    /** RunProcessContext property change listener. */
    private PropertyChangeListener pcl;

    private CTabItem targetExecutionTabItem;

    private CTabFolder leftTabFolder;

    /**
     * DOC chuger ProcessComposite2 constructor comment.
     * 
     * @param parent Parent composite.
     * @param style Style bits.
     */
    public ProcessComposite2(Composite parent, int style) {
        super(parent, style);
        initGraphicComponents(parent);
    }

    /**
     * DOC amaumont Comment method "initGraphicComponents".
     * @param parent
     */
    private void initGraphicComponents(Composite parent) {
        GridData data;
        GridLayout layout = new GridLayout();
        setLayout(layout);

        // Splitter
        SashForm sash = new SashForm(this, SWT.HORIZONTAL | SWT.SMOOTH);
        sash.setLayoutData(new GridData(GridData.FILL_BOTH));

        layout = new GridLayout();
        sash.setLayout(layout);
        
        leftTabFolder = new CTabFolder(sash, SWT.BORDER);
        leftTabFolder.setSimple(false);

        leftTabFolder.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        // Group context
        
        CTabItem contextTabItem = new CTabItem(leftTabFolder, SWT.BORDER);
        contextTabItem.setText(Messages.getString("ProcessComposite.contextTab")); //$NON-NLS-1$
        contextComposite = new ProcessContextComposite(leftTabFolder, SWT.NONE);
        contextComposite.setBackground(leftTabFolder.getDisplay().getSystemColor(SWT.COLOR_WHITE));
        contextTabItem.setControl(contextComposite);

        Composite targetExecutionComposite = createTargetExecutionComposite(leftTabFolder);
        
        targetExecutionTabItem = new CTabItem(leftTabFolder, SWT.BORDER);
        targetExecutionTabItem.setText(Messages.getString("ProcessComposite.targetExecutionTab")); //$NON-NLS-1$
        targetExecutionTabItem.setToolTipText(Messages.getString("ProcessComposite.targetExecutionTabTooltipAvailable"));
        targetExecutionTabItem.setControl(targetExecutionComposite);
        
        
        // Group execution
        Group execGroup = new Group(sash, SWT.NONE);
        execGroup.setText(Messages.getString("ProcessComposite.execGroup")); //$NON-NLS-1$
        layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        execGroup.setLayout(layout);

        ScrolledComposite execScroll = new ScrolledComposite(execGroup, SWT.V_SCROLL | SWT.H_SCROLL);
        execScroll.setExpandHorizontal(true);
        execScroll.setExpandVertical(true);
        execScroll.setLayoutData(new GridData(GridData.FILL_BOTH));

        Composite execContent = new Composite(execScroll, SWT.NONE);
        layout = new GridLayout();
        execContent.setLayout(layout);
        execScroll.setContent(execContent);

        Composite execHeader = new Composite(execContent, SWT.NONE);
        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = 7;
        formLayout.marginHeight = 4;
        formLayout.spacing = 7;
        execHeader.setLayout(formLayout);
        execHeader.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

        debugBtn = new Button(execHeader, SWT.PUSH);
        debugBtn.setText(Messages.getString("ProcessDebugDialog.debugBtn")); //$NON-NLS-1$
        debugBtn.setToolTipText(Messages.getString("ProcessComposite.debugHint")); //$NON-NLS-1$
        debugBtn.setImage(RunProcessPlugin.imageDescriptorFromPlugin(RunProcessPlugin.PLUGIN_ID, "icons/process_debug.gif") //$NON-NLS-1$
                .createImage());
        FormData formData = new FormData();
        formData.top = new FormAttachment(0, 15);
        formData.left = new FormAttachment(0);
        formData.right = new FormAttachment(0, BUTTON_SIZE);
        debugBtn.setLayoutData(formData);

        execBtn = new Button(execHeader, SWT.PUSH);
        execBtn.setText(Messages.getString("ProcessComposite.exec")); //$NON-NLS-1$
        execBtn.setToolTipText(Messages.getString("ProcessComposite.execHint")); //$NON-NLS-1$
        execBtn.setImage(ImageProvider.getImage(ERunprocessImages.RUN_PROCESS_ACTION));
        execBtn.setEnabled(false);
        formData = new FormData();
        formData.top = new FormAttachment(debugBtn, 0, SWT.TOP);
        formData.left = new FormAttachment(debugBtn, 0, SWT.RIGHT);
        formData.right = new FormAttachment(debugBtn, BUTTON_SIZE, SWT.RIGHT);
        execBtn.setLayoutData(formData);

        killBtn = new Button(execHeader, SWT.PUSH);
        killBtn.setText(Messages.getString("ProcessComposite.kill")); //$NON-NLS-1$
        killBtn.setToolTipText(Messages.getString("ProcessComposite.killHint")); //$NON-NLS-1$
        killBtn.setImage(RunProcessPlugin
                .imageDescriptorFromPlugin(RunProcessPlugin.PLUGIN_ID, "icons/process_kill.gif").createImage()); //$NON-NLS-1$
        setButtonLayoutData(killBtn);
        killBtn.setEnabled(false);
        formData = new FormData();
        formData.top = new FormAttachment(debugBtn, 0, SWT.TOP);
        formData.left = new FormAttachment(execBtn, 0, SWT.RIGHT);
        formData.right = new FormAttachment(execBtn, BUTTON_SIZE, SWT.RIGHT);
        killBtn.setLayoutData(formData);

        clearBeforeExec = new Button(execHeader, SWT.CHECK);
        clearBeforeExec.setText(Messages.getString("ProcessComposite.clearBefore")); //$NON-NLS-1$
        clearBeforeExec.setToolTipText(Messages.getString("ProcessComposite.clearBeforeHint")); //$NON-NLS-1$
        clearBeforeExec.setEnabled(false);
        clearBeforeExec.setSelection(true);
        data = new GridData();
        data.horizontalSpan = 2;
        data.horizontalAlignment = SWT.END;
        clearBeforeExec.setLayoutData(data);
        formData = new FormData();
        formData.top = new FormAttachment(execBtn, 0, SWT.BOTTOM);
        formData.left = new FormAttachment(execBtn, 0, SWT.LEFT);
        clearBeforeExec.setLayoutData(formData);

        watchBtn = new Button(execHeader, SWT.CHECK);
        watchBtn.setText(Messages.getString("ProcessComposite.execTime")); //$NON-NLS-1$
        watchBtn.setToolTipText(Messages.getString("ProcessComposite.execTimeHint")); //$NON-NLS-1$
        watchBtn.setEnabled(false);
        watchBtn.setSelection(true);
        data = new GridData();
        data.horizontalSpan = 2;
        data.horizontalAlignment = SWT.END;
        watchBtn.setLayoutData(data);
        formData = new FormData();
        formData.top = new FormAttachment(killBtn, 0, SWT.BOTTOM);
        formData.left = new FormAttachment(clearBeforeExec, 0, SWT.RIGHT);
        watchBtn.setLayoutData(formData);

        Group statisticsComposite = new Group(execHeader, SWT.NONE);
        statisticsComposite.setText(Messages.getString("ProcessComposite2.statsComposite")); //$NON-NLS-1$
        layout = new GridLayout(3, false);
        layout.marginWidth = 0;
        statisticsComposite.setLayout(layout);
        formData = new FormData();
        formData.right = new FormAttachment(100, 0);
        statisticsComposite.setLayoutData(formData);

        Composite statisticsButtonComposite = new Composite(statisticsComposite, SWT.NONE);
        layout = new GridLayout(1, false);
        layout.marginWidth = 0;
        statisticsButtonComposite.setLayout(layout);
        statisticsButtonComposite.setLayoutData(new GridData(GridData.FILL_VERTICAL));

        perfBtn = new Button(statisticsButtonComposite, SWT.CHECK);
        perfBtn.setText(Messages.getString("ProcessComposite.stat")); //$NON-NLS-1$
        perfBtn.setToolTipText(Messages.getString("ProcessComposite.statHint")); //$NON-NLS-1$
        perfBtn.setEnabled(false);

        traceBtn = new Button(statisticsButtonComposite, SWT.CHECK);
        traceBtn.setText(Messages.getString("ProcessComposite.trace")); //$NON-NLS-1$
        traceBtn.setToolTipText(Messages.getString("ProcessComposite.traceHint")); //$NON-NLS-1$
        traceBtn.setEnabled(false);

        clearTracePerfBtn = new Button(statisticsComposite, SWT.PUSH);
        clearTracePerfBtn.setText(Messages.getString("ProcessComposite.clear")); //$NON-NLS-1$
        clearTracePerfBtn.setToolTipText(Messages.getString("ProcessComposite.clearHint")); //$NON-NLS-1$
        clearTracePerfBtn.setImage(RunProcessPlugin.imageDescriptorFromPlugin(RunProcessPlugin.PLUGIN_ID,
                "icons/process_stat_clear.gif").createImage()); //$NON-NLS-1$
        clearTracePerfBtn.setEnabled(false);

        consoleText = new StyledText(execContent, SWT.BORDER | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        data = new GridData(GridData.FILL_BOTH);
        data.horizontalSpan = 2;
        data.minimumHeight = MINIMUM_HEIGHT;
        data.minimumWidth = MINIMUM_WIDTH;
        consoleText.setLayoutData(data);
        Font font = new Font(parent.getDisplay(), "courier", 8, SWT.NONE); //$NON-NLS-1$
        consoleText.setFont(font);

        execScroll.setMinSize(execContent.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        sash.setWeights(new int[] { 2, H_WEIGHT });

        pcl = new PropertyChangeListener() {

            public void propertyChange(PropertyChangeEvent evt) {
                runProcessContextChanged(evt);
            }
        };

        addListeners();
    }

    /**
     * DOC amaumont Comment method "getTargetExecutionComposite".
     * @param parent 
     * @return
     */
    protected Composite createTargetExecutionComposite(Composite parent) {
        Composite composite = new Composite(parent, SWT.NONE);
        
        GridLayout layout = new GridLayout();
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        composite.setLayout(layout);

        StyledText text = new StyledText(composite, SWT.NONE);
        text.setText(Messages.getString("ProcessComposite.targetExecutionTabTooltipAvailable"));
        text.setWordWrap(true);
        text.setEditable(false);
        text.setLayoutData(new GridData(GridData.FILL_BOTH));
        
        return composite;
    }

    public boolean hasProcess() {
        return processContext != null;
    }

    private void addListeners() {
        perfBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                processContext.setMonitorPerf(perfBtn.getSelection());
            }
        });

        traceBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                processContext.setMonitorTrace(traceBtn.getSelection());
            }
        });

        clearTracePerfBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                ClearPerformanceAction clearPerfAction = new ClearPerformanceAction();
                clearPerfAction.setProcess(processContext.getProcess());
                clearPerfAction.run();
                ClearTraceAction clearTraceAction = new ClearTraceAction();
                clearTraceAction.setProcess(processContext.getProcess());
                clearTraceAction.run();
            }
        });
        /*
         * clearLogBtn.addSelectionListener(new SelectionAdapter() {
         * 
         * @Override public void widgetSelected(SelectionEvent e) { processContext.clearMessages(); } });
         */
        execBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                exec();
            }
        });

        killBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                kill();
            }
        });

        debugBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(final SelectionEvent e) {
                debug();
            }
        });

        watchBtn.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                processContext.setWatchAllowed(watchBtn.getSelection());
            }

        });
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.widgets.Widget#dispose()
     */
    @Override
    public void dispose() {
        if (processContext != null) {
            processContext.removePropertyChangeListener(pcl);
        }

        super.dispose();
    }

    /**
     * Set the layout data of the button to a GridData with appropriate heights and widths.
     * 
     * @param button
     */
    protected static void setButtonLayoutData(final Button button) {
        GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
        final int widthHint = 80;
        Point minSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true);
        data.widthHint = Math.max(widthHint, minSize.x);
        button.setLayoutData(data);
    }

    public void setProcessContext(RunProcessContext processContext) {
        if (this.processContext != null) {
            this.processContext.removePropertyChangeListener(pcl);
        }
        this.processContext = processContext;
        if (processContext != null) {
            processContext.addPropertyChangeListener(pcl);
        }

        perfBtn.setSelection(processContext != null && processContext.isMonitorPerf());
        traceBtn.setSelection(processContext != null && processContext.isMonitorTrace());
        watchBtn.setSelection(processContext != null && processContext.isWatchAllowed());
        setRunnable(processContext != null && !processContext.isRunning());
        killBtn.setEnabled(processContext != null && processContext.isRunning());
        // clearLogBtn.setEnabled(processContext != null);
        contextComposite.setProcess(processContext != null ? processContext.getProcess() : null);
        fillConsole(processContext != null ? processContext.getMessages() : new ArrayList<ProcessMessage>());
    }

    private void setRunnable(boolean runnable) {
        perfBtn.setEnabled(runnable);
        traceBtn.setEnabled(runnable);
        clearTracePerfBtn.setEnabled(runnable);
        execBtn.setEnabled(runnable);
        debugBtn.setEnabled(runnable);
        contextComposite.setEnabled(runnable);
        clearBeforeExec.setEnabled(runnable);
        watchBtn.setEnabled(runnable);
    }

    private void appendToConsole(final ProcessMessage message) {
        getDisplay().asyncExec(new Runnable() {

            public void run() {
                doAppendToConsole(message);
                scrollToEnd();
            }
        });
    }

    private void doAppendToConsole(final ProcessMessage message) {
        StyleRange style = new StyleRange();
        style.start = consoleText.getText().length();
        style.length = message.getContent().length();
        if (message.getType() == MsgType.CORE_OUT || message.getType() == MsgType.CORE_ERR) {
            style.fontStyle = SWT.ITALIC;
        }
        Color color;
        switch (message.getType()) {
        case CORE_OUT:
            color = getDisplay().getSystemColor(SWT.COLOR_BLUE);
            break;
        case CORE_ERR:
            color = getDisplay().getSystemColor(SWT.COLOR_DARK_RED);
            break;
        case STD_ERR:
            color = getDisplay().getSystemColor(SWT.COLOR_RED);
            break;
        case STD_OUT:
        default:
            color = getDisplay().getSystemColor(SWT.COLOR_BLACK);
            break;
        }
        style.foreground = color;

        consoleText.append(message.getContent());
        consoleText.setStyleRange(style);
    }

    private void fillConsole(List<ProcessMessage> messages) {
        consoleText.setText(""); //$NON-NLS-1$
        for (ProcessMessage message : messages) {
            doAppendToConsole(message);
        }
        scrollToEnd();
    }

    private void scrollToEnd() {
        consoleText.setCaretOffset(consoleText.getText().length());
        consoleText.showSelection();
    }

    public void exec() {
        if (clearBeforeExec.getSelection()) {
            processContext.clearMessages();
        }
        if (watchBtn.getSelection()) {
            processContext.switchTime();
        }
        processContext.setSelectedContext(contextComposite.getSelectedContext());
        processContext.exec(getShell());
    }

    public void kill() {
        killBtn.setEnabled(false);

        processContext.kill();
    }

    public void debug() {
        if (contextComposite.promptConfirmLauch()) {

            final IContext context = contextComposite.getSelectedContext();

            IRunnableWithProgress worker = new IRunnableWithProgress() {

                public void run(IProgressMonitor monitor) {
                    IProcessor processor = ProcessorUtilities.getProcessor(processContext.getProcess(), context);
                    monitor.beginTask("Launching debugger", IProgressMonitor.UNKNOWN); //$NON-NLS-1$
                    try {
                        ILaunchConfiguration config = processor.debug();
                        if (config != null) {
                            // PlatformUI.getWorkbench().getActiveWorkbenchWindow().addPerspectiveListener(new
                            // DebugInNewWindowListener());
                            DebugUITools.launch(config, ILaunchManager.DEBUG_MODE);
                        } else {
                            MessageDialog.openInformation(getShell(), Messages.getString("ProcessDebugDialog.debugBtn"), //$NON-NLS-1$
                                    Messages.getString("ProcessDebugDialog.errortext")); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    } catch (ProcessorException e) {
                        IStatus status = new Status(IStatus.ERROR, RunProcessPlugin.PLUGIN_ID, IStatus.OK,
                                "Debug launch failed.", e); //$NON-NLS-1$
                        RunProcessPlugin.getDefault().getLog().log(status);
                        MessageDialog.openError(getShell(), Messages.getString("ProcessDebugDialog.debugBtn"), ""); //$NON-NLS-1$ //$NON-NLS-2$
                    } finally {
                        monitor.done();
                    }
                }
            };

            IProgressService progressService = PlatformUI.getWorkbench().getProgressService();
            try {
                progressService.runInUI(PlatformUI.getWorkbench().getProgressService(), worker, ResourcesPlugin.getWorkspace()
                        .getRoot());
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void runProcessContextChanged(final PropertyChangeEvent evt) {
        String propName = evt.getPropertyName();
        if (RunProcessContext.PROP_MESSAGE_ADD.equals(propName)) {
            appendToConsole((ProcessMessage) evt.getNewValue());
        } else if (RunProcessContext.PROP_MESSAGE_CLEAR.equals(propName)) {
            getDisplay().asyncExec(new Runnable() {

                public void run() {
                    consoleText.setText(""); //$NON-NLS-1$
                }
            });
        } else if (RunProcessContext.PROP_MONITOR.equals(propName)) {
            perfBtn.setSelection(((Boolean) evt.getNewValue()).booleanValue());
        } else if (RunProcessContext.TRACE_MONITOR.equals(propName)) {
            traceBtn.setSelection(((Boolean) evt.getNewValue()).booleanValue());
        } else if (RunProcessContext.PROP_RUNNING.equals(propName)) {
            getDisplay().asyncExec(new Runnable() {

                public void run() {
                    boolean running = ((Boolean) evt.getNewValue()).booleanValue();
                    setRunnable(!running);
                    killBtn.setEnabled(running);
                }
            });
        }
    }

    public Display getDisplay() {
        return Display.getDefault();
    }

    
    /**
     * Getter for targetExecutionTabItem.
     * @return the targetExecutionTabItem
     */
    public CTabItem getTargetExecutionTabItem() {
        return this.targetExecutionTabItem;
    }

    
    /**
     * Getter for leftTabFolder.
     * @return the leftTabFolder
     */
    public CTabFolder getLeftTabFolder() {
        return this.leftTabFolder;
    }

    
    
}
